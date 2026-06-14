package com.kema.k2look.service

import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Thread-safe wrapper around android.util.Log with optional rotating daily file logging. Keeps only
 * the 7 newest log files by modification time.
 */
class AppLoggerService(private val logsDir: File) {

    companion object {
        const val TAG = "AppLoggerService"
        const val MAX_LOG_FILES = 7
        val LOG_FILE_PATTERN = Regex("^k2look_\\d{8}_\\d{6}\\.log$")

        private val dateFormatterTL =
                object : ThreadLocal<SimpleDateFormat>() {
                    override fun initialValue(): SimpleDateFormat =
                            SimpleDateFormat("yyyyMMdd", Locale.US)
                }

        private val timeFormatterTL =
                object : ThreadLocal<SimpleDateFormat>() {
                    override fun initialValue(): SimpleDateFormat =
                            SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
                }

        private val fileDateFormatterTL =
                object : ThreadLocal<SimpleDateFormat>() {
                    override fun initialValue(): SimpleDateFormat =
                            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                }
    }

    private var fileWriter: FileWriter? = null
    private var printWriter: PrintWriter? = null
    private var fileLoggingEnabled: Boolean = false
    private var lastFileDate: String? = null
    private val lock = Any()

    init {
        if (!logsDir.exists()) {
            try {
                logsDir.mkdirs()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create logs directory: ${e.message}")
            }
        }
    }

    /**
     * Enable or disable file logging. When enabled, creates a new dated log file. When disabled,
     * closes the current log file.
     */
    fun enableFileLogging(enable: Boolean) {
        synchronized(lock) {
            if (enable == fileLoggingEnabled) return

            if (enable) {
                try {
                    fileLoggingEnabled = true
                    createLogFile()
                    cleanupOldLogs()
                } catch (e: IOException) {
                    Log.e(TAG, "IO error enabling file logging: ${e.message}", e)
                    fileLoggingEnabled = false
                    closeLogFile()
                } catch (e: SecurityException) {
                    Log.e(TAG, "Security error enabling file logging: ${e.message}", e)
                    fileLoggingEnabled = false
                    closeLogFile()
                } catch (e: Exception) {
                    Log.e(
                            TAG,
                            "Unexpected error enabling file logging: ${e.javaClass.simpleName}: ${e.message}",
                            e,
                    )
                    fileLoggingEnabled = false
                    closeLogFile()
                }
            } else {
                fileLoggingEnabled = false
                closeLogFile()
            }
        }
    }

    /** Log a debug message. */
    fun d(tag: String, msg: String): Int = d(tag, msg, null)

    fun d(tag: String, msg: String, tr: Throwable? = null): Int {
        Log.d(tag, msg, tr)
        writeToFile("D", tag, msg, tr)
        return 0
    }

    /** Log an info message. */
    fun i(tag: String, msg: String): Int = i(tag, msg, null)

    fun i(tag: String, msg: String, tr: Throwable? = null): Int {
        Log.i(tag, msg, tr)
        writeToFile("I", tag, msg, tr)
        return 0
    }

    /** Log a warning message. */
    fun w(tag: String, msg: String): Int = w(tag, msg, null)

    fun w(tag: String, msg: String, tr: Throwable? = null): Int {
        Log.w(tag, msg, tr)
        writeToFile("W", tag, msg, tr)
        return 0
    }

    /** Log an error message. */
    fun e(tag: String, msg: String): Int = e(tag, msg, null)

    fun e(tag: String, msg: String, tr: Throwable? = null): Int {
        Log.e(tag, msg, tr)
        writeToFile("E", tag, msg, tr)
        return 0
    }

    /** Log a verbose message. */
    fun v(tag: String, msg: String): Int = v(tag, msg, null)

    fun v(tag: String, msg: String, tr: Throwable? = null): Int {
        Log.v(tag, msg, tr)
        writeToFile("V", tag, msg, tr)
        return 0
    }

