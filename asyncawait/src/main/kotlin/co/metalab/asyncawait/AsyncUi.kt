package co.metalab.asyncawait

import android.app.Activity
import android.app.Fragment
import android.os.Handler
import android.os.Looper
import android.os.Message
import java.lang.ref.WeakReference
import java.util.concurrent.Executors

private val executor = Executors.newSingleThreadExecutor()

/**
 * Run asynchronous computations based on [c] coroutine parameter.
 * Must be used in UI thread.
 *
 * Execution starts immediately within the 'async' call and it runs until
 * the first suspension point is reached ('await' call).
 * Remaining part of coroutine will be executed after awaited task is completed
 * and result is delivered into UI thread.
 *
 * @param c a coroutine representing asynchronous computations
 *
 * @return AsyncController object allowing to define optional `onError` handler
 */
fun async(coroutine c: AsyncController.() -> Continuation<Unit>): AsyncController {
   return async(c, AsyncController())
}

/**
 * Run asynchronous computations based on [c] coroutine parameter.
 * Prevents suspended coroutine to be resumed when [Activity] is in finishing state.
 * Must be used in UI thread.
 *
 * Execution starts immediately within the 'async' call and it runs until
 * the first suspension point is reached ('await' call).
 * Remaining part of coroutine will be executed after awaited task is completed
 * and [Activity] is not in finishing state. Result is delivered into UI thread.
 *
 * @param c a coroutine representing asynchronous computations
 *
 * @return AsyncController object allowing to define optional `onError` handler
 */
fun Activity.async(coroutine c: AsyncController.() -> Continuation<Unit>): AsyncController {
   return async(c, AsyncController(activity = this))
}

/**
 * Run asynchronous computations based on [c] coroutine parameter.
 * Prevents suspended coroutine to be resumed when [Fragment] is in invalid state.
 * Must be used in UI thread.
 *
 * Execution starts immediately within the 'async' call and it runs until
 * the first suspension point is reached ('await' call).
 * Remaining part of coroutine will be executed after awaited task is completed
 * and [Fragment] has parental [Activity] instance and it's in attached state.
 * Result is delivered into UI thread.
 *
 * @param c a coroutine representing asynchronous computations
 *
 * @return AsyncController object allowing to define optional `onError` handler
 */
fun Fragment.async(coroutine c: AsyncController.() -> Continuation<Unit>): AsyncController {
   return async(c, AsyncController(fragment = this))
}

internal fun async(c: AsyncController.() -> Continuation<Unit>,
                   controller: AsyncController): AsyncController {
   controller.c().resume(Unit)
   return controller
}

typealias ErrorHandler = (Exception) -> Unit

typealias ProgressHandler<P> = (P) -> Unit

/**
 * Controls coroutine execution and thread scheduling
 */
@AllowSuspendExtensions
class AsyncController(private var activity: Activity? = null,
                      private var fragment: Fragment? = null) : ActivityLifecycleCallbacks {
   init {
      activity?.apply {
         application.registerActivityLifecycleCallbacks(this@AsyncController)
      }
   }

   private var errorHandler: ErrorHandler? = null
   private val uiHandler = object : Handler(Looper.getMainLooper()) {
      override fun handleMessage(msg: Message) {
         if (isAlive()) {
            @Suppress("UNCHECKED_CAST")
            (msg.obj as () -> Unit)()
         }
      }
   }

   private var machineRefHolder: Continuation<*>? = null

   /**
    * Non-blocking suspension point. Coroutine execution will proceed after [f] is finished
    * in background thread.
    *
    * @param f a function to call in background thread. The result of [f] will be delivered
    * into UI thread.
    *
    * @return the result of [f] delivered in UI thread after computation is done
    * in background thread
    */
   suspend fun <V> await(f: () -> V, machine: Continuation<V>) {
      machineRefHolder = machine //Hold reference to protect it from GC
      val task = if (activity != null) {
         WeakAwaitTask(f, WeakReference(this), WeakReference(machine))
      } else {
         StrongAwaitTask(f, this, machine)
      }
      executor.submit(task)
   }

   /**
    * Non-blocking suspension point. Similar to [await] but its function [f] has functional parameter
    * which can be called right in background thread in order to send progress value into
    * [onProgress] progress handler.
    *
    * @param f a function to call in background thread. Its functional parameter
    * `ProgressHandler<P>` can be called in background thread in order to send progress result.
    *
    * @param onProgress a function to handle progress result. Called in UI thread.
    *
    * @return the result of [f] delivered in UI thread after computation is done
    * in background thread
    */
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

   /**
    * Optional error handler. Any exception happening in background thread
    * will be delivered to this handler in UI thread. This handler has more priority than
    * try/catch blocks around [await].
    */
   fun onError(errorHandler: ErrorHandler) {
      this.errorHandler = errorHandler
   }

   internal fun <V> handleException(e: Exception, machine: Continuation<V>) {
      runOnUi { errorHandler?.invoke(e) ?: machine.resumeWithException(e) }
   }

   private fun isAlive(): Boolean {
      activity?.apply { return !isFinishing }
      fragment?.apply { return activity != null && !isDetached }

      return true
   }

   internal fun runOnUi(block: () -> Unit) {
      uiHandler.obtainMessage(0, block).sendToTarget()
   }

   override fun onActivityDestroyed(destroyedActivity: Activity) {
      if (destroyedActivity != activity) return

      activity?.apply {
         application.unregisterActivityLifecycleCallbacks(this@AsyncController)
      }
   }

}

private class StrongAwaitTask<V>(val f: () -> V,
                                 val asyncController: AsyncController,
                                 val machine: Continuation<V>) : Runnable {
   override fun run() {
      try {
         val value = f()
         asyncController.runOnUi { machine.resume(value) }
      } catch (e: Exception) {
         asyncController.handleException(e, machine)
      }
   }

}

private class WeakAwaitTask<V>(val f: () -> V,
                               val asyncControllerWeakRef: WeakReference<AsyncController>,
                               val machineWeakRef: WeakReference<Continuation<V>>) : Runnable {
   override fun run() {
      if (asyncControllerWeakRef.get() == null) return

      try {
         val value = f()
         machineWeakRef.get()?.apply {
            asyncControllerWeakRef.get()?.runOnUi { this.resume(value) }
         }
      } catch (e: Exception) {
         machineWeakRef.get()?.apply {
            asyncControllerWeakRef.get()?.handleException(e, this)
         }
      }
   }

}