package co.metalab.coroutinesample

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import co.metalab.asyncawait.ProgressHandler
import co.metalab.asyncawaitsample.R
import hugo.weaving.DebugLog
import kotlinx.android.synthetic.main.activity_coroutines.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch

class CoroutineMainActivity : AppCompatActivity(), KxOrangeView {
    private lateinit var orangePresenter: KxOrangePresenter
    private val job: Job = Job()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coroutines)

        btnAwaitNormal.setOnClickListener {
            launch(UI) { awaitNormal() }
        }

        btnAwaitWithProgress.setOnClickListener {
            launch(UI) { awaitWithProgress() }
        }

        btnThrowException.setOnClickListener {
            launch(UI) { throwException() }
        }

        btnTryCatchException.setOnClickListener {
            launch(UI) { tryCatchException() }
        }

        orangePresenter = KxOrangePresenter(this)
        btnOrangeTestMemoryLeaks.setOnClickListener {
            orangePresenter.startLongRunningOrangeTask()
        }

        btnOpenGithubActivity.setOnClickListener {
            startActivity(Intent(this, KxGitHubActivity::class.java))
        }
    }

    override fun onDestroy() {
        job.cancel()
        orangePresenter.stop()
        super.onDestroy()
    }

    suspend private fun awaitNormal() {
        btnAwaitNormal.isEnabled = false
        progressBar.visibility = View.VISIBLE
        progressBar.isIndeterminate = true
        txtResult.text = "Loading..."
        // Release main thread and wait until text is loaded
        val loadedText = loadTextSus()
        // Loaded successfully, come back in UI thread and show result
        txtResult.text = loadedText + " (to be processed)"
        // Have to continue processing in background
        txtResult.text = processTextSus(loadedText)
        progressBar.visibility = View.INVISIBLE
        btnAwaitNormal.isEnabled = true
    }

    suspend private fun awaitWithProgress() {
        btnAwaitWithProgress.isEnabled = false
        progressBar.visibility = View.VISIBLE
        progressBar.isIndeterminate = false
        txtResult.text = "Loading..."

        txtResult.text = loadTextWithProgress {
            progressBar.progress = it
            progressBar.max = 100
        }

        progressBar.visibility = View.INVISIBLE
        btnAwaitWithProgress.isEnabled = true
    }

    @Suppress("UNREACHABLE_CODE")
    suspend private fun throwException() {
        btnThrowException.isEnabled = false
        progressBar.visibility = View.VISIBLE
        progressBar.isIndeterminate = true
        txtResult.text = "Loading..."

        justThrow()

        txtResult.text = "Should never be displayed"
        progressBar.visibility = View.INVISIBLE
        btnThrowException.isEnabled = true
    }

    @Suppress("UNREACHABLE_CODE")
    suspend private fun tryCatchException() {
        btnTryCatchException.isEnabled = false
        progressBar.visibility = View.VISIBLE
        progressBar.isIndeterminate = true
        txtResult.text = "Loading..."
        try {
            justThrow()
            txtResult.text = "Should never be displayed"
        } catch (e: Exception) {
            // Exception always handled in UI thread
            txtResult.text = e.message
            btnTryCatchException.text = "Handled"
        }
        progressBar.visibility = View.INVISIBLE
        btnTryCatchException.isEnabled = true
    }

    override fun setOrangeButtonText(text: String) {
        btnOrangeTestMemoryLeaks.text = text
    }

    @DebugLog
    suspend private fun loadTextSus(): String {
        delay(1000)
        return "Loaded Text"
    }

    @DebugLog
    suspend private fun processTextSus(input: String): String {
        delay(1000)
        return "Processed $input"
    }

    @DebugLog
    suspend private fun loadTextWithProgress(handleProgress: ProgressHandler<Int>): String {
        for (i in 1..10) {
            handleProgress(i * 100 / 10) // in %
            delay(300)
        }
        return "Loaded Text"
    }

    @DebugLog
    suspend private fun justThrow() {
        throw RuntimeException("Test exception")
    }

}