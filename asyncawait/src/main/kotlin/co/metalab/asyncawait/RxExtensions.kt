@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package co.metalab.asyncawait

import rx.Observable

/**
 * Waits in background until [observable] emits first value.
 *
 * @result the first value emitted by [observable]
 */
suspend fun <V> AsyncController.await(observable: Observable<V>): V = this.await {
   observable.toBlocking().first()
}

