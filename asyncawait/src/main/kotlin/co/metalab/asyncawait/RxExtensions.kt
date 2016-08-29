package co.metalab.asyncawait

import rx.Observable

suspend fun <V> AsyncController.await(observable: Observable<V>, machine: Continuation<V>) {
   this.await({ observable.toBlocking().first() }, machine)
}

