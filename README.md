# Async/Await
A Kotlin library for Android allowing writing asynchronous code in synchronous style using `async`/`await` approach

```Kotlin
async {
   progressBar.visibility = View.VISIBLE
   // Release main thread and wait until text is loaded in background thread
   val loadedText = await { loadFromServer() }
   // Loaded successfully, come back in UI thread and show the result
   txtResult.text = loadedText
   progressBar.visibility = View.INVISIBLE
}
```
The point is that you can write asynchronous code in a simple imperative style. Calling `await` to run code in background doesn't lock UI thread. Then execution _continues_ in UI thread after background work is finished. There is no magic, see [how it works](#how-it-works).


## Usage
### `async`

Coroutine code has to be passed as a lambda in `async` function
```Kotlin
async {
   // Coroutine body
}
```

### `await`

Long running code has to be passed as a lambda in `await` function
```Kotlin
async {
   val result = await {
      //Long running code
   }
   // Use result
}
```
You may have many `await` calls inside `async` block, or have `await` in a loop

```Kotlin 
async {
   val repos = await { github.getRepos() }
   showList(repos)
   repos.forEach { repo ->
      val stats = await { github.getStats(repo.name) }
      showStats(repo, stats)
   }
}
```

### `awaitWithProgress`

Use it to show loading progress, its second parameter is a progress handler.
```Kotlin
val loadedText = awaitWithProgress(::loadTextWithProgress) {
         // Called in UI thread
         progressBar.progress = it
         progressBar.max = 100
      }
```
A data loading function (like the `loadTextWithProgress` above) should has a functional parameter of type `(P) -> Unit` which can be called in order to push progress value. For example, it could be like:
```Kotlin
private fun loadTextWithProgress(handleProgress: (Int) -> Unit): String {
   for (i in 1..10) {
      handleProgress(i * 100 / 10) // in %
      Thread.sleep(300)
   }
   return "Loaded Text"
}
```

### Handle exceptions using `try/catch`

```Kotlin
async {
   try {
      val loadedText = await {
         // throw exception in background thread
      }
      // Process loaded text
   } catch (e: Exception) {
      // Handle exception in UI thread
   }
}
```

### Handle exceptions using `onError` block

Using `onError` can be more convenient because resulting code has fewer indents. `onError`, when defined, has more priority than `try/catch`.
```Kotlin
async {
   val loadedText = await {
      // throw exception in background thread
   }
   // Process loaded text
}.onError {
   // Handle exception in UI thread
}
```

### Safe execution

The library has `Activity.async` and `Fragment.async` extension functions to produce more safe code. So when using `async` inside Activity/Fragment, coroutine won't be resumed if `Activity` is in finishing state or `Fragment` is detached.

### Avoid memory leaks

Long running background code referencing any view/context may produce memory leaks. To avoid such memory leaks, call `async.cancelAll()` when all running coroutines referencing current object should be interrupted, like
```Kotlin
override fun onDestroy() {
      super.onDestroy()
      async.cancelAll()
}
```
The `async` is an extension property for `Any` type. So calling `[this.]async.cancelAll` intrerrupts only coroutines started by `[this.]async {}` function.

### Common extensions

The library has a convenient API to work with Retrofit and rxJava.

#### For Retorift
* `await(retrofit2.Call)`
```Kotlin
reposResponse = await(github.listRepos(userName))
```

* `awaitSuccessful(retrofit2.Call)`

Returns `Response<V>.body()` if successful, or throws `RetrofitHttpError` with error response otherwise.  
```Kotlin
reposList = awaitSuccessful(github.listRepos(userName))
```
#### For rxJava
* await(Observable<V>)

Waits until `observable` emits first value.
```Kotlin
async {
   val observable = Observable.just("O")
   result = await(observable)
}
```

###How to create custom extensions
You can create your own `await` implementations. Here is example to give you idea 
```Kotlin
suspend fun <V> AsyncController.await(observable: Observable<V>, machine: Continuation<V>) {
   this.await({ observable.toBlocking().first() }, machine)
}
```

##How it works

The library is built upon [coroutines](https://github.com/Kotlin/kotlin-coroutines/blob/master/kotlin-coroutines-informal.md) introduced in [Kotlin 1.1](https://blog.jetbrains.com/kotlin/2016/07/first-glimpse-of-kotlin-1-1-coroutines-type-aliases-and-more/).

The Kotlin compiler responsibility is to convert _coroutine_ (everything inside `async` block) into a state machine, where every `await` call is non-blocking suspension point. The library is responsible for thread handling and managing state machine. When background computation is done the library delivers result back into UI thread and resumes _coroutine_ execution.

# How to use it
Add library dependency into your app's `build.gradle`
```Groovy
compile 'co.metalab.asyncawait:asyncawait:0.5'
```

As for now Kotlin 1.1 is not released yet, you have to download and setup latest Early Access Preview release. 
* Go to `Tools` -> `Kotlin` -> `Configure Kotlin Plugin updates` -> `Select EAP 1.1` -> `Check for updates` and install latest one. 
* Make sure you have similar config in the main `build.gradle`
```
buildscript {
    ext.kotlin_version = '1.1-M01'
    repositories {
        ...
        maven {
            url "http://dl.bintray.com/kotlin/kotlin-eap-1.1"
        }
    }
    dependencies {
        // (!) 2.1.2 This version is more stable with Kotlin for now
        classpath 'com.android.tools.build:gradle:2.1.2'
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
    }
}
```
* Make sure you have similar config in app's `build.gradle`
```
buildscript {
    repositories {
        ...
        maven {
            url "http://dl.bintray.com/kotlin/kotlin-eap-1.1"
        }
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:2.1.2'
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
    }
}
```
and this section for getting latest kotlin-stdlib
```
repositories {
    mavenCentral()
    maven {
        url "http://dl.bintray.com/kotlin/kotlin-eap-1.1"
    }
}
```
