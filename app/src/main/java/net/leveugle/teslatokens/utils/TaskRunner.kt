package net.leveugle.teslatokens.utils

import android.os.Handler
import android.os.Looper
import net.leveugle.teslatokens.utils.MyLog
import java.lang.Exception
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class TaskRunner {
    private val executor: Executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    interface Callback<R> {
        fun onComplete(result: R)
        fun onError(exc: Exception)
    }

    fun <R> executeAsync(callable: Callable<R>, callback: Callback<R>) {
        executor.execute {
            try {
                val result = callable.call()
                handler.post { callback.onComplete(result) }
            } catch (e: Exception) {
                MyLog.e("TaskRunner", "Exception", e)
                handler.post { callback.onError(e) }
            }
        }
    }
}