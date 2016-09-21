package co.metalab.asyncawaitsample

import android.app.Application
import com.squareup.leakcanary.LeakCanary

class MainApp : Application() {
   override fun onCreate() {
      super.onCreate()
      LeakCanary.install(this)
   }
}