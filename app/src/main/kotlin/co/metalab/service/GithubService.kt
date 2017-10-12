package co.metalab.service

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface GitHubService {
    @GET("users/{user}/repos")
    fun listRepos(@Path("user") user: String): Call<List<Repo>>

    @GET("repos/{user}/{repo}")
    fun repoDetails(@Path("user") user: String, @Path("repo") repo: String): Call<Repo>
}

data class Repo(val name: String, val stargazers_count: Int, var stars: Int? = null)

data class GithubErrorResponse(val message: String, val documentation_url: String)