package coroutineandroid

import android.os.Handler
import android.os.Looper
import kotlin.coroutines.Continuation

fun asyncUi(coroutine c: AsyncController.() -> Continuation<Unit>): AsyncController {
    val controller = AsyncController()
    // TODO If not in UI thread - force run resume() in UI thread
    controller.c().resume(Unit)
    return controller
}

class AsyncController {
    private var errorHandler: ErrorHandler? = null
    private val uiHandler = Handler(Looper.getMainLooper())

    suspend fun <V> await(f: () -> V, machine: Continuation<V>) {
        Thread {
            try {
                val value = f()
                uiHandler.post {
                    machine.resume(value)
                }
            } catch (e: Exception) {
                uiHandler.post {
                    errorHandler?.invoke(e) ?: machine.resumeWithException(e)
                }
            }

        }.start()

    }

    fun onError(errorHandler: ErrorHandler) {
        this.errorHandler = errorHandler
    }
}

typealias ErrorHandler = (Exception) -> Unit