    /**
     * Write log line to file if file logging is enabled. Format: HH:mm:ss.SSS [LEVEL] tag: msg If
     * throwable is provided, appends stack trace.
     */
    private fun writeToFile(level: String, tag: String, msg: String, tr: Throwable?) {
        synchronized(lock) {
            try {
                if (!fileLoggingEnabled) return

                val now = Date()
                val today = checkNotNull(dateFormatterTL.get()).format(now)

                // Rotate file when date changes while logging remains enabled.
                if (lastFileDate != null && lastFileDate != today) {
                    closeLogFile()
                }

                if (printWriter == null) {
                    cleanupOldLogs()
                    createLogFile()
                }

                val writer = printWriter ?: return
                lastFileDate = today

                val timestamp = checkNotNull(timeFormatterTL.get()).format(now)
                val logLine = "$timestamp [$level] $tag: $msg"
                writer.println(logLine)

                if (tr != null) {
                    tr.printStackTrace(writer)
                }

                writer.flush()
            } catch (e: IOException) {
                Log.e(TAG, "IO error writing log: ${e.message}", e)
            } catch (e: SecurityException) {
                Log.e(TAG, "Security error (permissions?) writing log: ${e.message}", e)
            } catch (e: Exception) {
                Log.e(
                        TAG,
                        "Unexpected error writing log: ${e.javaClass.simpleName}: ${e.message}",
                        e,
                )
            }
        }
    }

    /** Create a new dated log file with name: k2look_YYYYMMDD_HHmmss.log */
    private fun createLogFile() {
        synchronized(lock) {
            try {
                closeLogFile()

                val now = Date()
                val fileName = "k2look_${checkNotNull(fileDateFormatterTL.get()).format(now)}.log"
                val logFile = File(logsDir, fileName)

                fileWriter = FileWriter(logFile, true)
                printWriter = PrintWriter(fileWriter!!, true)
                lastFileDate = checkNotNull(dateFormatterTL.get()).format(now)
            } catch (e: IOException) {
                Log.e(TAG, "IO error creating log file: ${e.message}", e)
                fileWriter = null
                printWriter = null
                lastFileDate = null
            } catch (e: SecurityException) {
                Log.e(TAG, "Security error creating log file: ${e.message}", e)
                fileWriter = null
                printWriter = null
                lastFileDate = null
            } catch (e: Exception) {
                Log.e(
                        TAG,
                        "Unexpected error creating log file: ${e.javaClass.simpleName}: ${e.message}",
                        e,
                )
                fileWriter = null
                printWriter = null
                lastFileDate = null
            }
        }
    }

    /** Close the current log file. */
    private fun closeLogFile() {
        try {
            printWriter?.close()
            fileWriter?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close log file: ${e.message}")
        }
        printWriter = null
        fileWriter = null
        lastFileDate = null
    }

    /** Delete old log files, keeping only the MAX_LOG_FILES newest by modification time. */
    private fun cleanupOldLogs() {
        synchronized(lock) {
            try {
                val logFiles =
                        logsDir.listFiles { file ->
                            file.isFile && LOG_FILE_PATTERN.matches(file.name)
                        }
                                ?: return

                if (logFiles.size <= MAX_LOG_FILES) return

                // Sort by modification time (oldest first) and delete excess
                val filesToDelete = logFiles.sortedBy { it.lastModified() }.dropLast(MAX_LOG_FILES)

                for (file in filesToDelete) {
                    try {
                        file.delete()
                    } catch (e: SecurityException) {
                        Log.w(TAG, "Failed to delete old log file ${file.name}: ${e.message}")
                    } catch (e: Exception) {
                        Log.w(
                                TAG,
                                "Unexpected error deleting old log file ${file.name}: ${e.javaClass.simpleName}: ${e.message}",
                        )
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "IO error cleaning up old logs: ${e.message}", e)
            } catch (e: SecurityException) {
                Log.e(TAG, "Security error cleaning up old logs: ${e.message}", e)
            } catch (e: Exception) {
                Log.e(
                        TAG,
                        "Unexpected error cleaning up old logs: ${e.javaClass.simpleName}: ${e.message}",
                        e,
                )
            }
        }
    }

    /** Flush and close the log file. Used for testing and cleanup. */
    fun flushAndClose() {
        synchronized(lock) {
            closeLogFile()
            fileLoggingEnabled = false
        }
    }
}
