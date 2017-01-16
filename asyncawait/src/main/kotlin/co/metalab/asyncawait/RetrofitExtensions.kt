@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package co.metalab.asyncawait

import retrofit2.Call
import retrofit2.Response

/**
 * Waits successful execution of retrofit's [call]. If request finished with HTTP error the
 * custom [RetrofitHttpError] will be thrown.
 *
 * @return The awaited result - a body of successful response
 */
suspend fun <V> AsyncController.awaitSuccessful(call: Call<V>) : V = this.await {
   val response = call.execute()
   if (response.isSuccessful) {
      response.body()
   } else {
      throw RetrofitHttpError(response)
   }
}

class RetrofitHttpError(val errorResponse: Response<*>) : RuntimeException("${errorResponse.code()}")