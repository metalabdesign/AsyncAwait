## AsyncAwait
A Kotlin Android library allowing write asynchronous code in synchronous style using `async`/`await` approach, like:

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
As you can see, we don't have callback to be executed when `loadFromServer()` is done. All the code after calling `await` can be treated like a "callback". But there is no magic here. See How it works (LINK HERE).


## Usage
### `async`

Coroutine code have to be passed as a lambda in `async` function
```Kotlin
async {
   // Coroutine body
}
```

### `await`

Long running code have to be passed as a lambda in `await` function
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

Use to show loading progress, where second parameter is a progress handler.
```Kotlin
val loadedText = awaitWithProgress(::loadTextWithProgress) {
         progressBar.progress = it
         progressBar.max = 100
      }
```
The data loading function (like `loadTextWithProgress` above) should have a functional parameter of type `(P) -> Unit` which can be called on order to push progress value. For example above it could be 
```Kotlin
private fun loadTextWithProgress(handleProgress: (Int) -> Unit): String {
   for (i in 1..10) {
      handleProgress(i * 100 / 10) // in %
      Thread.sleep(300)
   }
   return "Loaded Text"
}
```

### Handle exception using `try/catch`

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

Could be more convenient as resulting code has less indents. Has more priority than `try/catch` around `await`.
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

There is `Actiivty.async` and `Fragment.async` extension functions. In order to use them the result won't be delivered if eg. `Activity` is finishing or `Fragment` is detached. 


### Common extensions
#### For Retorift
* `await(retrofit2.Call)`
```Kotlin
reposResponse = await(github.listRepos(userName))
```

* `awaitSuccessful(retrofit2.Call)`

Returns `Response<V>.body()` if successful, throw `RetrofitHttpError` with error response otherwise.  
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

### How to create extensions
You can create your own `await` implementations. Here is example to give you idea 
```Kotlin
suspend fun <V> AsyncController.await(observable: Observable<V>, machine: Continuation<V>) {
   this.await({ observable.toBlocking().first() }, machine)
}
```

## How it works

The library is built upon [coroutines](https://github.com/Kotlin/kotlin-coroutines/blob/master/kotlin-coroutines-informal.md) introduced in [Kotlin 1.1](https://blog.jetbrains.com/kotlin/2016/07/first-glimpse-of-kotlin-1-1-coroutines-type-aliases-and-more/).

The Kotlin compiler responsibility is to convert _coroutine_ (everything inside `async` block) into state machine, where every `await` call is non-blocking suspension point. The library is responsible for thread handling and state machine managing. When background computation is done the library delivers result back into UI thread and resumes _coroutine_ execution.


# How to use it
Add library dependency into your app's `build.gradle`
```Groovy
compile 'co.metalab.asyncawait:asyncawait:0.5'
```

As for now Kotlin 1.1 is not released yet, you have to download and setup latest Early Access Preview release. 
* Go to `Tools->Kotlin->Configure Kotlin Plugin updates->Select EAP 1.1->Check for updates` and install latest one. 
* Make sure you have similar config in the main `build.gradle`
```
buildscript {
    ext.kotlin_version = '1.1-M01'
    repositories {
        jcenter()
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
* Make sure you have similar config in app's `build.gradle`
```
buildscript {
    repositories {
        jcenter()
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
and this one for getting latest kotlin-stdlib
```
repositories {
    mavenCentral()
    maven {
        url "http://dl.bintray.com/kotlin/kotlin-eap-1.1"
    }
}
```