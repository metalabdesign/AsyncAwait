package co.metalab.asyncawait

import retrofit2.Call
import retrofit2.Response

/**
 * Waits result of retrofit's api [call] execution.
 *
 * @return [Response] is a result of executed [call] in background thread.
 */
suspend fun <V> AsyncController.await(call: Call<V>, machine: Continuation<Response<V>>) {
   this.await({ call.execute() }, machine)
}

/**
 * Waits successful execution of retrofit's [call]. If request finished with HTTP error the
 * custom [RetrofitHttpError] will be thrown.
 *
 * @return The awaited result - a body of successful response
 */
suspend fun <V> AsyncController.awaitSuccessful(call: Call<V>, machine: Continuation<V>) {
   this.await({
      val response = call.execute()
      if (response.isSuccessful) {
         response.body()
      } else {
         throw RetrofitHttpError(response)
      }
   }, machine)
}

class RetrofitHttpError(val errorResponse: Response<*>) : RuntimeException("${errorResponse.code()}")