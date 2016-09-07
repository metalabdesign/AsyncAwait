package co.metalab.asyncawaitsample

import android.os.SystemClock
import android.util.Log
import co.metalab.asyncawait.async

class OrangePresenter(val orangeView: OrangeView) {

   fun startLongRunningOrangeTask() = async {
      orangeView.setOrangeButtonText("Orange task in progress...")
      val newButtonText = await {
         SystemClock.sleep(15000)
         Log.d("OrangePresenter", "Orange task is done")
         "Orange task is done"
      }
      orangeView.setOrangeButtonText(newButtonText)
      Log.d("OrangePresenter", "Orange task result delivered into UI thread")
   }

   fun onStop() {
      async.cancelAll()
   }

}

interface OrangeView {
   fun setOrangeButtonText(text: String)
}
