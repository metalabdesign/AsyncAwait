package co.metalab.coroutinesample

import android.util.Log
import kotlinx.coroutines.experimental.delay

class KxOrangePresenter(val orangeView: KxOrangeView) {

    suspend fun startLongRunningOrangeTask() {
        orangeView.setOrangeButtonText("Orange task in progress...")
        val newButtonText = orangeTask()
        orangeView.setOrangeButtonText(newButtonText)
        Log.d("OrangePresenter", "Orange task result delivered into UI thread")
    }

    suspend private fun orangeTask(): String {
        delay(3000)
        Log.d("OrangePresenter", "Orange task is done")
        return "Orange task is done"
    }

}

interface KxOrangeView {
    fun setOrangeButtonText(text: String)
}