# CoroutineAndroid
Experimental implementation of async/await for Android using Kotlin Coroutines

This is very basic implementation of what could be achieved using _suspendable computation_ aka _coroutines_ on Android. 
For example
```Kotlin
asyncUI {
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
}
```

Debug log for method invocations (take a look for threads)
```
 ⇢ startCoroutineUsingMoreConvenientErrorHandling()
 ⇠ startCoroutineUsingMoreConvenientErrorHandling [4ms]
 ⇢ loadText() [Thread:"Thread-206"]
 ⇠ loadText [1003ms] = "Loaded Text"
 ⇢ processText(input="Loaded Text") [Thread:"Thread-207"]
 ⇠ processText [2013ms] = "Processed Loaded Text"
 ```
 
# How to run
 
 1. Install Kotlin plugin in Android Studio and download EAP of Kotlin 1.1-M01 `Tools->Kotlin->Configure Kotlin Plugin updates->Select EAP 1.1->Check for updates`
 2. Checkout this repo and import as gradle project
