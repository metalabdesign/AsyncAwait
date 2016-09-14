package co.metalab.asyncawait

import android.os.Looper
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

@RunWith(RobolectricTestRunner::class)
class HandleExceptionTest {

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
   fun `Thrown exceptions should be wrapped with AsyncException`() {
      var done = false
      async {
         await {
            throw RuntimeException("Catch me!")
         }
      }.onError { e ->
         assertTrue(e is AsyncException)
         assertTrue(e.cause is RuntimeException)
         assertEquals("co.metalab.asyncawait.HandleExceptionTest\$Thrown exceptions should be wrapped with AsyncException$1", e.stackTrace[1].className)
         assertEquals("doResume", e.stackTrace[1].methodName)
         done = true
      }
      loopUntil { done }
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

}