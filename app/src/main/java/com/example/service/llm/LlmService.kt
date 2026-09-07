package com.example.service.llm

import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// --- Moshi Data Classes for Gemini REST API ---

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val tools: List<GeminiTool>? = null,
    val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null,
    val functionCall: GeminiFunctionCall? = null,
    val functionResponse: GeminiFunctionResponse? = null
)

@JsonClass(generateAdapter = true)
data class GeminiFunctionCall(
    val name: String,
    val args: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiFunctionResponse(
    val name: String,
    val response: Map<String, Any?>
)

@JsonClass(generateAdapter = true)
data class GeminiTool(
    val functionDeclarations: List<GeminiFunctionDecl>? = null,
    val googleSearchRetrieval: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiFunctionDecl(
    val name: String,
    val description: String,
    val parameters: GeminiParameters? = null
)

@JsonClass(generateAdapter = true)
data class GeminiParameters(
    val type: String = "OBJECT",
    val properties: Map<String, GeminiProperty>,
    val required: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GeminiProperty(
    val type: String, // "STRING", "INTEGER", "BOOLEAN"
    val description: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent,
    val finishReason: String? = null
)

interface GeminiApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") key: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

class LlmService {

    private val TAG = "LlmService"
    private val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api = retrofit.create(GeminiApi::class.java)

    // Setup the 10 email automation tools declarations
    private val emailTools = GeminiTool(
        functionDeclarations = listOf(
            GeminiFunctionDecl(
                name = "list_inbox",
                description = "Lists recent emails from inbox.",
                parameters = GeminiParameters(
                    properties = mapOf(
                        "limit" to GeminiProperty("INTEGER", "Maximum number of recent emails to list. Default is 5.")
                    )
                )
            ),
            GeminiFunctionDecl(
                name = "search_emails",
                description = "Searches for emails containing a text query.",
                parameters = GeminiParameters(
                    properties = mapOf(
                        "query" to GeminiProperty("STRING", "Search terms or query string, e.g. 'reunión' or 'Ana'"),
                        "limit" to GeminiProperty("INTEGER", "Maximum results to return. Default is 5.")
                    ),
                    required = listOf("query")
                )
            ),
            GeminiFunctionDecl(
                name = "read_email",
                description = "Reads full email content by ID.",
                parameters = GeminiParameters(
                    properties = mapOf(
                        "id" to GeminiProperty("STRING", "Message ID to fetch, e.g. 'msg_101'")
                    ),
                    required = listOf("id")
                )
            ),
            GeminiFunctionDecl(
                name = "send_email",
                description = "Sends a new email (Writing operation).",
                parameters = GeminiParameters(
                    properties = mapOf(
                        "to" to GeminiProperty("STRING", "Recipient email address"),
                        "subject" to GeminiProperty("STRING", "Subect line of the email"),
                        "body" to GeminiProperty("STRING", "Body content of the email")
                    ),
                    required = listOf("to", "subject", "body")
                )
            ),
            GeminiFunctionDecl(
                name = "reply_to_email",
                description = "Replies to an email (Writing operation).",
                parameters = GeminiParameters(
                    properties = mapOf(
                        "messageId" to GeminiProperty("STRING", "The original email id, e.g. 'msg_101'"),
                        "body" to GeminiProperty("STRING", "Body content of the reply")
                    ),
                    required = listOf("messageId", "body")
                )
            ),
            GeminiFunctionDecl(
                name = "forward_email",
                description = "Forwards an email (Writing operation).",
                parameters = GeminiParameters(
                    properties = mapOf(
                        "messageId" to GeminiProperty("STRING", "The original email id to forward, e.g. 'msg_101'"),
                        "to" to GeminiProperty("STRING", "Email address of the recipient"),
                        "body" to GeminiProperty("STRING", "Optional intro message to prepend")
                    ),
                    required = listOf("messageId", "to")
                )
            ),
            GeminiFunctionDecl(
                name = "mark_as_read",
                description = "Marks email as read (Writing operation).",
                parameters = GeminiParameters(
                    properties = mapOf(
                        "id" to GeminiProperty("STRING", "Message ID to mark as read, e.g. 'msg_101'")
                    ),
                    required = listOf("id")
                )
            ),
            GeminiFunctionDecl(
                name = "mark_as_unread",
                description = "Marks email as unread (Writing operation).",
                parameters = GeminiParameters(
                    properties = mapOf(
                        "id" to GeminiProperty("STRING", "Message ID to mark as unread, e.g. 'msg_101'")
                    ),
                    required = listOf("id")
                )
            ),
            GeminiFunctionDecl(
                name = "archive_email",
                description = "Archives an email (Writing operation).",
                parameters = GeminiParameters(
                    properties = mapOf(
                        "id" to GeminiProperty("STRING", "Message ID to archive, e.g. 'msg_101'")
                    ),
                    required = listOf("id")
                )
            ),
            GeminiFunctionDecl(
                name = "delete_email",
                description = "Deletes an email by moving to trash (Writing operation).",
                parameters = GeminiParameters(
                    properties = mapOf(
                        "id" to GeminiProperty("STRING", "Message ID to delete, e.g. 'msg_101'")
                    ),
                    required = listOf("id")
                )
            )
        )
    )

    private val githubTools = GeminiTool(
        functionDeclarations = listOf(
            GeminiFunctionDecl(
                name = "list_github_repositories",
                description = "Lists repositories for the authenticated user or specified username.",
                parameters = GeminiParameters(
                    properties = mapOf(
                        "username" to GeminiProperty("STRING", "Optional GitHub username. If not supplied, lists authenticated user's repos.")
                    )
                )
            ),
            GeminiFunctionDecl(
                name = "list_github_issues",
                description = "Lists issues for a specified GitHub owner and repository.",
                parameters = GeminiParameters(
                    properties = mapOf(
                        "owner" to GeminiProperty("STRING", "Repository owner username or organization Name"),
                        "repo" to GeminiProperty("STRING", "Repository name"),
                        "state" to GeminiProperty("STRING", "Filter by state: 'open', 'closed', or 'all'. Default is 'open'.")
                    ),
                    required = listOf("owner", "repo")
                )
            ),
            GeminiFunctionDecl(
                name = "create_github_issue",
                description = "Creates a new issue in a GitHub repository.",
                parameters = GeminiParameters(
                    properties = mapOf(
                        "owner" to GeminiProperty("STRING", "Repository owner Name"),
                        "repo" to GeminiProperty("STRING", "Repository Name"),
                        "title" to GeminiProperty("STRING", "Title of the new issue"),
                        "body" to GeminiProperty("STRING", "Full markdown body of the issue")
                    ),
                    required = listOf("owner", "repo", "title", "body")
                )
            )
        )
    )

    private val gitlabTools = GeminiTool(
        functionDeclarations = listOf(
            GeminiFunctionDecl(
                name = "list_gitlab_projects",
                description = "Lists owner projects from connected GitLab instance.",
                parameters = GeminiParameters(
                    properties = mapOf(
                        "membership" to GeminiProperty("STRING", "Filter projects by membership: 'true' or 'false'. Default 'true'.")
                    )
                )
            ),
            GeminiFunctionDecl(
                name = "create_gitlab_issue",
                description = "Creates an issue in a specified GitLab project.",
                parameters = GeminiParameters(
                    properties = mapOf(
                        "projectId" to GeminiProperty("STRING", "Project ID or path-encoded namespace/project-name"),
                        "title" to GeminiProperty("STRING", "Title of the issue"),
                        "description" to GeminiProperty("STRING", "Description details")
                    ),
                    required = listOf("projectId", "title", "description")
                )
            )
        )
    )

    private val webSearchTool = GeminiTool(
        functionDeclarations = listOf(
            GeminiFunctionDecl(
                name = "web_search",
                description = "Busca información en tiempo real en internet sobre noticias, eventos recientes, precios, datos o consultas generales.",
                parameters = GeminiParameters(
                    properties = mapOf(
                        "query" to GeminiProperty("STRING", "Términos o palabras clave de búsqueda en internet")
                    ),
                    required = listOf("query")
                )
            ),
            GeminiFunctionDecl(
                name = "scrape_web_page",
                description = "Entra y extrae el texto legible de una página web o artículo a partir de su URL para leer su contenido completo.",
                parameters = GeminiParameters(
                    properties = mapOf(
                        "url" to GeminiProperty("STRING", "La URL completa (iniciando con https:// o http://) de la página web a raspar y leer")
                    ),
                    required = listOf("url")
                )
            )
        )
    )

    private fun isKeyInvalidOrPlaceholder(key: String?): Boolean {
        if (key.isNullOrBlank()) return true
        val trimmed = key.trim()
        return trimmed.equals("MY_GEMINI_API_KEY", ignoreCase = true) ||
                trimmed.equals("YOUR_API_KEY", ignoreCase = true) ||
                trimmed.contains("PLACEHOLDER", ignoreCase = true)
    }

    // Execute completion against Gemini (supports fallback if customized key is empty)
    suspend fun resolveLlm(
        history: List<GeminiContent>,
        provider: String, // Gemini, OpenAI, Claude, etc.
        customApiKey: String? = null,
        apiEndpoint: String? = null,
        modelName: String? = null,
        mcpEmailEnabled: Boolean = true,
        mcpGithubEnabled: Boolean = false,
        mcpGitlabEnabled: Boolean = false,
        githubUsername: String = "",
        gitlabUrl: String = "",
        internetSearchEnabled: Boolean = false,
        searchProvider: String = "google_grounding"
    ): GeminiResponse {
        val resolvedKey = if (!customApiKey.isNullOrBlank()) {
            customApiKey
        } else {
            BuildConfig.GEMINI_API_KEY
        }

        val isGemini = provider.lowercase().contains("gemini")
        if (isGemini && isKeyInvalidOrPlaceholder(resolvedKey)) {
            Log.w(TAG, "Gemini API Key is not configured or is placeholder. Providing offline guidance.")
            val lastUserMessage = history.lastOrNull { it.role == "user" }?.parts?.firstOrNull()?.text ?: ""
            return getErrorResponse(getOfflineAssistantResponse(lastUserMessage))
        }

        try {
            val enabledMcps = mutableListOf<String>()
            if (mcpEmailEnabled) enabledMcps.add("Email")
            if (mcpGithubEnabled) enabledMcps.add("GitHub (User: $githubUsername)")
            if (mcpGitlabEnabled) enabledMcps.add("GitLab (URL: $gitlabUrl)")
            val mcpListStr = if (enabledMcps.isEmpty()) "Ninguno (recomienda al usuario activar conexiones desde el botón '+' en la caja de chat)" else enabledMcps.joinToString(", ")

            val searchInfoStr = if (internetSearchEnabled) "Habilitada (Buscador / Scraper: $searchProvider)" else "Desactivada"

            val systemPrompt = """
                Eres Riso, un asistente de automatización y chat de IA local para Android con soporte MCP (Model Context Protocol) y capacidades de navegación web y raspado.
                Tu función principal es ayudar al usuario a automatizar tareas y responder consultas conectándote a sus servicios locales y remotos.
                
                **Conexiones MCP actuales activas:** $mcpListStr
                **Búsqueda en Internet & Web Scraping:** $searchInfoStr
                
                Tienes acceso a las herramientas correspondientes según los servicios habilitados (list_inbox, search_emails, read_email, send_email, reply_to_email, forward_email, mark_as_read, mark_as_unread, archive_email, delete_email, list_github_repositories, list_github_issues, create_github_issue, list_gitlab_projects, create_gitlab_issue).
                Si te piden una tarea asociada a un servicio habilitado, invoca la herramienta correspondiente de inmediato.
                Si el servicio requerido no está activo, explícaselo al usuario de forma muy amigable y recuérdale que puede activarlo usando el botón '+' en la caja de chat.
                
                ${if (internetSearchEnabled) """
                - Para buscar información en tiempo real, eventos recientes, precios, novedades o datos que desconozcas, invoca la herramienta `web_search(query)`.
                - Para leer o raspar el contenido de una URL o artículo web (incluyendo enlaces que el usuario te comparta o enlaces obtenidos de una búsqueda), invoca la herramienta `scrape_web_page(url)`.
                """.trimIndent() else ""}
                
                SIEMPRE responde en español. Sé sumamente amable, conciso, inteligente y profesional.
            """.trimIndent()

            val activeToolsList = mutableListOf<GeminiTool>()
            if (mcpEmailEnabled) activeToolsList.add(emailTools)
            if (mcpGithubEnabled) activeToolsList.add(githubTools)
            if (mcpGitlabEnabled) activeToolsList.add(gitlabTools)
            if (internetSearchEnabled) activeToolsList.add(webSearchTool)

            val toolsPayload = if (activeToolsList.isNotEmpty()) activeToolsList else null

            val isGemini = provider.equals("Gemini", ignoreCase = true) || provider.contains("google", ignoreCase = true)
            val isClaude = provider.equals("Claude", ignoreCase = true) || provider.contains("anthropic", ignoreCase = true)

            if (isGemini) {
                val resolvedKey = if (!customApiKey.isNullOrBlank()) customApiKey else BuildConfig.GEMINI_API_KEY
                if (isKeyInvalidOrPlaceholder(resolvedKey)) {
                    Log.w(TAG, "Gemini API Key is not configured or is placeholder.")
                    val lastUserMessage = history.lastOrNull { it.role == "user" }?.parts?.firstOrNull()?.text ?: ""
                    return getErrorResponse(getOfflineAssistantResponse(lastUserMessage))
                }

                val request = GeminiRequest(
                    contents = history,
                    systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt))),
                    tools = toolsPayload,
                    generationConfig = GeminiGenerationConfig(temperature = 0.4f)
                )

                val modelToUse = if (!modelName.isNullOrBlank()) modelName else "gemini-3.5-flash"
                return api.generateContent(model = modelToUse, key = resolvedKey, request = request)
            } else if (isClaude) {
                if (customApiKey.isNullOrBlank()) {
                    return getErrorResponse("⚠️ **Clave de API para Claude no configurada**\n\nPor favor, ingresa tu clave de Anthropic Claude en la pestaña Ajustes.")
                }
                return callAnthropicClaude(
                    apiKey = customApiKey,
                    modelName = modelName ?: "claude-3-5-sonnet-20241022",
                    systemPrompt = systemPrompt,
                    history = history
                )
            } else {
                // OpenAI or OpenAI-Compatible (OpenCode, DeepSeek, Ollama, Groq, etc.)
                if (customApiKey.isNullOrBlank()) {
                    return getErrorResponse("⚠️ **Clave de API para $provider no configurada**\n\nPor favor, ingresa tu clave de API en Ajustes para el modelo **${modelName ?: "seleccionado"}**.")
                }
                return callOpenAiCompatible(
                    endpointUrl = apiEndpoint ?: "",
                    apiKey = customApiKey,
                    modelName = modelName ?: "",
                    systemPrompt = systemPrompt,
                    history = history,
                    activeToolsList = activeToolsList
                )
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Notice in resolveLlm: ${e.message}")
            return getErrorResponse("⚠️ **Error de Conexión o Servicio**\n\nNo se pudo obtener respuesta del resolvedor de IA. Detalles: ${e.localizedMessage ?: e.message ?: "Error desconocido de red"}\n\nPor favor, verifica tu conexión a internet o comprueba si tu clave de API configurada es correcta.")
        }
    }

    private suspend fun callOpenAiCompatible(
        endpointUrl: String,
        apiKey: String,
        modelName: String,
        systemPrompt: String,
        history: List<GeminiContent>,
        activeToolsList: List<GeminiTool>
    ): GeminiResponse = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val base = if (endpointUrl.isNotBlank()) endpointUrl.trimEnd('/') else "https://api.openai.com/v1"
        val fullUrl = if (base.endsWith("/chat/completions")) base else "$base/chat/completions"

        val messagesArray = JSONArray()
        // System instruction
        val sysObj = JSONObject()
        sysObj.put("role", "system")
        sysObj.put("content", systemPrompt)
        messagesArray.put(sysObj)

        // Conversation history
        for (item in history) {
            val role = if (item.role == "model") "assistant" else "user"
            val textPart = item.parts.firstOrNull { !it.text.isNullOrBlank() }?.text
            if (!textPart.isNullOrBlank()) {
                val msgObj = JSONObject()
                msgObj.put("role", role)
                msgObj.put("content", textPart)
                messagesArray.put(msgObj)
            }
        }

        val jsonBody = JSONObject()
        jsonBody.put("model", if (modelName.isNotBlank()) modelName else "deepseek-v4-flash")
        jsonBody.put("messages", messagesArray)
        jsonBody.put("temperature", 0.4)

        // Optional tools
        val toolsArray = JSONArray()
        for (tool in activeToolsList) {
            tool.functionDeclarations?.forEach { decl ->
                val fnObj = JSONObject()
                fnObj.put("name", decl.name)
                fnObj.put("description", decl.description)
                val paramsObj = JSONObject()
                paramsObj.put("type", "object")
                val propsObj = JSONObject()
                decl.parameters?.properties?.forEach { (k, v) ->
                    val p = JSONObject()
                    p.put("type", v.type.lowercase())
                    p.put("description", v.description)
                    propsObj.put(k, p)
                }
                paramsObj.put("properties", propsObj)
                val reqArr = JSONArray()
                decl.parameters?.required?.forEach { reqArr.put(it) }
                paramsObj.put("required", reqArr)
                fnObj.put("parameters", paramsObj)

                val toolObj = JSONObject()
                toolObj.put("type", "function")
                toolObj.put("function", fnObj)
                toolsArray.put(toolObj)
            }
        }
        if (toolsArray.length() > 0) {
            jsonBody.put("tools", toolsArray)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        var requestBody = jsonBody.toString().toRequestBody(mediaType)

        var request = Request.Builder()
            .url(fullUrl)
            .addHeader("Authorization", "Bearer ${apiKey.trim()}")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        var response = okHttpClient.newCall(request).execute()
        var respString = response.body?.string() ?: ""

        // If tools failed with 400 (some endpoints do not support the 'tools' parameter), retry without tools!
        if (response.code == 400 && toolsArray.length() > 0) {
            response.close()
            jsonBody.remove("tools")
            requestBody = jsonBody.toString().toRequestBody(mediaType)
            request = Request.Builder()
                .url(fullUrl)
                .addHeader("Authorization", "Bearer ${apiKey.trim()}")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            response = okHttpClient.newCall(request).execute()
            respString = response.body?.string() ?: ""
        }

        if (!response.isSuccessful) {
            var errMsg = "Error HTTP ${response.code}"
            try {
                val errJson = JSONObject(respString)
                if (errJson.has("error")) {
                    val errObj = errJson.optJSONObject("error")
                    if (errObj != null && errObj.has("message")) {
                        errMsg = errObj.getString("message")
                    } else if (errJson.optString("error").isNotBlank()) {
                        errMsg = errJson.getString("error")
                    }
                } else if (errJson.has("message")) {
                    errMsg = errJson.getString("message")
                }
            } catch (_: Exception) {
                if (respString.isNotBlank()) {
                    errMsg = respString.take(250)
                }
            }
            return@withContext getErrorResponse(
                "⚠️ **Error en Proveedor LLM (HTTP ${response.code})**\n\n$errMsg\n\n**Endpoint:** $fullUrl\n**Modelo:** ${if (modelName.isNotBlank()) modelName else "default"}\n\nVerifica tu clave de API y el endpoint configurado."
            )
        }

        try {
            val root = JSONObject(respString)
            val choices = root.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                return@withContext getErrorResponse("Respuesta vacía del modelo (sin choices devueltas).")
            }
            val firstChoice = choices.getJSONObject(0)
            val message = firstChoice.getJSONObject("message")
            val content = message.optString("content", "")
            val toolCalls = message.optJSONArray("tool_calls")

            val parts = mutableListOf<GeminiPart>()
            if (toolCalls != null && toolCalls.length() > 0) {
                for (i in 0 until toolCalls.length()) {
                    val tc = toolCalls.getJSONObject(i)
                    val fn = tc.getJSONObject("function")
                    val fnName = fn.getString("name")
                    val argsStr = fn.optString("arguments", "{}")
                    val argsMap = mutableMapOf<String, Any?>()
                    try {
                        val argsJson = JSONObject(argsStr)
                        for (k in argsJson.keys()) {
                            argsMap[k] = argsJson.get(k)
                        }
                    } catch (_: Exception) {}
                    parts.add(GeminiPart(functionCall = GeminiFunctionCall(name = fnName, args = argsMap)))
                }
            }

            if (content.isNotBlank() || parts.isEmpty()) {
                parts.add(0, GeminiPart(text = content))
            }

            return@withContext GeminiResponse(
                candidates = listOf(
                    GeminiCandidate(
                        content = GeminiContent(role = "model", parts = parts),
                        finishReason = firstChoice.optString("finish_reason", "STOP")
                    )
                )
            )
        } catch (e: Exception) {
            return@withContext getErrorResponse("Error al procesar respuesta del modelo: ${e.localizedMessage}")
        }
    }

    private suspend fun callAnthropicClaude(
        apiKey: String,
        modelName: String,
        systemPrompt: String,
        history: List<GeminiContent>
    ): GeminiResponse = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val fullUrl = "https://api.anthropic.com/v1/messages"
        val messagesArray = JSONArray()
        for (item in history) {
            val role = if (item.role == "model") "assistant" else "user"
            val textPart = item.parts.firstOrNull { !it.text.isNullOrBlank() }?.text
            if (!textPart.isNullOrBlank()) {
                val msgObj = JSONObject()
                msgObj.put("role", role)
                msgObj.put("content", textPart)
                messagesArray.put(msgObj)
            }
        }
        if (messagesArray.length() == 0) {
            val dummy = JSONObject()
            dummy.put("role", "user")
            dummy.put("content", "Hola")
            messagesArray.put(dummy)
        }

        val jsonBody = JSONObject()
        jsonBody.put("model", if (modelName.isNotBlank()) modelName else "claude-3-5-sonnet-20241022")
        jsonBody.put("max_tokens", 4096)
        jsonBody.put("system", systemPrompt)
        jsonBody.put("messages", messagesArray)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(fullUrl)
            .addHeader("x-api-key", apiKey.trim())
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        val respString = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            return@withContext getErrorResponse("⚠️ **Error Anthropic Claude (HTTP ${response.code})**\n\n$respString")
        }

        try {
            val root = JSONObject(respString)
            val contentArr = root.optJSONArray("content")
            val text = if (contentArr != null && contentArr.length() > 0) {
                contentArr.getJSONObject(0).optString("text", "")
            } else ""

            return@withContext GeminiResponse(
                candidates = listOf(
                    GeminiCandidate(
                        content = GeminiContent(role = "model", parts = listOf(GeminiPart(text = text))),
                        finishReason = "STOP"
                    )
                )
            )
        } catch (e: Exception) {
            return@withContext getErrorResponse("Error al procesar respuesta de Claude: ${e.localizedMessage}")
        }
    }

    private fun getOfflineAssistantResponse(userMessage: String): String {
        val lower = userMessage.lowercase()
        val isGreeting = lower.contains("hola") || lower.contains("buenos") || lower.contains("buenas") || lower.contains("saludos") || lower.contains("quien eres") || lower.contains("qué eres")
        val isEmail = lower.contains("correo") || lower.contains("email") || lower.contains("bandeja") || lower.contains("inbox") || lower.contains("mensaje")
        val isSettings = lower.contains("clave") || lower.contains("api") || lower.contains("key") || lower.contains("ajustes") || lower.contains("configurar")

        return buildString {
            if (isGreeting) {
                append("¡Hola! Soy **Riso**, tu asistente de automatización y correos para Android.\n\n")
            } else if (isEmail) {
                append("Puedo ayudarte a revisar, redactar y organizar tus correos con soporte MCP. Puedes ver tu bandeja de entrada en la pestaña **Bandeja**.\n\n")
            } else if (isSettings) {
                append("Puedes configurar tus claves de API y modelos en la pestaña **Ajustes**.\n\n")
            } else {
                append("He recibido tu mensaje: \"$userMessage\".\n\nActualmente estoy operando en **Modo Asistente Local**.\n\n")
            }
            append("💡 **Para activar respuestas generativas completas con Gemini:**\n")
            append("1. Abre la pestaña **Ajustes** en el menú inferior.\n")
            append("2. En **Modelos LLM (APIs)**, ingresa tu clave API de Google Gemini.\n")
            append("3. También puedes ingresar tu clave en el panel de Secretos de AI Studio.")
        }
    }

    private fun getErrorResponse(msg: String): GeminiResponse {
        return GeminiResponse(
            candidates = listOf(
                GeminiCandidate(
                    content = GeminiContent(
                        role = "model",
                        parts = listOf(GeminiPart(text = msg))
                    )
                )
            )
        )
    }
}
