package co.metalab.asyncawait

import android.app.Activity
import android.app.Application
import android.os.Bundle

interface ActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
   override fun onActivityPaused(activity: Activity) {
   }

   override fun onActivityStarted(activity: Activity) {
   }

   override fun onActivityDestroyed(destroyedActivity: Activity) {
   }

   override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle?) {
   }

   override fun onActivityStopped(activity: Activity) {
   }

   override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
   }

   override fun onActivityResumed(activity: Activity) {
   }

}