package co.metalab.asyncawait

import retrofit2.Call
import retrofit2.Response

suspend fun <V> AsyncController.await(call: Call<V>, machine: Continuation<Response<V>>) {
   this.await({ call.execute() }, machine)
}

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