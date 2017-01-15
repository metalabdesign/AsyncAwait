package co.metalab.asyncawait

import android.os.Looper
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

@Suppress("EXPERIMENTAL_FEATURE_WARNING")
@RunWith(RobolectricTestRunner::class)
class HandleExceptionTest {

   @Test
   fun `onError block never called if exception is caught in try-catch`() {
      var done = false
      async {
         try {
            await { throw CustomException("Catch me!") }
         } catch (e: CustomException) {
            assertEquals("Catch me!", e.message)
            done = true
         }
      }.onError {
         fail("onError block should never be called if exception caught in try/catch")
      }
      loopUntil { done }
   }

   @Test
   fun `onError block called if exception is not caught in try-catch`() {
      var done = false
      async {
         try {
            await { throw RuntimeException("Catch me!") }
         } catch (e: CustomException) {
            fail("catch block should never be called as we don't catch only CustomException here")
         }
      }.onError { e ->
         assertEquals("java.lang.RuntimeException: Catch me!", e.message)
         done = true
      }
      loopUntil { done }
   }

   @Test
   fun `Exception from background thread can be caught outside await block using try-catch in UI thread`() {
      var done = false
      async {
         try {
            await { throw CustomException("Catch me!") }
            @Suppress("UNREACHABLE_CODE")
            fail("Exception should be thrown before this point")
         } catch (e: CustomException) {
            assertTrue(e is CustomException, "Exception thrown in await block should be caught here")
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
   fun `Exception handled in onError is wrapped by AsyncException`() {
      var done = false
      async {
         await {
            throw RuntimeException("Catch me!")
         }
      }.onError { e ->
         assertTrue(e is AsyncException)
         assertTrue(e.cause is RuntimeException)
         assertEquals("co.metalab.asyncawait.HandleExceptionTest\$Exception handled in onError is wrapped by AsyncException$1", e.stackTrace[0].className)
         assertEquals("doResume", e.stackTrace[0].methodName)
         done = true
      }
      loopUntil { done }
   }

   @Test(expected = AsyncException::class)
   fun `Unhandled exception in background thread delivered to system`() {
      async {
         await { throw CustomException("Catch me!") }
         @Suppress("UNREACHABLE_CODE")
         fail("Exception should be thrown before this point")
      }
      loopUntil { false }
   }

}

class CustomException(message: String) : Exception(message)