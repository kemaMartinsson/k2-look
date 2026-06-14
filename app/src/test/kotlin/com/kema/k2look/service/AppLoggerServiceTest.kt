package com.kema.k2look.service

import com.google.common.truth.Truth.assertThat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AppLoggerServiceTest {

    private lateinit var tempLogsDir: File
    private lateinit var logger: AppLoggerService

    @Before
    fun setUp() {
        tempLogsDir =
                File.createTempFile("k2look_logs", "").apply {
                    delete()
                    mkdirs()
                }
        logger = AppLoggerService(tempLogsDir)
    }

    @After
    fun tearDown() {
        logger.flushAndClose()
        tempLogsDir.deleteRecursively()
    }

    /**
     * Test 1: d() logs to logcat without file write when disabled. Verify no log files are created
     * when file logging is disabled.
     */
    @Test
    fun testDebugLogsToLogcatWithoutFileWriteWhenDisabled() {
        // Ensure file logging is disabled (default)
        assertThat(tempLogsDir.listFiles().orEmpty().any { it.name.endsWith(".log") }).isFalse()

        logger.d("TestTag", "Debug message without file logging")

        // Verify no log files were created
        val logFiles = tempLogsDir.listFiles() ?: emptyArray()
        assertThat(logFiles.size).isEqualTo(0)
    }

    /**
     * Test 2: enableFileLogging creates log file with today's date. Verify file exists and name
     * matches pattern: k2look_YYYYMMDD_HHmmss.log
     */
    @Test
    fun testEnableFileLoggingCreatesLogFileWithTodaysDate() {
        logger.enableFileLogging(true)

        // Verify a log file was created
        val logFiles = tempLogsDir.listFiles() ?: emptyArray()
        assertThat(logFiles.size).isEqualTo(1)

        val logFile = logFiles[0]
        assertThat(logFile.name.matches(AppLoggerService.LOG_FILE_PATTERN)).isTrue()

        // Verify filename contains today's date
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
        val todayDate = dateFormat.format(Date())
        assertThat(logFile.name).contains(todayDate)
    }

    /**
     * Test 3: File contains formatted log line with timestamp and tag. Verify that i() writes to
     * file with proper format.
     */
    @Test
    fun testFileContainsFormattedLogLineWithTimestampAndTag() {
        logger.enableFileLogging(true)
        logger.i("MyTag", "Test info message")
        logger.flushAndClose()

        val logFiles = tempLogsDir.listFiles() ?: emptyArray()
        assertThat(logFiles.size).isEqualTo(1)

        val logContent = logFiles[0].readText()
        assertThat(logContent).isNotEmpty()

        // Verify log line contains expected components
        assertThat(logContent).contains("MyTag")
        assertThat(logContent).contains("Test info message")
        assertThat(logContent).contains("[I]")

        // Verify timestamp format (HH:mm:ss.SSS)
        val timePattern = Regex("\\d{2}:\\d{2}:\\d{2}\\.\\d{3}")
        assertThat(timePattern.containsMatchIn(logContent)).isTrue()
    }

    /**
     * Test 4: Retention keeps only 7 newest files. Create synthetic old log files with controlled
     * timestamps, then verify cleanup keeps 7.
     */
    @Test
    fun testRetentionKeepsOnly7NewestFiles() {
        logger.enableFileLogging(true)

        // Create 10 synthetic old log files with deterministic names and mtime ordering.
        repeat(10) { i ->
            val logFile = File(tempLogsDir, "k2look_20260601_${String.format("%06d", i)}.log")
            logFile.writeText("Dummy log content $i")
            val modTime = System.currentTimeMillis() - (10 - i) * 1000L
            logFile.setLastModified(modTime)
        }

        val filesBeforeCleanup = tempLogsDir.listFiles()?.size ?: 0
        assertThat(filesBeforeCleanup).isAtLeast(10)

        // Trigger cleanup through a disable/enable cycle and write.
        logger.enableFileLogging(false)
        logger.enableFileLogging(true)
        logger.d("TestTag", "Cleanup trigger")
        logger.flushAndClose()

        val files = tempLogsDir.listFiles() ?: emptyArray()
        assertEquals(7, files.size)

        val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val fileNames = files.map { it.name }
        assertThat(fileNames.any { it.contains(today) }).isTrue()
    }

    /** Test 5: Date rollover closes old writer and creates a new file. */
    @Test
    fun testDateRolloverCreatesNewFileAtMidnightBoundary() {
        logger.enableFileLogging(true)
        logger.d("TestTag", "Before rollover")

        val firstFiles = tempLogsDir.listFiles() ?: emptyArray()
        assertThat(firstFiles.size).isEqualTo(1)
        val firstName = firstFiles[0].name

        val field = AppLoggerService::class.java.getDeclaredField("lastFileDate")
        field.isAccessible = true
        field.set(logger, "19000101")

        Thread.sleep(1100)
        logger.d("TestTag", "After rollover")
        logger.flushAndClose()

        val secondFiles = tempLogsDir.listFiles() ?: emptyArray()
        assertThat(secondFiles.size).isEqualTo(2)
        val secondName = secondFiles.first { it.name != firstName }.name

        assertThat(firstName).matches(AppLoggerService.LOG_FILE_PATTERN.pattern)
        assertThat(secondName).matches(AppLoggerService.LOG_FILE_PATTERN.pattern)
    }

    /** Test 6: Concurrent writes and enable/disable transitions should not throw. */
    @Test
    fun testConcurrentDisableDuringWriteDoesNotCrash() {
        logger.enableFileLogging(true)
        val errors = Collections.synchronizedList(mutableListOf<Exception>())

        val writerThread = Thread {
            try {
                repeat(100) {
                    logger.d("TestTag", "Message $it")
                    Thread.sleep(1)
                }
            } catch (e: Exception) {
                errors.add(e)
            }
        }

        val toggleThread = Thread {
            try {
                Thread.sleep(5)
                logger.enableFileLogging(false)
                Thread.sleep(5)
                logger.enableFileLogging(true)
                Thread.sleep(5)
                logger.enableFileLogging(false)
            } catch (e: Exception) {
                errors.add(e)
            }
        }

        writerThread.start()
        toggleThread.start()
        writerThread.join()
        toggleThread.join()

        assertThat(errors).isEmpty()
    }

    /**
     * Test 7: disableFileLogging stops writing to file. Enable logging, write a message, disable,
     * write another, verify file count unchanged.
     */
    @Test
    fun testDisableFileLoggingStopsWritingToFile() {
        logger.enableFileLogging(true)
        logger.i("Tag1", "Message 1")

        val filesAfterEnable = tempLogsDir.listFiles()?.size ?: 0
        assertThat(filesAfterEnable).isEqualTo(1)

        logger.enableFileLogging(false)
        logger.i("Tag2", "Message 2")

        val filesAfterDisable = tempLogsDir.listFiles()?.size ?: 0
        assertThat(filesAfterDisable).isEqualTo(1)

        // Verify that only the first message was written to file
        val logFile = (tempLogsDir.listFiles() ?: emptyArray())[0]
        val logContent = logFile.readText()
        assertThat(logContent).contains("Message 1")
        assertThat(logContent).doesNotContain("Message 2")
    }

    /**
     * Test 8: Throwable is included in log line. Verify that e() with exception writes exception
     * details to file.
     */
    @Test
    fun testThrowableIsIncludedInLogLine() {
        logger.enableFileLogging(true)

        val exception = Exception("Test exception message")
        logger.e("ErrorTag", "An error occurred", exception)
        logger.flushAndClose()

        val logFiles = tempLogsDir.listFiles() ?: emptyArray()
        assertThat(logFiles.size).isEqualTo(1)

        val logContent = logFiles[0].readText()
        assertThat(logContent).contains("ErrorTag")
        assertThat(logContent).contains("An error occurred")
        assertThat(logContent).contains("[E]")

        // Verify exception class name and message are in the stack trace
        assertThat(logContent).contains("Exception")
        assertThat(logContent).contains("Test exception message")
    }

    /**
     * Test 9: File logging initialization failures are handled gracefully. Uses a file path instead
     * of a directory to force file creation failure.
     */
    @Test
    fun testEnableFileLoggingHandlesIoErrorsGracefully() {
        val notADirectory = File.createTempFile("k2look_not_a_dir", ".tmp")

        try {
            val loggerWithBadPath = AppLoggerService(notADirectory)
            loggerWithBadPath.enableFileLogging(true)
            loggerWithBadPath.d("TestTag", "This should not crash")
            loggerWithBadPath.flushAndClose()

            // Reaching this point confirms graceful degradation with no crash.
            assertThat(true).isTrue()
        } finally {
            notADirectory.delete()
        }
    }
}
