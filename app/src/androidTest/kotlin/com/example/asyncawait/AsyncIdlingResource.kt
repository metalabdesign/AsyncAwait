package com.example.asyncawait

import android.support.test.espresso.IdlingResource
import co.metalab.asyncawait.onIdleCoroutines
import co.metalab.asyncawait.onRunningCoroutine

class AsyncIdlingResource : IdlingResource {

    private var areCoroutinesIdle = true
    private var callback: IdlingResource.ResourceCallback? = null

    override fun getName(): String = "AsyncIdlingResource"

    override fun isIdleNow(): Boolean = areCoroutinesIdle

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        this.callback = callback
        onRunningCoroutine = {
            areCoroutinesIdle = false
            callback?.onTransitionToIdle()
        }
        onIdleCoroutines = {
            areCoroutinesIdle = true
            callback?.onTransitionToIdle()
        }
    }
}
