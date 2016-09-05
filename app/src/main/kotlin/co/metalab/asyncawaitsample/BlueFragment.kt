package co.metalab.asyncawaitsample

import android.app.Fragment
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import co.metalab.asyncawait.async

class BlueFragment : Fragment() {

   private lateinit var btnTest: Button

   override fun onCreateView(inflater: LayoutInflater,
                             container: ViewGroup?,
                             savedInstanceState: Bundle?): View? {
      val view = inflater.inflate(R.layout.fragment_blue, container, false)

      btnTest = view.findViewById(R.id.btnFragmentBlueTestMemoryLeaks) as Button
      btnTest.setOnClickListener {
         testMemoryLeaks()
      }

      return view
   }

   private fun testMemoryLeaks() = async {
      btnTest.text = "Loading fragment task"
      btnTest.text = await {
         SystemClock.sleep(50000)
         Log.d("BlueFragment", "Task is done")
         "Fragment task done"
      }
      Log.d("BlueFragment", "Result delivered in UI thread")
   }


}


