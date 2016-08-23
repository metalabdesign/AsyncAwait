package coroutineandroid

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.example.coroutineandroid.R
import hugo.weaving.DebugLog
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button.setOnClickListener {
            //startCoroutine()
            //startCoroutineUsingMoreConvenientErrorHandling()
            startCoroutineWithProgress()
        }
    }

    private fun startCoroutine() = asyncUI {
        progress.visibility = View.VISIBLE
        text.text = "Loading..."
        try {
            // Release main thread and wait until text loaded
            // Progress animation shown during loading
            val loadedText = await(::loadText)
            // Loaded successfully, come back in UI thread and show result
            text.text = loadedText + " (to be processed)"
            // Oh ah we need to run more processing in background
            text.text = await { processText(loadedText) }
        } catch (e: Exception) {
            // Exception could be thrown in UI or background thread
            // but handled in UI thread
            text.text = e.message
        }
        progress.visibility = View.INVISIBLE
    }

    private fun startCoroutineUsingMoreConvenientErrorHandling() = asyncUI {
        progress.visibility = View.VISIBLE
        text.text = "Loading..."
        // Release main thread and wait until text loaded
        // Progress animation shown during loading
        val loadedText = await(::loadText)
        // Loaded successfully, come back in UI thread and show result
        text.text = loadedText + " (to be processed)"
        // Oh ah we need to run more processing in background
        text.text = await { processText(loadedText) }
        progress.visibility = View.INVISIBLE
    }.onError {
        text.text = it.message
        progress.visibility = View.INVISIBLE
    }


    private fun startCoroutineWithProgress() = asyncUI {
        button.isEnabled = false
        progress.visibility = View.VISIBLE
        progressValues.visibility = View.VISIBLE
        text.text = "Loading..."

        val loadedText = awaitWithProgress(::loadTextWithProgress) {
            progressValues.progress = it.curr
            progressValues.max = it.max
        }

        progressValues.visibility = View.INVISIBLE
        text.text = loadedText + " (to be processed)"
        text.text = await { processText(loadedText) }

        progress.visibility = View.INVISIBLE
        button.isEnabled = true
    }
}

@DebugLog
private fun loadText(): String {
    Thread.sleep(1000)
    if (1 == 1) throw RuntimeException("You are in the wrong place")
    return "Loaded Text"
}

@DebugLog
private fun loadTextWithProgress(p: Progress): String {
    p.max = 10
    for (i in 1..10) {
        p.curr = i
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