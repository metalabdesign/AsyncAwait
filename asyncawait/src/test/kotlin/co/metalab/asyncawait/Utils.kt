package co.metalab.asyncawait

import android.os.Handler
import android.os.Looper
import java.util.concurrent.TimeoutException

internal fun loopUntil(timeout: Int = 5000,
                       throwExceptionOnTimeout: Boolean = true,
                       done: () -> Boolean) {
   val handler = Handler()
   val timeThreshold = System.currentTimeMillis() + timeout

   fun loopUntilDone() {
      if (System.currentTimeMillis() > timeThreshold) {
         if (throwExceptionOnTimeout)
            throw TimeoutException()
         else
            return
      }
      Thread.sleep(100)
      if (!done()) handler.post(::loopUntilDone)
   }

   loopUntilDone()

   Looper.loop()
}