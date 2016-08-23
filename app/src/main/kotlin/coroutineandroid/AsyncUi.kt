package coroutineandroid

import android.app.Activity
import android.app.Fragment
import android.os.Handler
import android.os.Looper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.Continuation

private val executor = Executors.newSingleThreadExecutor()

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
        executor.submit {
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
        }
    }

    suspend fun <V, P, M> awaitWithProgress(f: ((P, M) -> Unit) -> V, p: (P, M) -> Unit, machine: Continuation<V>) {
        executor.submit {
            try {
                val value = f { curr, max ->
                    uiHandler.post {
                        if (isAlive()) {
                            p(curr, max)
                        }
                    }
                }
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
        }
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