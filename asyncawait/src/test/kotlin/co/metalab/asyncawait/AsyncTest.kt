package co.metalab.asyncawait

import android.app.Activity
import android.app.Fragment
import android.os.Handler
import android.os.Looper
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeoutException
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
class AsyncTest {

   @Test
   fun `Normal async-await usage`() {
      var result = ""
      var done = false
      async {
         result = await { "O" } + "K"
         done = true
      }

      loopUntil { done }

      assertEquals("OK", result)
   }

   @Test
   fun `Await block executed in background thread, result delivered in UI thread`() {
      var result = ""
      var done = false
      assertEquals(Looper.getMainLooper(), Looper.myLooper(), "1. Test runs on UI thread")
      async {
         assertEquals(Looper.getMainLooper(), Looper.myLooper(), "2. Continue on UI thread")

         result = await {
            assertNull(Looper.myLooper(), "4. Long operation runs on background thread")
            "O"
         }
         assertEquals(Looper.getMainLooper(), Looper.myLooper(), "5. Result delivered in UI thread")
         result += "K"
         done = true
      }

      assertFalse(done, "3. Long running operation is not done yet")

      loopUntil { done }

      assertEquals("OK", result, "6. Test is done")
   }

   @Test(expected = RuntimeException::class)
   fun `Unhandled exception in background thread delivered to system`() {
      async {
         await { throw RuntimeException("Catch me!") }
         @Suppress("UNREACHABLE_CODE")
         fail("Exception should be thrown before this point")
      }
      loopUntil { false }
   }

   @Test
   fun `Exception from background thread can be caught outside await block using try-catch in UI thread`() {
      var done = false
      async {
         try {
            await { throw RuntimeException("Catch me!") }
            @Suppress("UNREACHABLE_CODE")
            fail("Exception should be thrown before this point")
         } catch (e: RuntimeException) {
            assertTrue(e is RuntimeException, "Exception thrown in await block should be caught here")
            assertEquals(Looper.getMainLooper(), Looper.myLooper())
            done = true
         }
      }
      loopUntil { done }
   }

   @Test
   fun `Exception from background thread can be caught in onError block in UI thread`() {
      var done = false
      async {
         await { throw RuntimeException("Catch me!") }
         @Suppress("UNREACHABLE_CODE")
         fail("Exception should be thrown before this point")
      }.onError { e ->
         assertTrue(e is RuntimeException, "Exception thrown in await block should be caught here")
         assertEquals(Looper.getMainLooper(), Looper.myLooper())
         done = true
      }
      loopUntil { done }
   }

   @Test
   fun `Catch block is ignored when error block is specified`() {
      var done = false
      async {
         try {
            await { throw RuntimeException("Catch me!") }
         } catch (e: RuntimeException) {
            fail("onError block should handle this exception")
         }
      }.onError { e ->
         assertTrue(e is RuntimeException, "Exception thrown in await block should be caught here")
         done = true
      }
      loopUntil { done }
   }

   @Test
   fun `Await with progress`() {
      var result = ""
      var progressValues = ""
      var done = false

      fun load(publishProgress: (Int) -> Unit): String {
         for (i in 1..3) {
            // Publish progress changes
            publishProgress(i)
         }
         return "OK"
      }

      async {
         result = awaitWithProgress(::load) {
            // Handle progress changes
            progressValues += it
         }

         done = true
      }

      loopUntil { done }

      assertEquals("OK", result)
      assertEquals("123", progressValues)
   }

   @Test
   fun `Deliver result when Activity is alive`() {
      val activity = Mockito.mock(Activity::class.java)
      Mockito.`when`(activity.isFinishing).thenReturn(false)

      var result = ""
      var done = false
      activity.async {
         result = "O"
         result += await { "K" }
         done = true
      }

      loopUntil { done }
      assertEquals("OK", result, "Full result from await block")
   }

   @Test
   fun `Do not deliver result when Activity is finishing`() {
      val activity = Mockito.mock(Activity::class.java)

      var result = ""
      var done = false
      activity.async {
         result = "O"
         Mockito.`when`(activity.isFinishing).thenReturn(true)
         result += await { done = true; "K" }
         done = true
      }

      loopUntil { done }
      assertEquals("O", result, "`K` shouldn't be delivered as activity is in finishing state")
   }

   @Test
   fun `Deliver result when Fragment is alive`() {
      val fragment = Mockito.mock(Fragment::class.java)
      Mockito.`when`(fragment.activity).thenReturn(Mockito.mock(Activity::class.java))
      Mockito.`when`(fragment.isDetached).thenReturn(false)

      var result = ""
      var done = false
      fragment.async {
         result = "O"
         result += await { "K" }
         done = true
      }

      loopUntil { done }
      assertEquals("OK", result, "Full result from await block")
   }

   @Test
   fun `Do not deliver result when Fragment is detached`() {
      val fragment = Mockito.mock(Fragment::class.java)
      Mockito.`when`(fragment.activity).thenReturn(Mockito.mock(Activity::class.java))

      var result = ""
      var done = false
      fragment.async {
         result = "O"
         Mockito.`when`(fragment.isDetached).thenReturn(true)
         result += await { done = true; "K" }
         done = true
      }

      loopUntil { done }
      assertEquals("O", result, "`K` shouldn't be delivered as activity is in finishing state")
   }

   @Test
   fun `Do not deliver result when Fragments activity instance is null`() {
      val fragment = Mockito.mock(Fragment::class.java)
      Mockito.`when`(fragment.isDetached).thenReturn(true)

      var result = ""
      var done = false
      fragment.async {
         result = "O"
         Mockito.`when`(fragment.activity).thenReturn(null)
         result += await { done = true; "K" }
         done = true
      }

      loopUntil { done }
      assertEquals("O", result, "`K` shouldn't be delivered as activity is in finishing state")
   }

   private fun loopUntil(timeout: Int = 5000,
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
}