package co.metalab.asyncawaitsample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import co.metalab.asyncawait.AsyncController
import co.metalab.asyncawait.async
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_github.*
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

class GitHubActivity : AppCompatActivity() {
   private val TAG = GitHubActivity::class.java.simpleName

   var retrofit = Retrofit.Builder()
         .baseUrl("https://api.github.com/")
         .addConverterFactory(GsonConverterFactory.create())
         .build()

   var github = retrofit.create(GitHubService::class.java)

   var reposList = emptyList<Repo>()

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContentView(R.layout.activity_github)
      btnGetRepos.setOnClickListener { refreshRepos() }
   }

   private fun refreshRepos() = async {
      txtRepos.text = ""
      progressBar.isIndeterminate = true
      progressBar.visibility = View.VISIBLE
      btnGetRepos.isEnabled = false
      txtStatus.text = "Loading repos list..."

      val userName = txtUserName.text.toString()
      reposList = await(github.listRepos(userName))
      showRepos(reposList)

      progressBar.isIndeterminate = false
      reposList.forEachIndexed { i, repo ->
         txtStatus.text = "Loading info for ${repo.name}..."
         progressBar.progress = i * 100 / reposList.size
         val repoDetails = await(github.repoDetails(userName, repo.name))
         repo.stars = repoDetails.stargazers_count
         showRepos(reposList)
      }

      progressBar.visibility = View.INVISIBLE
      btnGetRepos.isEnabled = true
      txtStatus.text = "Done."
   }.onError {
      progressBar.visibility = View.INVISIBLE
      btnGetRepos.isEnabled = true

      val errorMessage = if (it is RetrofitHttpException) {
         val httpErrorCode = it.errorResponse.code()
         val errorResponse = Gson().fromJson(it.errorResponse.errorBody().string(), GithubErrorResponse::class.java)
         "[$httpErrorCode] ${errorResponse.message}"
      } else {
         "Couldn't load repos (${it.message})"
      }

      txtStatus.text = errorMessage
      Log.e(TAG, errorMessage, it)
   }

   private fun showRepos(reposResponse: List<Repo>) {
      val reposList = reposResponse.joinToString(separator = "\n") {
         val starsCount = if (it.stars == null) "" else {
            " * ${it.stars}"
         }
         it.name + starsCount
      }
      txtRepos.text = reposList
   }
}

interface GitHubService {
   @GET("users/{user}/repos")
   fun listRepos(@Path("user") user: String): Call<List<Repo>>

   @GET("repos/{user}/{repo}")
   fun repoDetails(@Path("user") user: String, @Path("repo") repo: String): Call<Repo>
}

data class Repo(val name: String, val stargazers_count: Int, var stars: Int? = null)

data class GithubErrorResponse(val message: String, val documentation_url: String)