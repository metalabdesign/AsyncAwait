package co.metalab.asyncawait

import rx.Observable

/**
 * Waits in background until [observable] emits first value.
 *
 * @result the first value emitted by [observable]
 */
suspend fun <V> AsyncController.await(observable: Observable<V>, machine: Continuation<V>) {
   this.await({ observable.toBlocking().first() }, machine)
}

