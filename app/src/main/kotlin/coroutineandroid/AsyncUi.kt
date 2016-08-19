package coroutineandroid

import android.app.Activity
import android.app.Fragment
import android.os.Handler
import android.os.Looper
import kotlin.coroutines.Continuation

fun asyncUI(coroutine c: AsyncController.() -> Continuation<Unit>): AsyncController {
    return asyncUI(c, AsyncController())
}

fun Activity.asyncUI(coroutine c: AsyncController.() -> Continuation<Unit>): AsyncController {
    return asyncUI(c, AsyncController(activity = this))
}

fun Fragment.asyncUI(coroutine c: AsyncController.() -> Continuation<Unit>): AsyncController {
    return asyncUI(c, AsyncController(fragment = this))
}

internal fun asyncUI(c: AsyncController.() -> Continuation<Unit>, controller: AsyncController): AsyncController {
    // TODO If not in UI thread - force run resume() in UI thread
    controller.c().resume(Unit)
    return controller
}

typealias ErrorHandler = (Exception) -> Unit

class AsyncController(val activity: Activity? = null,
                      val fragment: Fragment? = null) {
    private var errorHandler: ErrorHandler? = null
    private val uiHandler = Handler(Looper.getMainLooper())

    suspend fun <V> await(f: () -> V, machine: Continuation<V>) {
        Thread {
            try {
                val value = f()
                uiHandler.post {
                    if (isAlive()) {
                        machine.resume(value)
                    }
                }
            } catch (e: Exception) {
                uiHandler.post {
                    if (isAlive()) {
                        errorHandler?.invoke(e) ?: machine.resumeWithException(e)
                    }
                }
            }
        }.start()

    }

    fun onError(errorHandler: ErrorHandler) {
        this.errorHandler = errorHandler
    }

    private fun isAlive(): Boolean {
        if (activity != null) {
            return !activity.isFinishing
        } else if (fragment != null) {
            return fragment.context != null && !fragment.isDetached
        } else {
            return false
        }
    }

}