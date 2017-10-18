package co.metalab.asyncawaitsample

import android.util.Log
import co.metalab.asyncawait.async
import co.metalab.util.longRunningTask

class OrangePresenter(val orangeView: OrangeView) {

   fun startLongRunningOrangeTask() = async {
      orangeView.setOrangeButtonText("Orange task in progress...")
      val newButtonText = await {
         longRunningTask(15000)
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
