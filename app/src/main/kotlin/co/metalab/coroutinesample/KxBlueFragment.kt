package co.metalab.coroutinesample

import android.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import co.metalab.asyncawaitsample.R
import co.metalab.util.longRunningTask
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch

class KxBlueFragment : Fragment() {

    private lateinit var btnTest: Button
    private var job: Job = Job()

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_blue, container, false)

        btnTest = view.findViewById<Button>(R.id.btnFragmentBlueTestMemoryLeaks)
        btnTest.setOnClickListener {
            launch(job + UI) { testMemoryLeaks() }
        }

        return view
    }


    suspend private fun testMemoryLeaks() {
        btnTest.text = "Loading fragment task"
        btnTest.text = doTask().await()
        Log.d("BlueFragment", "Result delivered in UI thread")
    }

    suspend private fun doTask() = async {
        longRunningTask(3000)
        Log.d("BlueFragment", "Task is done")
        "Fragment task done"
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }
}