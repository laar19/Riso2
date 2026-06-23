package com.example.service.search

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class SearchResult(
    val title: String,
    val url: String,
    val snippet: String
)

class WebSearchService {
    private val TAG = "WebSearchService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // 1. DuckDuckGo HTML Scraper (FREE, No API key required)
    fun searchDuckDuckGo(query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://html.duckduckgo.com/html/?q=$encodedQuery"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "es-ES,es;q=0.8,en-US;q=0.5,en;q=0.3")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "DDG Scraper HTTP fail: ${response.code}")
                    return results
                }
                val html = response.body?.string() ?: ""
                
                // Parse results using regex patterns since we are on Android without JSoup initialized
                // DDG HTML structures search results in classes like "result__body" / "result__snippet" / "result__url"
                // Let's use robust string partitioning or simple regex to extract results.
                val resultBlocks = html.split("<div class=\"result results_links results_links_deep web-result")
                for (i in 1 until resultBlocks.size) {
                    if (results.size >= 5) break
                    val block = resultBlocks[i]
                    
                    // Extract URL and Title
                    // Typical: <a class="result__url" href="URL">...</a> or <a class="result__snippet" ...>
                    // <a class="result__snippet" href="http://...">
                    val urlMatcher = Pattern.compile("href=\"([^\"]+)\"").matcher(block)
                    var resultUrl = ""
                    if (urlMatcher.find()) {
                        val rawUrl = urlMatcher.group(1) ?: ""
                        // Extract actual URL out of DDG redirect if present
                        resultUrl = if (rawUrl.contains("uddg=")) {
                            val part = rawUrl.substringAfter("uddg=")
                            val encoded = part.substringBefore("&amp;")
                            java.net.URLDecoder.decode(encoded, "UTF-8")
                        } else {
                            rawUrl
                        }
                    }

                    val titleMatcher = Pattern.compile("<a class=\"result__a\"[^>]*>([^<]+)</a>").matcher(block)
                    var resultTitle = ""
                    if (titleMatcher.find()) {
                        resultTitle = titleMatcher.group(1)?.replace("&amp;", "&") ?: ""
                    }

                    val snippetMatcher = Pattern.compile("<a class=\"result__snippet\"[^>]*>([^<]+)</a>").matcher(block)
                    var resultSnippet = ""
                    if (snippetMatcher.find()) {
                        resultSnippet = snippetMatcher.group(1)?.replace("&amp;", "&") ?: ""
                    } else {
                        // Fallback: try parsing with <span class="result__snippet">
                        val spanMatcher = Pattern.compile("<span class=\"result__snippet\"[^>]*>([^<]+)</span>").matcher(block)
                        if (spanMatcher.find()) {
                            resultSnippet = spanMatcher.group(1)?.replace("&amp;", "&") ?: ""
                        }
                    }

                    // Clean tags/html residue
                    resultTitle = stripHtml(resultTitle).trim()
                    resultSnippet = stripHtml(resultSnippet).trim()

                    if (resultTitle.isNotEmpty() && resultUrl.isNotEmpty()) {
                        results.add(
                            SearchResult(
                                title = resultTitle,
                                url = resultUrl,
                                snippet = if (resultSnippet.isNotEmpty()) resultSnippet else "Ver resultado completo en $resultUrl"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scraping DuckDuckGo", e)
        }
        return results
    }

    // 2. Brave Search API (Requires Brave Subscription Key)
    fun searchBrave(query: String, apiKey: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        if (apiKey.isBlank()) {
            return results
        }
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.search.brave.com/res/v1/web/search?q=$encodedQuery&count=5"
            
            val request = Request.Builder()
                .url(url)
                .header("X-Subscription-Token", apiKey)
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Brave Search HTTP fail: ${response.code}")
                    return results
                }
                val bodyStr = response.body?.string() ?: ""
                val root = JSONObject(bodyStr)
                if (root.has("web")) {
                    val web = root.getJSONObject("web")
                    if (web.has("results")) {
                        val array = web.getJSONArray("results")
                        for (i in 0 until array.length()) {
                            val item = array.getJSONObject(i)
                            results.add(
                                SearchResult(
                                    title = item.optString("title"),
                                    url = item.optString("url"),
                                    snippet = item.optString("description")
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying Brave Search API", e)
        }
        return results
    }

    private fun stripHtml(html: String): String {
        return html.replace(Regex("<[^>]*>"), "")
    }
}
