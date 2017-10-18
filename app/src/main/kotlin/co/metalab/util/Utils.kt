package co.metalab.util

/**
 * Emulate long running task.
 * Throws exception when executed in Android main thread.
 */
fun longRunningTask(millis: Long) {
   if (Thread.currentThread().name == "main") {
      throw IllegalStateException("You have called longRunningTask from the main thread")
   }
   Thread.sleep(millis)
}