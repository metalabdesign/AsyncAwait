package co.metalab.asyncawaitsample

import co.metalab.asyncawait.AsyncController
import retrofit2.Call
import retrofit2.Response

suspend fun <V> AsyncController.await(call: Call<V>, machine: Continuation<V>) {
   this.await({
      val response = call.execute()
      if (response.isSuccessful) {
         response.body()
      } else {
         throw RetrofitHttpException(response)
      }
   }, machine)
}

class RetrofitHttpException(val errorResponse: Response<*>) : RuntimeException()
