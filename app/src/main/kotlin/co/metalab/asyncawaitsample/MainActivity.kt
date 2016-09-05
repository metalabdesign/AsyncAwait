package co.metalab.asyncawaitsample

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import co.metalab.asyncawait.ProgressHandler
import co.metalab.asyncawait.async
import hugo.weaving.DebugLog
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), OrangeView {

   private lateinit var orangePresenter: OrangePresenter

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContentView(R.layout.activity_main)
      btnStart.setOnClickListener {
         //startCoroutine()
         //startCoroutineUsingMoreConvenientErrorHandling()
         startCoroutineWithProgress()
      }
      btnOpenGithubActivity.setOnClickListener {
         startActivity(Intent(this, GitHubActivity::class.java))
      }
      btnTestMemoryLeaks.setOnClickListener {
         startVeryLongTask()
         System.gc()
      }
      orangePresenter = OrangePresenter(this)
      btnOrangeTestMemoryLeaks.setOnClickListener {
         orangePresenter.startLongRunningOrangeTask()
      }
   }

   private fun startCoroutine() = async {
      progressBar.visibility = View.VISIBLE
      txtResult.text = "Loading..."
      try {
         // Release main thread and wait until text loaded
         // Progress animation shown during loading
         val loadedText = await(::loadText)
         // Loaded successfully, come back in UI thread and show result
         txtResult.text = loadedText + " (to be processed)"
         // Oh ah we need to run more processing in background
         txtResult.text = await { processText(loadedText) }
      } catch (e: Exception) {
         // Exception could be thrown in UI or background thread
         // but handled in UI thread
         txtResult.text = e.message
      }
      progressBar.visibility = View.INVISIBLE
   }

   private fun startCoroutineUsingMoreConvenientErrorHandling() = async {
      progressBar.visibility = View.VISIBLE
      txtResult.text = "Loading..."
      // Release main thread and wait until text loaded
      // Progress animation shown during loading
      val loadedText = await(::loadText)
      // Loaded successfully, come back in UI thread and show result
      txtResult.text = loadedText + " (to be processed)"
      // Oh ah we need to run more processing in background
      txtResult.text = await { processText(loadedText) }
      progressBar.visibility = View.INVISIBLE
   }.onError {
      txtResult.text = it.message
      progressBar.visibility = View.INVISIBLE
   }


   private fun startCoroutineWithProgress() = async {
      btnStart.isEnabled = false
      progressBar.visibility = View.VISIBLE
      progressBar.isIndeterminate = false
      txtResult.text = "Loading..."

      val loadedText = awaitWithProgress(::loadTextWithProgress) {
         progressBar.progress = it
         progressBar.max = 100
      }

      progressBar.isIndeterminate = true
      txtResult.text = loadedText + " (to be processed)"
      txtResult.text = await { processText(loadedText) }

      progressBar.visibility = View.INVISIBLE
      btnStart.isEnabled = true
   }

   private fun startVeryLongTask() = async {
      btnTestMemoryLeaks.text = "Press Back, watch leaks..."
      btnTestMemoryLeaks.text = await {
         SystemClock.sleep(10000)
         Log.d("MainActivity", "Task is done")
         "Done. So, did you see leaks?"
      }
      Log.d("MainActivity", "Result delivered in UI thread")
   }

   override fun setOrangeButtonText(text: String) {
      btnOrangeTestMemoryLeaks.text = text
   }

}

@DebugLog
private fun loadText(): String {
   Thread.sleep(1000)
   if (1 == 1) throw RuntimeException("You are in the wrong place")
   return "Loaded Text"
}

@DebugLog
private fun loadTextWithProgress(handleProgress: ProgressHandler<Int>): String {
   for (i in 1..10) {
      handleProgress(i * 100 / 10) // in %
      Thread.sleep(300)
   }
   //if (1 == 1) throw RuntimeException("You are in the wrong place")
   return "Loaded Text"
}

@DebugLog
private fun processText(input: String): String {
   Thread.sleep(2000)
   return "Processed $input"
}