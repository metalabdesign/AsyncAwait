@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package co.metalab.asyncawait

import android.app.Activity
import android.app.Fragment
import android.os.Handler
import android.os.Looper
import android.os.Message
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.experimental.*

private val executors = WeakHashMap<Any, ExecutorService>()

private val coroutines = WeakHashMap<Any, ArrayList<WeakReference<AsyncController>>>()

private val allCoroutines: Collection<WeakReference<AsyncController>>
   get() = coroutines.flatMap { it.value }

var onRunningCoroutine: (() -> Unit)? = null
    set(value) {
        if (allCoroutines.isNotEmpty()) { value?.invoke() }
        field = value
    }
var onIdleCoroutines: (() -> Unit)? = null
   set(value) {
       if (allCoroutines.isEmpty()) { value?.invoke() }
       field = value
   }

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
 * @return AsyncController object allowing to define optional [AsyncController.onError]
 * or [AsyncController.finally] handlers
 */
fun Any.async(c: suspend AsyncController.() -> Unit): AsyncController {
   val controller = AsyncController(this)
   keepCoroutineForCancelPurpose(controller)
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
fun Activity.async(c: suspend AsyncController.() -> Unit): AsyncController {
   val controller = AsyncController(this)
   keepCoroutineForCancelPurpose(controller)
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
fun Fragment.async(c: suspend AsyncController.() -> Unit): AsyncController {
   val controller = AsyncController(this)
   keepCoroutineForCancelPurpose(controller)
   return async(c, controller)
}

internal fun async(c: suspend AsyncController.() -> Unit,
                   controller: AsyncController): AsyncController {
   c.startCoroutine(controller, completion = object : Continuation<Unit> {
      override val context: CoroutineContext = EmptyCoroutineContext

      override fun resume(value: Unit) {
      }

      override fun resumeWithException(exception: Throwable) {
         throw exception
      }

   })
   return controller
}

typealias ErrorHandler = (Exception) -> Unit

typealias ProgressHandler<P> = (P) -> Unit

/**
 * Controls coroutine execution and thread scheduling
 */
class AsyncController(private val target: Any) {

   private var errorHandler: ErrorHandler? = null
   private var finallyHandler: (() -> Unit)? = null

   private val uiHandler = object : Handler(Looper.getMainLooper()) {
      override fun handleMessage(msg: Message) {
         if (isAlive()) {
            @Suppress("UNCHECKED_CAST")
            (msg.obj as () -> Unit)()
         }
      }
   }

   internal var currentTask: CancelableTask<*>? = null

   private lateinit var uiThreadStackTrace: Array<out StackTraceElement>

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
   suspend fun <V> await(f: () -> V): V {
      onRunningCoroutine?.invoke()
      keepAwaitCallerStackTrace()
      return suspendCoroutine {
         currentTask = AwaitTask(f, this, it)
         target.getExecutorService().submit(currentTask)
      }
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
   suspend fun <V, P> awaitWithProgress(
       f: (ProgressHandler<P>) -> V,
       onProgress: ProgressHandler<P>
   ): V {
      onRunningCoroutine?.invoke()
      keepAwaitCallerStackTrace()
      return suspendCoroutine {
         currentTask = AwaitWithProgressTask(f, onProgress, this, it)
         target.getExecutorService().submit(currentTask)
      }
   }

   /**
    * Optional error handler. Exceptions happening in a background thread and not caught within
    * try/catch in coroutine body will be delivered to this handler in UI thread.
    *
    *  @return this AsyncController object allowing to define optional [finally] handlers
    */
   fun onError(errorHandler: ErrorHandler): AsyncController {
      this.errorHandler = errorHandler
      return this
   }

   /**
    * Optional handler to be invoked after successful coroutine execution
    * or after handling exception in [onError].
    */
   fun finally(finallyHandler: () -> Unit) {
      this.finallyHandler = finallyHandler
   }

   internal fun cancel() {
      currentTask?.cancel()
   }

   internal fun <V> handleException(originalException: Exception, continuation: Continuation<V>) {
      runOnUi {
         currentTask = null

         try {
            continuation.resumeWithException(originalException)
         } catch (e: Exception) {
            val asyncException = AsyncException(e, refineUiThreadStackTrace())
            errorHandler?.invoke(asyncException) ?: throw asyncException
         }

         applyFinallyBlock()
      }
   }

   internal fun applyFinallyBlock() {
      if (isLastCoroutineResumeExecuted()) {
         finallyHandler?.invoke()
         onIdleCoroutines?.invoke()
      }
   }

   private fun isLastCoroutineResumeExecuted() = currentTask == null

   private fun isAlive(): Boolean {
      return when (target) {
         is Activity -> return !target.isFinishing
         is Fragment -> return target.activity != null && !target.isDetached
         else -> true
      }
   }

   internal fun runOnUi(block: () -> Unit) {
      uiHandler.obtainMessage(0, block).sendToTarget()
   }

   private fun keepAwaitCallerStackTrace() {
      uiThreadStackTrace = Thread.currentThread().stackTrace
   }

   private fun refineUiThreadStackTrace(): Array<out StackTraceElement> {
      return uiThreadStackTrace
          .dropWhile { it.methodName != "keepAwaitCallerStackTrace" }
          .drop(2)
          .toTypedArray()
   }

}

private fun Any.keepCoroutineForCancelPurpose(controller: AsyncController) {
   val list = coroutines.getOrElse(this) {
      val newList = ArrayList<WeakReference<AsyncController>>()
      coroutines[this] = newList
      newList
   }

   list.add(WeakReference(controller))
}

private fun Any.getExecutorService(): ExecutorService {
   val threadName = "AsyncAwait-${this::class.java.simpleName}"
   return executors.getOrElse(this) {
      val newExecutor = Executors.newSingleThreadExecutor(AsyncThreadFactory(threadName))
      executors[this] = newExecutor
      newExecutor
   }
}

private class AsyncThreadFactory(val name: String) : ThreadFactory {
   private var counter = 0
   override fun newThread(r: Runnable): Thread {
      counter++
      return Thread(r, "$name-$counter")
   }
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

internal abstract class CancelableTask<V>(@Volatile
                                          var asyncController: AsyncController?,
                                          @Volatile
                                          var continuation: Continuation<V>?) : Runnable {

   private val isCancelled = AtomicBoolean(false)

   open internal fun cancel() {
      isCancelled.set(true)
      asyncController = null
      continuation = null
   }

   override fun run() {
      // Finish task immediately if it was cancelled while being in queue
      if (isCancelled.get()) return

      try {
         val value = obtainValue()
         if (isCancelled.get()) return
         asyncController?.apply {
            runOnUi {
               currentTask = null
               continuation?.resume(value)
               applyFinallyBlock()
            }
         }

      } catch (e: Exception) {
         if (isCancelled.get()) return

         continuation?.apply {
            asyncController?.handleException(e, this)
         }
      }
   }

   abstract fun obtainValue(): V
}

private class AwaitTask<V>(val f: () -> V,
                           asyncController: AsyncController,
                           continuation: Continuation<V>)
: CancelableTask<V>(asyncController, continuation) {

   override fun obtainValue(): V {
      return f()
   }
}

private class AwaitWithProgressTask<P, V>(val f: (ProgressHandler<P>) -> V,
                                          @Volatile
                                          var onProgress: ProgressHandler<P>?,
                                          asyncController: AsyncController,
                                          continuation: Continuation<V>)
: CancelableTask<V>(asyncController, continuation) {

   override fun obtainValue(): V {
      return f { progressValue ->
         onProgress?.apply {
            asyncController?.runOnUi { this(progressValue) }
         }
      }
   }

   override fun cancel() {
      super.cancel()
      onProgress = null
   }

}

class AsyncException(e: Exception, stackTrace: Array<out StackTraceElement>) : RuntimeException(e) {
   init {
      this.stackTrace = stackTrace
   }
}