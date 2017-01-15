package co.metalab.asyncawait

import android.util.Log
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog
import kotlin.test.assertEquals

@Suppress("EXPERIMENTAL_FEATURE_WARNING")
@RunWith(RobolectricTestRunner::class)
class FinallyTest {

   @Before
   @Throws(Exception::class)
   fun setUp() {
      ShadowLog.stream = System.out
   }

   @Test
   fun `Run finally block`() {
      var result = ""
      var done1 = false
      var done2 = false

      async {
         result += "start"
         result += await { "+O" }
         done1 = true

         result += await { "K" }
         result += "+continue"
      }.finally {
         result += "+finally"
         done2 = true
      }

      loopUntil { done1 }
      loopUntil { done2 }

      assertEquals("start+OK+continue+finally", result)

      Log.d("Result", result)
   }

   @Suppress("UNREACHABLE_CODE")
   @Test
   fun `Run finally block after catching exception`() {
      var done = false
      var result = ""
      async {
         result += "start"
         try {
            result += "+try"
            await { throw RuntimeException("Catch me!") }
            result += "unreachable_code"
         } catch (e: RuntimeException) {
            result += "+catch"
            done = true
         }
         result += "+continue"
      }.finally {
         result += "+finally"
      }
      loopUntil { done }
      assertEquals("start+try+catch+continue+finally", result)
   }

   @Suppress("UNREACHABLE_CODE")
   @Test
   fun `Run finally block after handling exception in onError`() {
      var done = false
      var result = ""
      async {
         result += "start"

         await { throw RuntimeException("Catch me!") }
         result += "unreachable_code"
      }.onError {
         result += "+onError"
      }.finally {
         result += "+finally"
         done = true
      }
      loopUntil { done }
      assertEquals("start+onError+finally", result)
   }


}
