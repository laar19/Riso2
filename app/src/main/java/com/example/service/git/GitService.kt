package com.example.service.git

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GitService {
    private val TAG = "GitService"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // --- GITHUB REAL API INTEGRATIONS ---

    suspend fun listGithubRepositories(
        token: String,
        targetUsername: String? = null
    ): Map<String, Any> = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        val url = if (!targetUsername.isNullOrBlank()) {
            "https://api.github.com/users/$targetUsername/repos?sort=updated&per_page=15"
        } else {
            "https://api.github.com/user/repos?sort=updated&per_page=15"
        }

        try {
            val reqBuilder = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Riso-Android-Agent")
            if (cleanToken.isNotBlank()) {
                reqBuilder.header("Authorization", "Bearer $cleanToken")
            }

            val response = client.newCall(reqBuilder.build()).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext mapOf(
                    "success" to false,
                    "error" to "GitHub API error (${response.code}): $body"
                )
            }

            val jsonArray = JSONArray(body)
            val reposList = mutableListOf<Map<String, Any>>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                reposList.add(
                    mapOf(
                        "name" to obj.optString("name"),
                        "full_name" to obj.optString("full_name"),
                        "description" to obj.optString("description", "Sin descripción"),
                        "stars" to obj.optInt("stargazers_count", 0),
                        "forks" to obj.optInt("forks_count", 0),
                        "language" to obj.optString("language", "N/A"),
                        "html_url" to obj.optString("html_url"),
                        "private" to obj.optBoolean("private", false)
                    )
                )
            }

            mapOf(
                "success" to true,
                "total_count" to reposList.size,
                "repositories" to reposList
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error listing GitHub repositories", e)
            mapOf("success" to false, "error" to (e.localizedMessage ?: "Error desconocido en GitHub API"))
        }
    }

    suspend fun listGithubIssues(
        token: String,
        owner: String,
        repo: String,
        state: String = "open"
    ): Map<String, Any> = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        val url = "https://api.github.com/repos/$owner/$repo/issues?state=$state&per_page=15"

        try {
            val reqBuilder = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Riso-Android-Agent")
            if (cleanToken.isNotBlank()) {
                reqBuilder.header("Authorization", "Bearer $cleanToken")
            }

            val response = client.newCall(reqBuilder.build()).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext mapOf(
                    "success" to false,
                    "error" to "GitHub API error (${response.code}): $body"
                )
            }

            val jsonArray = JSONArray(body)
            val issuesList = mutableListOf<Map<String, Any>>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                // Filter out pull requests if any
                if (obj.has("pull_request")) continue
                val userObj = obj.optJSONObject("user")
                issuesList.add(
                    mapOf(
                        "number" to obj.optInt("number"),
                        "title" to obj.optString("title"),
                        "state" to obj.optString("state"),
                        "author" to (userObj?.optString("login") ?: "anónimo"),
                        "comments" to obj.optInt("comments", 0),
                        "created_at" to obj.optString("created_at"),
                        "html_url" to obj.optString("html_url")
                    )
                )
            }

            mapOf(
                "success" to true,
                "owner" to owner,
                "repo" to repo,
                "state" to state,
                "issues" to issuesList
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error listing GitHub issues", e)
            mapOf("success" to false, "error" to (e.localizedMessage ?: "Error al consultar issues de GitHub"))
        }
    }

    suspend fun createGithubIssue(
        token: String,
        owner: String,
        repo: String,
        title: String,
        bodyText: String
    ): Map<String, Any> = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) {
            return@withContext mapOf(
                "success" to false,
                "error" to "Se requiere un token Personal Access Token (PAT) de GitHub para crear issues."
            )
        }

        val url = "https://api.github.com/repos/$owner/$repo/issues"
        val payload = JSONObject().apply {
            put("title", title)
            put("body", bodyText)
        }

        try {
            val req = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $cleanToken")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Riso-Android-Agent")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(req).execute()
            val respBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext mapOf(
                    "success" to false,
                    "error" to "Error al crear issue en GitHub (${response.code}): $respBody"
                )
            }

            val json = JSONObject(respBody)
            mapOf(
                "success" to true,
                "owner" to owner,
                "repo" to repo,
                "issue_number" to json.optInt("number"),
                "title" to json.optString("title"),
                "html_url" to json.optString("html_url")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating GitHub issue", e)
            mapOf("success" to false, "error" to (e.localizedMessage ?: "Error al crear issue en GitHub"))
        }
    }

    // --- GITLAB REAL API INTEGRATIONS ---

    suspend fun listGitlabProjects(
        instanceUrl: String,
        token: String,
        membership: Boolean = true
    ): Map<String, Any> = withContext(Dispatchers.IO) {
        val baseUrl = instanceUrl.ifBlank { "https://gitlab.com" }.trimEnd('/')
        val url = "$baseUrl/api/v4/projects?membership=$membership&per_page=15&order_by=updated_at"

        try {
            val reqBuilder = Request.Builder()
                .url(url)
                .header("User-Agent", "Riso-Android-Agent")
            if (token.isNotBlank()) {
                reqBuilder.header("PRIVATE-TOKEN", token.trim())
            }

            val response = client.newCall(reqBuilder.build()).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext mapOf(
                    "success" to false,
                    "error" to "GitLab API error (${response.code}): $body"
                )
            }

            val jsonArray = JSONArray(body)
            val projects = mutableListOf<Map<String, Any>>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                projects.add(
                    mapOf(
                        "id" to obj.optInt("id"),
                        "name" to obj.optString("name"),
                        "path_with_namespace" to obj.optString("path_with_namespace"),
                        "description" to obj.optString("description", "Sin descripción"),
                        "visibility" to obj.optString("visibility"),
                        "star_count" to obj.optInt("star_count", 0),
                        "web_url" to obj.optString("web_url")
                    )
                )
            }

            mapOf(
                "success" to true,
                "instance_url" to baseUrl,
                "projects" to projects
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error listing GitLab projects", e)
            mapOf("success" to false, "error" to (e.localizedMessage ?: "Error al consultar proyectos en GitLab"))
        }
    }

    suspend fun createGitlabIssue(
        instanceUrl: String,
        token: String,
        projectId: String,
        title: String,
        description: String
    ): Map<String, Any> = withContext(Dispatchers.IO) {
        val baseUrl = instanceUrl.ifBlank { "https://gitlab.com" }.trimEnd('/')
        if (token.isBlank()) {
            return@withContext mapOf(
                "success" to false,
                "error" to "Se requiere un Personal Access Token (PAT) de GitLab para crear issues."
            )
        }

        // URL encode projectId if it is a path like "owner/repo"
        val encodedProject = if (projectId.contains("/")) {
            java.net.URLEncoder.encode(projectId, "UTF-8")
        } else {
            projectId
        }
        val url = "$baseUrl/api/v4/projects/$encodedProject/issues"

        val payload = JSONObject().apply {
            put("title", title)
            put("description", description)
        }

        try {
            val req = Request.Builder()
                .url(url)
                .header("PRIVATE-TOKEN", token.trim())
                .header("User-Agent", "Riso-Android-Agent")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(req).execute()
            val respBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext mapOf(
                    "success" to false,
                    "error" to "Error al crear issue en GitLab (${response.code}): $respBody"
                )
            }

            val json = JSONObject(respBody)
            mapOf(
                "success" to true,
                "projectId" to projectId,
                "issue_id" to json.optInt("iid", json.optInt("id")),
                "title" to json.optString("title"),
                "web_url" to json.optString("web_url")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating GitLab issue", e)
            mapOf("success" to false, "error" to (e.localizedMessage ?: "Error al crear issue en GitLab"))
        }
    }
}
