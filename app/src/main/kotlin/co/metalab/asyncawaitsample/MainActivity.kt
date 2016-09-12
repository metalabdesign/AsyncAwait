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
   private val TAG = this::class.java.simpleName

   private lateinit var orangePresenter: OrangePresenter

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContentView(R.layout.activity_main)

      btnAwaitNormal.setOnClickListener {
         awaitNormal()
      }

      btnAwaitWithProgress.setOnClickListener {
         awaitWithProgress()
      }

      btnThrowException.setOnClickListener {
         throwException()
      }

      btnTryCatchException.setOnClickListener {
         tryCatchException()
      }

      btnHandleExceptionInOnError.setOnClickListener {
         handleExceptionInOnError()
      }

      orangePresenter = OrangePresenter(this)
      btnOrangeTestMemoryLeaks.setOnClickListener {
         orangePresenter.startLongRunningOrangeTask()
      }

      btnOpenGithubActivity.setOnClickListener {
         startActivity(Intent(this, GitHubActivity::class.java))
      }
   }

   private fun awaitNormal() = async {
      btnAwaitNormal.isEnabled = false
      progressBar.visibility = View.VISIBLE
      progressBar.isIndeterminate = true
      txtResult.text = "Loading..."
      // Release main thread and wait until text is loaded
      val loadedText = await(::loadText)
      // Loaded successfully, come back in UI thread and show result
      txtResult.text = loadedText + " (to be processed)"
      // Have to continue processing in background
      txtResult.text = await { processText(loadedText) }
      progressBar.visibility = View.INVISIBLE
      btnAwaitNormal.isEnabled = true
   }

   private fun awaitWithProgress() = async {
      btnAwaitWithProgress.isEnabled = false
      progressBar.visibility = View.VISIBLE
      progressBar.isIndeterminate = false
      txtResult.text = "Loading..."

      txtResult.text = awaitWithProgress(::loadTextWithProgress) {
         progressBar.progress = it
         progressBar.max = 100
      }

      progressBar.visibility = View.INVISIBLE
      btnAwaitWithProgress.isEnabled = true
   }

   @Suppress("UNREACHABLE_CODE")
   private fun throwException() = async {
      btnThrowException.isEnabled = false
      progressBar.visibility = View.VISIBLE
      progressBar.isIndeterminate = true
      txtResult.text = "Loading..."
      await {
         throw RuntimeException("Test exception")
      }
      txtResult.text = "Should never be displayed"
      progressBar.visibility = View.INVISIBLE
      btnThrowException.isEnabled = true
   }

   @Suppress("UNREACHABLE_CODE")
   private fun tryCatchException() = async {
      btnTryCatchException.isEnabled = false
      progressBar.visibility = View.VISIBLE
      progressBar.isIndeterminate = true
      txtResult.text = "Loading..."
      try {
         await {
            throw RuntimeException("Test exception")
         }
         txtResult.text = "Should never be displayed"
      } catch (e: Exception) {
         // Exception always handled in UI thread
         txtResult.text = e.message
         btnTryCatchException.text = "Handled. see the log"
         Log.e(TAG, "Couldn't update text", e)
      }
      progressBar.visibility = View.INVISIBLE
      btnTryCatchException.isEnabled = true
   }

   @Suppress("UNREACHABLE_CODE")
   private fun handleExceptionInOnError() = async {
      btnHandleExceptionInOnError.isEnabled = false
      progressBar.visibility = View.VISIBLE
      progressBar.isIndeterminate = true
      txtResult.text = "Loading..."

      await {
         throw RuntimeException("Test exception")
      }

      txtResult.text = "Should never be displayed"
      progressBar.visibility = View.INVISIBLE
      btnHandleExceptionInOnError.isEnabled = true
   }.onError {
      // Exception always handled in UI thread
      txtResult.text = it.message
      btnHandleExceptionInOnError.text = "Handled. see the log"
      progressBar.visibility = View.INVISIBLE
      btnHandleExceptionInOnError.isEnabled = true
      Log.e(TAG, "Couldn't update text", it)
   }

   override fun setOrangeButtonText(text: String) {
      btnOrangeTestMemoryLeaks.text = text
   }

   override fun onDestroy() {
      super.onDestroy()
      orangePresenter.onStop()
      async.cancelAll()
   }
}

@DebugLog
private fun loadText(): String {
   SystemClock.sleep(1000)
   return "Loaded Text"
}

@DebugLog
private fun loadTextWithProgress(handleProgress: ProgressHandler<Int>): String {
   for (i in 1..10) {
      handleProgress(i * 100 / 10) // in %
      SystemClock.sleep(300)
   }
   return "Loaded Text"
}

@DebugLog
private fun processText(input: String): String {
   SystemClock.sleep(10000)
   return "Processed $input"
}