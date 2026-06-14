package com.kema.k2look.service

import com.kema.k2look.K2LookApplication

/**
 * App-wide logging facade.
 *
 * It always emits to Android Logcat and, when enabled, mirrors logs to the rotating on-device log
 * files managed by AppLoggerService.
 */
object AppLog {

    private fun logger(): AppLoggerService? = K2LookApplication.getAppLogger()

    fun d(tag: String, msg: String): Int = logger()?.d(tag, msg) ?: android.util.Log.d(tag, msg)

    fun d(tag: String, msg: String, tr: Throwable?): Int =
            logger()?.d(tag, msg, tr) ?: android.util.Log.d(tag, msg, tr)

    fun i(tag: String, msg: String): Int = logger()?.i(tag, msg) ?: android.util.Log.i(tag, msg)

    fun i(tag: String, msg: String, tr: Throwable?): Int =
            logger()?.i(tag, msg, tr) ?: android.util.Log.i(tag, msg, tr)

    fun w(tag: String, msg: String): Int = logger()?.w(tag, msg) ?: android.util.Log.w(tag, msg)

    fun w(tag: String, msg: String, tr: Throwable?): Int =
            logger()?.w(tag, msg, tr) ?: android.util.Log.w(tag, msg, tr)

    fun e(tag: String, msg: String): Int = logger()?.e(tag, msg) ?: android.util.Log.e(tag, msg)

    fun e(tag: String, msg: String, tr: Throwable?): Int =
            logger()?.e(tag, msg, tr) ?: android.util.Log.e(tag, msg, tr)

    fun v(tag: String, msg: String): Int = logger()?.v(tag, msg) ?: android.util.Log.v(tag, msg)

    fun v(tag: String, msg: String, tr: Throwable?): Int =
            logger()?.v(tag, msg, tr) ?: android.util.Log.v(tag, msg, tr)

    fun wtf(tag: String, msg: String): Int = android.util.Log.wtf(tag, msg)

    fun wtf(tag: String, msg: String, tr: Throwable?): Int = android.util.Log.wtf(tag, msg, tr)
}
