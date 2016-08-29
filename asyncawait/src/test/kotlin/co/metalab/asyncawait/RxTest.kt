package co.metalab.asyncawait

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import rx.Observable
import kotlin.test.assertEquals
import kotlin.test.fail

@RunWith(RobolectricTestRunner::class)
class RxTest {

   @Test
   fun `Normal usage`() {
      val observable = Observable.just("O")
      var result = ""
      var done = false
      async {
         result = await(observable)
         result += "K"
         done = true
      }

      loopUntil { done }
      assertEquals("OK", result)
   }

   @Test
   fun `Handle exception`() {
      val observable = Observable.error<String>(RuntimeException())
      var result = "never-changed"
      var done = false
      async {
         result = await(observable)
         fail()
      }.onError {
         done = true
      }

      loopUntil { done }
      assertEquals("never-changed", result)
   }

}

