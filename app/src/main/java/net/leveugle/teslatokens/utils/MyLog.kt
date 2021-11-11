package net.leveugle.teslatokens.utils

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

object MyLog {
    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)

    @JvmStatic
    fun e(tag: String, msg: String): Int {
        //val logMsg = DATE_FORMAT.format(Date()) + " E/${tag}: ${msg}"
        //FirebaseCrashlytics.getInstance().log(logMsg);
        return Log.e(tag, msg)
    }

    @JvmStatic
    fun e(tag: String, msg: String, tr: Throwable): Int {
        //val logMsg = DATE_FORMAT.format(Date()) + " E/${tag}: ${msg}\n${Log.getStackTraceString(tr)}"
        //FirebaseCrashlytics.getInstance().log(logMsg);
        return Log.e(tag, msg, tr)
    }

    @JvmStatic
    fun i(tag: String, msg: String): Int {
        //val logMsg = DATE_FORMAT.format(Date()) + " I/${tag}: ${msg}"
        //FirebaseCrashlytics.getInstance().log(logMsg);
        return Log.i(tag, msg)
    }

    @JvmStatic
    fun d(tag: String, msg: String): Int {
        //val logMsg = DATE_FORMAT.format(Date()) + " D/${tag}: ${msg}"
        //FirebaseCrashlytics.getInstance().log(logMsg);
        return Log.d(tag, msg)
    }

    @JvmStatic
    fun w(tag: String, msg: String): Int {
        //val logMsg = DATE_FORMAT.format(Date()) + " W/${tag}: ${msg}"
        //FirebaseCrashlytics.getInstance().log(logMsg);
        return Log.w(tag, msg)
    }
}