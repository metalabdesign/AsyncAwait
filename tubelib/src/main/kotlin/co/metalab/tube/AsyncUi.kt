package co.metalab.tube

import android.app.Activity
import android.app.Fragment
import android.os.Handler
import android.os.Looper
import android.os.Message
import java.util.concurrent.Executors

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

internal fun asyncUI(c: AsyncController.() -> Continuation<Unit>,
                     controller: AsyncController): AsyncController {
   // TODO If not in UI thread - force run resume() in UI thread
   controller.c().resume(Unit)
   return controller
}

typealias ErrorHandler = (Exception) -> Unit

typealias ProgressHandler<P> = (P) -> Unit

@AllowSuspendExtensions
class AsyncController(val activity: Activity? = null, val fragment: Fragment? = null) {
   private var errorHandler: ErrorHandler? = null
   private val uiHandler = object : Handler(Looper.getMainLooper()) {
      override fun handleMessage(msg: Message) {
         if (isAlive()) {
            @Suppress("UNCHECKED_CAST")
            (msg.obj as () -> Unit)()
         }
      }
   }

   suspend fun <V> await(f: () -> V, machine: Continuation<V>) {
      executor.submit {
         try {
            val value = f()
            runOnUi { machine.resume(value) }
         } catch (e: Exception) {
            handleException(e, machine)
         }
      }
   }

   suspend fun <V, P> awaitWithProgress(f: (ProgressHandler<P>) -> V,
                                        onProgress: ProgressHandler<P>, machine: Continuation<V>) {
      executor.submit {
         try {
            val value = f { progressValue ->
               runOnUi { onProgress(progressValue) }
            }
            runOnUi { machine.resume(value) }
         } catch (e: Exception) {
            handleException(e, machine)
         }
      }
   }

   fun onError(errorHandler: ErrorHandler) {
      this.errorHandler = errorHandler
   }

   private fun <V> handleException(e: Exception, machine: Continuation<V>) {
      runOnUi { errorHandler?.invoke(e) ?: machine.resumeWithException(e) }
   }

   private fun isAlive(): Boolean {
      if (activity != null) {
         return !activity.isFinishing
      } else if (fragment != null) {
         return fragment.activity != null && !fragment.isDetached
      } else {
         return true
      }
   }

   private fun runOnUi(block: () -> Unit) {
      uiHandler.obtainMessage(0, block).sendToTarget()
   }

}