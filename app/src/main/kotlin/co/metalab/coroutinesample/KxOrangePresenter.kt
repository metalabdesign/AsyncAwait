package co.metalab.coroutinesample

import android.util.Log
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.CoroutineContext

class KxOrangePresenter(val orangeView: KxOrangeView,
                        coroutineDispatcher : CoroutineContext = UI) {
    private val job: Job = Job()
    // Coroutine context
    private val cc = job + coroutineDispatcher

   fun startLongRunningOrangeTask() = launch(cc) {
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

    fun stop() {
       job.cancel()
    }

}

interface KxOrangeView {
    fun setOrangeButtonText(text: String)
}