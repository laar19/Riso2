package com.example.service.search

import android.util.Log
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class SearchResult(
    val title: String,
    val url: String,
    val snippet: String
)

data class ScrapedPage(
    val url: String,
    val title: String,
    val text: String,
    val success: Boolean,
    val error: String? = null
)

class WebSearchService {
    private val TAG = "WebSearchService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // 1. DuckDuckGo Scraper (100% FREE, No API key required)
    fun searchDuckDuckGo(query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        if (query.isBlank()) return results

        // Strategy A: DuckDuckGo Lite (Fast, clean HTML tables, no JS required)
        try {
            val formBody = FormBody.Builder()
                .add("q", query)
                .build()

            val request = Request.Builder()
                .url("https://lite.duckduckgo.com/lite/")
                .post(formBody)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val html = response.body?.string() ?: ""
                    parseDuckDuckGoLite(html, results)
                } else {
                    Log.w(TAG, "DDG Lite returned code ${response.code}, trying fallback")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "DDG Lite error: ${e.message}, attempting fallback")
        }

        // If Strategy A got results, return them
        if (results.isNotEmpty()) {
            return results.take(5)
        }

        // Strategy B: DuckDuckGo HTML standard endpoint fallback
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://html.duckduckgo.com/html/?q=$encodedQuery"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val html = response.body?.string() ?: ""
                    parseDuckDuckGoHtml(html, results)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "DDG HTML fallback error", e)
        }

        return results.take(5)
    }

    private fun parseDuckDuckGoLite(html: String, results: MutableList<SearchResult>) {
        // DDG Lite format:
        // Results are in table rows:
        // <a class="result-link" href="...">Title</a>
        // <td class="result-snippet">Snippet</td>
        val linkPattern = Pattern.compile("<a[^>]*class=[\"']result-link[\"'][^>]*href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>", Pattern.DOTALL or Pattern.CASE_INSENSITIVE)
        val snippetPattern = Pattern.compile("<td[^>]*class=[\"']result-snippet[\"'][^>]*>(.*?)</td>", Pattern.DOTALL or Pattern.CASE_INSENSITIVE)

        val linkMatcher = linkPattern.matcher(html)
        val snippetMatcher = snippetPattern.matcher(html)

        while (linkMatcher.find() && results.size < 5) {
            val rawUrl = linkMatcher.group(1) ?: ""
            val rawTitle = linkMatcher.group(2) ?: ""
            
            val actualUrl = extractActualUrl(rawUrl)
            val title = cleanHtmlText(rawTitle)

            var snippet = ""
            if (snippetMatcher.find()) {
                snippet = cleanHtmlText(snippetMatcher.group(1) ?: "")
            }

            if (title.isNotBlank() && actualUrl.isNotBlank() && !actualUrl.contains("duckduckgo.com")) {
                results.add(
                    SearchResult(
                        title = title,
                        url = actualUrl,
                        snippet = if (snippet.isNotBlank()) snippet else "Ver detalle en $actualUrl"
                    )
                )
            }
        }
    }

    private fun parseDuckDuckGoHtml(html: String, results: MutableList<SearchResult>) {
        val resultBlocks = html.split("<div class=\"result results_links results_links_deep web-result")
        for (i in 1 until resultBlocks.size) {
            if (results.size >= 5) break
            val block = resultBlocks[i]

            val urlMatcher = Pattern.compile("href=\"([^\"]+)\"").matcher(block)
            var rawUrl = ""
            if (urlMatcher.find()) {
                rawUrl = urlMatcher.group(1) ?: ""
            }
            val actualUrl = extractActualUrl(rawUrl)

            val titleMatcher = Pattern.compile("<a class=\"result__a\"[^>]*>(.*?)</a>", Pattern.DOTALL).matcher(block)
            var title = ""
            if (titleMatcher.find()) {
                title = cleanHtmlText(titleMatcher.group(1) ?: "")
            }

            val snippetMatcher = Pattern.compile("<a class=\"result__snippet\"[^>]*>(.*?)</a>", Pattern.DOTALL).matcher(block)
            var snippet = ""
            if (snippetMatcher.find()) {
                snippet = cleanHtmlText(snippetMatcher.group(1) ?: "")
            } else {
                val spanMatcher = Pattern.compile("<span class=\"result__snippet\"[^>]*>(.*?)</span>", Pattern.DOTALL).matcher(block)
                if (spanMatcher.find()) {
                    snippet = cleanHtmlText(spanMatcher.group(1) ?: "")
                }
            }

            if (title.isNotBlank() && actualUrl.isNotBlank()) {
                results.add(
                    SearchResult(
                        title = title,
                        url = actualUrl,
                        snippet = if (snippet.isNotBlank()) snippet else "Ver detalle en $actualUrl"
                    )
                )
            }
        }
    }

    private fun extractActualUrl(rawUrl: String): String {
        return try {
            if (rawUrl.contains("uddg=")) {
                val encoded = rawUrl.substringAfter("uddg=").substringBefore("&")
                URLDecoder.decode(encoded, "UTF-8")
            } else if (rawUrl.startsWith("//")) {
                "https:$rawUrl"
            } else {
                rawUrl
            }
        } catch (e: Exception) {
            rawUrl
        }
    }

    // 2. Brave Search API (Requires Brave Search Subscription API Key)
    fun searchBrave(query: String, apiKey: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        if (apiKey.isBlank()) return results

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

    // 3. Web Page Scraper / URL Reader (Scrapes text from any website or article)
    fun scrapeWebPage(url: String): ScrapedPage {
        try {
            var targetUrl = url.trim()
            if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
                targetUrl = "https://$targetUrl"
            }

            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return ScrapedPage(
                        url = targetUrl,
                        title = "",
                        text = "",
                        success = false,
                        error = "HTTP ${response.code}: No se pudo acceder a la página."
                    )
                }

                val html = response.body?.string() ?: ""
                
                // Extract Title
                val titleMatcher = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(html)
                val title = if (titleMatcher.find()) cleanHtmlText(titleMatcher.group(1) ?: "") else ""

                // Clean body
                var cleaned = html
                // Remove scripts, styles, svg, header, footer, nav
                cleaned = cleaned.replace(Regex("(?is)<script[^>]*>.*?</script>"), " ")
                cleaned = cleaned.replace(Regex("(?is)<style[^>]*>.*?</style>"), " ")
                cleaned = cleaned.replace(Regex("(?is)<noscript[^>]*>.*?</noscript>"), " ")
                cleaned = cleaned.replace(Regex("(?is)<svg[^>]*>.*?</svg>"), " ")
                cleaned = cleaned.replace(Regex("(?is)<header[^>]*>.*?</header>"), " ")
                cleaned = cleaned.replace(Regex("(?is)<footer[^>]*>.*?</footer>"), " ")
                cleaned = cleaned.replace(Regex("(?is)<nav[^>]*>.*?</nav>"), " ")

                // Extract paragraphs or main textual content
                val paragraphs = mutableListOf<String>()
                val pMatcher = Pattern.compile("(?is)<p[^>]*>(.*?)</p>").matcher(cleaned)
                while (pMatcher.find()) {
                    val pText = cleanHtmlText(pMatcher.group(1) ?: "")
                    if (pText.length > 25) {
                        paragraphs.add(pText)
                    }
                }

                val extractedText = if (paragraphs.isNotEmpty()) {
                    paragraphs.joinToString("\n\n")
                } else {
                    cleanHtmlText(cleaned)
                }

                // Limit length to avoid exceeding context window (max 4500 chars)
                val truncatedText = if (extractedText.length > 4500) {
                    extractedText.take(4500) + "\n\n... [Texto truncado para optimizar lectura]"
                } else {
                    extractedText
                }

                return ScrapedPage(
                    url = targetUrl,
                    title = title,
                    text = truncatedText,
                    success = true
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scraping page $url", e)
            return ScrapedPage(
                url = url,
                title = "",
                text = "",
                success = false,
                error = e.localizedMessage ?: e.message ?: "Error al raspar página"
            )
        }
    }

    private fun cleanHtmlText(html: String): String {
        return html
            .replace(Regex("<[^>]*>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

