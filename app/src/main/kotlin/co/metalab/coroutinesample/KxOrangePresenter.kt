package co.metalab.coroutinesample

import android.util.Log
import kotlinx.coroutines.experimental.async

class KxOrangePresenter(val orangeView: KxOrangeView) {

    suspend fun startLongRunningOrangeTask() {
        orangeView.setOrangeButtonText("Orange task in progress...")
        val newButtonText = orangeTask().await()
        orangeView.setOrangeButtonText(newButtonText)
        Log.d("OrangePresenter", "Orange task result delivered into UI thread")
    }

    private fun orangeTask() = async {
        Thread.sleep(3000)
        Log.d("OrangePresenter", "Orange task is done")
        "Orange task is done"
    }

}

interface KxOrangeView {
    fun setOrangeButtonText(text: String)
}