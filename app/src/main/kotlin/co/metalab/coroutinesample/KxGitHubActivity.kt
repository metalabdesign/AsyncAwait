package co.metalab.coroutinesample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import co.metalab.asyncawait.RetrofitHttpError
import co.metalab.asyncawaitsample.R
import co.metalab.service.GitHubService
import co.metalab.service.GithubErrorResponse
import co.metalab.service.Repo
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_github.*
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class KxGitHubActivity : AppCompatActivity() {
    private val TAG = KxGitHubActivity::class.java.simpleName
    private var job: Job = Job()

    var retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    var github = retrofit.create(GitHubService::class.java)

    var reposList = emptyList<Repo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_github)
        btnGetRepos.setOnClickListener {
            launch(job + UI, CoroutineStart.UNDISPATCHED) { refreshRepos() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    suspend private fun refreshRepos() {
        showLoadingUi()

        try {
            val userName = txtUserName.text.toString()
            reposList = asyncListRepos(userName).await()
            showRepos(reposList)

            progressBar.isIndeterminate = false
            reposList.forEachIndexed { i, repo ->
                txtStatus.text = "Loading info for ${repo.name}..."
                progressBar.progress = i * 100 / reposList.size
                val repoDetails = asyncRepoDetails(userName, repo).await()
                repo.stars = repoDetails.stargazers_count
                showRepos(reposList)
            }

            txtStatus.text = "Done."
        } catch (e: Exception) {
            val errorMessage = getErrorMessage(e)

            txtStatus.text = errorMessage
            Log.e(TAG, errorMessage, e)
        } finally {
            progressBar.visibility = View.INVISIBLE
            btnGetRepos.isEnabled = true
        }

    }

    private fun asyncRepoDetails(userName: String, repo: Repo) = async {
        val response = github.repoDetails(userName, repo.name).execute()
        if (response.isSuccessful) {
            response.body()
        } else {
            throw RetrofitHttpError(response)
        }
    }

    fun asyncListRepos(userName: String) = async {
        val response = github.listRepos(userName).execute()
        if (response.isSuccessful) {
            response.body()
        } else {
            throw RetrofitHttpError(response)
        }
    }

    private fun showLoadingUi() {
        txtRepos.text = ""
        progressBar.isIndeterminate = true
        progressBar.visibility = View.VISIBLE
        btnGetRepos.isEnabled = false
        txtStatus.text = "Loading repos list..."
    }

    private fun getErrorMessage(it: Throwable): String {
        return if (it is RetrofitHttpError) {
            val httpErrorCode = it.errorResponse.code()
            val errorResponse = Gson().fromJson(it.errorResponse.errorBody().string(), GithubErrorResponse::class.java)
            "[$httpErrorCode] ${errorResponse.message}"
        } else {
            "Couldn't load repos (${it.message})"
        }
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