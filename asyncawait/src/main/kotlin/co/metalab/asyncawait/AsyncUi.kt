package co.metalab.asyncawait

import android.app.Activity
import android.app.Fragment
import android.os.Handler
import android.os.Looper
import android.os.Message
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private val executor = Executors.newSingleThreadExecutor()

private val coroutines = WeakHashMap<Any, ArrayList<WeakReference<AsyncController>>>()

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
fun Any.async(coroutine c: AsyncController.() -> Continuation<Unit>): AsyncController {
   val controller = AsyncController()
   trackCoroutineForCancelPurpose(controller)
   return async(c, controller)
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
   val controller = AsyncController(activity = this)
   trackCoroutineForCancelPurpose(controller)
   return async(c, controller)
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
   val controller = AsyncController(fragment = this)
   trackCoroutineForCancelPurpose(controller)
   return async(c, controller)
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
class AsyncController(private val activity: Activity? = null,
                      private val fragment: Fragment? = null) {

   private var errorHandler: ErrorHandler? = null

   private val uiHandler = object : Handler(Looper.getMainLooper()) {
      override fun handleMessage(msg: Message) {
         if (isAlive()) {
            @Suppress("UNCHECKED_CAST")
            (msg.obj as () -> Unit)()
         }
      }
   }

   private var currentTask: AwaitTask<*>? = null

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
      currentTask = AwaitTask(f, this, machine)
      executor.submit(currentTask)
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

   internal fun cancel() {
      currentTask?.cancel()
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

}

private fun Any.trackCoroutineForCancelPurpose(controller: AsyncController) {
   val list = coroutines.getOrElse(this) {
      val newList = ArrayList<WeakReference<AsyncController>>()
      coroutines[this] = newList
      newList
   }

   list.add(WeakReference(controller))
}

val Any.async: Async
   get() = Async(this)

class Async(private val asyncTarget: Any) {
   fun cancelAll() {
      coroutines[asyncTarget]?.forEach {
         it.get()?.cancel()
      }
   }
}

private class AwaitTask<V>(val f: () -> V,
                           @Volatile
                           var asyncController: AsyncController?,
                           @Volatile
                           var machine: Continuation<V>?) : Runnable {

   private val isCancelled = AtomicBoolean(false)

   internal fun cancel() {
      asyncController = null
      machine = null
      isCancelled.set(true)
   }

   override fun run() {
      // Finish task immediately if it was cancelled while being in queue
      if (isCancelled.get()) return

      try {
         val value = f()
         machine?.apply {
            asyncController?.runOnUi { this.resume(value) }
         }

      } catch (e: Exception) {
         machine?.apply {
            asyncController?.handleException(e, this)
         }
      }
   }

}
