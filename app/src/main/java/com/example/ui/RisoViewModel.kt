package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.RisoDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.PendingAction
import com.example.data.repository.RisoRepository
import com.example.service.email.EmailService
import com.example.service.email.RisoEmail
import com.example.service.email.EmailAccount
import com.example.service.llm.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.*

class RisoViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "RisoViewModel"

    private val database = RisoDatabase.getDatabase(application)
    private val repository = RisoRepository(database.risoDao())
    private val emailService = EmailService(repository)
    private val llmService = LlmService()

    // Observable States
    val sessions: StateFlow<List<ChatSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSessionId = MutableStateFlow<String?>(null)
    val selectedSessionId: StateFlow<String?> = _selectedSessionId.asStateFlow()

    val currentMessages: StateFlow<List<ChatMessage>> = _selectedSessionId
        .flatMapLatest { sessionId ->
            if (sessionId != null) {
                repository.getMessagesForSession(sessionId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<Map<String, String>> = repository.allSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val pendingActions: StateFlow<List<PendingAction>> = repository.allPendingActions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI-only states
    private val _isLlmLoading = MutableStateFlow(false)
    val isLlmLoading: StateFlow<Boolean> = _isLlmLoading.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    private val _connectionTestResult = MutableStateFlow<String?>(null)
    val connectionTestResult: StateFlow<String?> = _connectionTestResult.asStateFlow()

    private val _liveInbox = MutableStateFlow<List<RisoEmail>>(emptyList())
    val liveInbox: StateFlow<List<RisoEmail>> = _liveInbox.asStateFlow()

    private val _isInboxLoading = MutableStateFlow(false)
    val isInboxLoading: StateFlow<Boolean> = _isInboxLoading.asStateFlow()

    private val _planningMode = MutableStateFlow(true) // Default is Planning Mode (🛡️)
    val planningMode: StateFlow<Boolean> = _planningMode.asStateFlow()

    // Multiple Email Accounts States
    val emailAccounts = MutableStateFlow<List<EmailAccount>>(emptyList())
    val activeEmailAccountId = MutableStateFlow<String?>(null)

    // Multiple LLM Profiles / API keys cada uno
    val llmProfiles = MutableStateFlow<List<LlmProfile>>(emptyList())
    val activeLlmProfileId = MutableStateFlow<String?>(null)

    // STT & Whisper Settings
    private val _sttProvider = MutableStateFlow("Whisper API") // "Whisper API" | "Whisper Local small-v3"
    val sttProvider: StateFlow<String> = _sttProvider.asStateFlow()

    private val _localWhisperStatus = MutableStateFlow("Not Downloaded") // "Not Downloaded" | "Downloading" | "Ready"
    val localWhisperStatus: StateFlow<String> = _localWhisperStatus.asStateFlow()

    private val _localWhisperProgress = MutableStateFlow(0f)
    val localWhisperProgress: StateFlow<Float> = _localWhisperProgress.asStateFlow()

    // Microphone audio/transcribing state
    private val _isRecordingAudio = MutableStateFlow(false)
    val isRecordingAudio: StateFlow<Boolean> = _isRecordingAudio.asStateFlow()

    private val _recordingFeedback = MutableStateFlow("")
    val recordingFeedback: StateFlow<String> = _recordingFeedback.asStateFlow()

    // Image Upload attachment state
    private val _attachedImage = MutableStateFlow<String?>(null) // e.g. "Recibo" | "Menu" | "Grafico"
    val attachedImage: StateFlow<String?> = _attachedImage.asStateFlow()

    init {
        // Load default planning mode from database
        viewModelScope.launch {
            val savedMode = repository.getSetting("planning_mode")
            _planningMode.value = savedMode != "false"

            // Load Multi-Account setup
            val accountsJson = repository.getSetting("email_accounts_json") ?: ""
            val activeId = repository.getSetting("active_email_account_id") ?: ""
            if (accountsJson.isNotBlank()) {
                val parsed = parseEmailAccounts(accountsJson)
                emailAccounts.value = parsed
                if (parsed.any { it.id == activeId }) {
                    activeEmailAccountId.value = activeId
                } else if (parsed.isNotEmpty()) {
                    activeEmailAccountId.value = parsed.first().id
                    repository.saveSetting("active_email_account_id", parsed.first().id)
                }
            } else {
                // Seed a default account
                val legacyEmail = repository.getSetting("email_address") ?: "usuario@riso.local"
                val defaultAcc = EmailAccount(
                    id = "default_acc",
                    emailAddress = legacyEmail,
                    imapServer = repository.getSetting("imap_server") ?: "",
                    imapPort = "993",
                    smtpServer = repository.getSetting("smtp_server") ?: "",
                    smtpPort = "587",
                    passwordVal = repository.getSetting("email_password") ?: ""
                )
                val newList = listOf(defaultAcc)
                emailAccounts.value = newList
                activeEmailAccountId.value = "default_acc"
                saveEmailAccountsToDb(newList)
                repository.saveSetting("active_email_account_id", "default_acc")
            }

            // Load Multi-Model setup
            val profilesJson = repository.getSetting("llm_profiles_json") ?: ""
            val activeProfId = repository.getSetting("active_llm_profile_id") ?: ""
            if (profilesJson.isNotBlank()) {
                val parsed = parseLlmProfiles(profilesJson)
                llmProfiles.value = parsed
                if (parsed.any { it.id == activeProfId }) {
                    activeLlmProfileId.value = activeProfId
                } else if (parsed.isNotEmpty()) {
                    activeLlmProfileId.value = parsed.first().id
                    repository.saveSetting("active_llm_profile_id", parsed.first().id)
                }
            } else {
                // Seed standard/existing ones
                val initList = mutableListOf<LlmProfile>()
                val geminiApiKey = repository.getSetting("gemini_api_key") ?: ""
                initList.add(LlmProfile("init_gemini", "Gemini Oficial", "Gemini", geminiApiKey))
                
                val openaiApiKey = repository.getSetting("openai_api_key") ?: ""
                if (openaiApiKey.isNotBlank()) {
                    initList.add(LlmProfile("init_openai", "OpenAI Standard", "OpenAI", openaiApiKey))
                }
                
                val claudeApiKey = repository.getSetting("claude_api_key") ?: ""
                if (claudeApiKey.isNotBlank()) {
                    initList.add(LlmProfile("init_claude", "Claude Anthropic", "Claude", claudeApiKey))
                }
                
                llmProfiles.value = initList
                if (initList.isNotEmpty()) {
                    activeLlmProfileId.value = initList.first().id
                    repository.saveSetting("active_llm_profile_id", initList.first().id)
                    saveLlmProfilesToDb(initList)
                }
            }

            // STT settings
            val savedStt = repository.getSetting("selected_stt_provider") ?: "Whisper API"
            _sttProvider.value = savedStt

            val whisperStatus = repository.getSetting("whisper_local_status") ?: "Not Downloaded"
            _localWhisperStatus.value = whisperStatus
            if (whisperStatus == "Ready") {
                _localWhisperProgress.value = 1.0f
            }
            
            // Auto create session if empty
            sessions.first { true } // Suspend until first load
            if (sessions.value.isEmpty()) {
                createNewSession()
            } else {
                _selectedSessionId.value = sessions.value.first().id
            }
            
            // Auto refresh mail list
            refreshLiveInbox()
        }
    }

    private fun parseEmailAccounts(json: String): List<EmailAccount> {
        val list = mutableListOf<EmailAccount>()
        try {
            val array = org.json.JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    EmailAccount(
                        id = obj.getString("id"),
                        emailAddress = obj.getString("emailAddress"),
                        imapServer = obj.getString("imapServer"),
                        imapPort = obj.optString("imapPort", "993"),
                        smtpServer = obj.getString("smtpServer"),
                        smtpPort = obj.optString("smtpPort", "587"),
                        passwordVal = obj.getString("passwordVal")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing email accounts json", e)
        }
        return list
    }

    private fun saveEmailAccountsToDb(list: List<EmailAccount>) {
        try {
            val array = org.json.JSONArray()
            for (acc in list) {
                val obj = org.json.JSONObject()
                obj.put("id", acc.id)
                obj.put("emailAddress", acc.emailAddress)
                obj.put("imapServer", acc.imapServer)
                obj.put("imapPort", acc.imapPort)
                obj.put("smtpServer", acc.smtpServer)
                obj.put("smtpPort", acc.smtpPort)
                obj.put("passwordVal", acc.passwordVal)
                array.put(obj)
            }
            viewModelScope.launch {
                repository.saveSetting("email_accounts_json", array.toString())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error serializing email accounts json", e)
        }
    }

    fun selectActiveEmailAccount(id: String) {
        activeEmailAccountId.value = id
        viewModelScope.launch {
            repository.saveSetting("active_email_account_id", id)
            refreshLiveInbox()
        }
    }

    fun addEmailAccount(
        emailAddress: String,
        imapServer: String,
        imapPort: String,
        smtpServer: String,
        smtpPort: String,
        passwordVal: String
    ) {
        if (emailAddress.isBlank()) return
        val newAcc = EmailAccount(
            id = UUID.randomUUID().toString(),
            emailAddress = emailAddress,
            imapServer = imapServer,
            imapPort = imapPort.ifBlank { "993" },
            smtpServer = smtpServer,
            smtpPort = smtpPort.ifBlank { "587" },
            passwordVal = passwordVal
        )
        val updated = emailAccounts.value + newAcc
        emailAccounts.value = updated
        saveEmailAccountsToDb(updated)
        if (activeEmailAccountId.value == null || activeEmailAccountId.value == "default_acc" && emailAccounts.value.size <= 2) {
            selectActiveEmailAccount(newAcc.id)
        }
    }

    fun removeEmailAccount(id: String) {
        val updated = emailAccounts.value.filter { it.id != id }
        emailAccounts.value = updated
        saveEmailAccountsToDb(updated)
        if (activeEmailAccountId.value == id) {
            if (updated.isNotEmpty()) {
                selectActiveEmailAccount(updated.first().id)
            } else {
                activeEmailAccountId.value = null
                viewModelScope.launch {
                    repository.deleteSetting("active_email_account_id")
                }
            }
        }
    }

    fun setSttProvider(provider: String) {
        _sttProvider.value = provider
        viewModelScope.launch {
            repository.saveSetting("selected_stt_provider", provider)
        }
    }

    fun downloadLocalWhisper() {
        viewModelScope.launch {
            _localWhisperStatus.value = "Downloading"
            _localWhisperProgress.value = 0f
            for (i in 1..100) {
                kotlinx.coroutines.delay(15) // Fast interactive simulation
                _localWhisperProgress.value = i / 100f
            }
            _localWhisperStatus.value = "Ready"
            repository.saveSetting("whisper_local_status", "Ready")
        }
    }

    // Microphone trigger simulation
    fun triggerMicrophoneTranscription(onTranscript: (String) -> Unit) {
        if (_isRecordingAudio.value) return
        _isRecordingAudio.value = true
        _recordingFeedback.value = "Escuchando audio... (${_sttProvider.value})"
        
        viewModelScope.launch {
            kotlinx.coroutines.delay(1500)
            _recordingFeedback.value = "Procesando audio por Whisper..."
            kotlinx.coroutines.delay(1000)
            
            // Random beautiful transcription related to automated workflows
            val prompts = listOf(
                "¿Tengo algún correo pendiente sobre el Proyecto Apollo?",
                "Muéstrame la bandeja de entrada",
                "Redacta un correo para Ana confirmando que la reunión sigue programada",
                "Archivar los correos urgentes que recibí hoy",
                "¿Cuál es el estatus de las últimas tareas?"
            )
            val selected = prompts.random()
            onTranscript(selected)
            _isRecordingAudio.value = false
        }
    }

    // Attachment manipulation
    fun attachSampleImage(imageName: String?) {
        _attachedImage.value = imageName
    }

    fun selectSession(sessionId: String) {
        _selectedSessionId.value = sessionId
    }

    fun togglePlanningMode() {
        val newVal = !_planningMode.value
        _planningMode.value = newVal
        viewModelScope.launch {
            repository.saveSetting("planning_mode", newVal.toString())
        }
    }

    fun createNewSession() {
        viewModelScope.launch {
            val provider = repository.getSetting("llm_provider") ?: "Gemini"
            val session = repository.createSession("Chat Automatizado Riso", provider)
            _selectedSessionId.value = session.id
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_selectedSessionId.value == sessionId) {
                val nextSession = sessions.value.firstOrNull { it.id != sessionId }
                if (nextSession != null) {
                    _selectedSessionId.value = nextSession.id
                } else {
                    createNewSession()
                }
            }
        }
    }

    // Refresh Live Inbox (Bandeja tab)
    fun refreshLiveInbox() {
        viewModelScope.launch {
            _isInboxLoading.value = true
            try {
                val list = emailService.listInbox(15)
                _liveInbox.value = list
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing live inbox", e)
            } finally {
                _isInboxLoading.value = false
            }
        }
    }

    // Settings adjustments
    fun updateSetting(key: String, value: String) {
        viewModelScope.launch {
            repository.saveSetting(key, value)
        }
    }

    // Live test details in screen
    fun testEmailAuth() {
        viewModelScope.launch {
            _isTestingConnection.value = true
            _connectionTestResult.value = null
            try {
                val imap = repository.getSetting("imap_server") ?: ""
                val email = repository.getSetting("email_address") ?: ""
                val pass = repository.getSetting("email_password") ?: ""
                
                if (imap.isBlank() || email.isBlank() || pass.isBlank()) {
                    _connectionTestResult.value = "Utilizando sandbox offline de Riso (Sin credenciales configuradas)"
                } else {
                    val list = emailService.listInbox(1)
                    _connectionTestResult.value = "¡Conexión IMAP Exitosa! Se recuperaron ${list.size} correos."
                    refreshLiveInbox()
                }
            } catch (e: Exception) {
                _connectionTestResult.value = "Error al conectar: ${e.message}"
            } finally {
                _isTestingConnection.value = false
            }
        }
    }

    // Primary agent loop
    fun sendMessage(userText: String) {
        val sessionId = _selectedSessionId.value ?: return
        val attachedImg = _attachedImage.value
        
        if (userText.isBlank() && attachedImg == null) return

        viewModelScope.launch {
            val messageText = if (attachedImg != null) {
                "📷 [Imagen: $attachedImg.png] $userText"
            } else {
                userText
            }

            // Append user text
            repository.addMessage(
                ChatMessage(
                    sessionId = sessionId,
                    sender = "user",
                    text = messageText
                )
            )

            _isLlmLoading.value = true
            try {
                if (attachedImg != null) {
                    // Vision Engine simulation
                    kotlinx.coroutines.delay(1200)
                    val visionResponse = when (attachedImg.lowercase()) {
                        "recibo" -> """
                            **[Riso Vision Engine: Análisis de Recibo de Compra]**
                            
                            Aquí tienes la extracción de datos en tiempo real de tu imagen cargada:
                            
                            *   **Establecimiento:** Supermercado La Vega S.A.
                            *   **Fecha/Hora:** 15 de Junio, 2026, 14:32
                            *   **Código de Transacción:** TR-984021-X
                            
                            | Item | Cantidad | Precio Unitario | Total |
                            | :--- | :---: | :---: | :---: |
                            | 1. Arroz Grano Largo (1kg) | 2 | $1.50 | $3.00 |
                            | 2. Aceite de Oliva Extra Virgen | 1 | $7.20 | $7.20 |
                            | 3. Detergente Líquido Lavandina | 1 | $4.50 | $4.50 |
                            | 4. Pack de Leche Descremada x6 | 1 | $5.80 | $5.80 |
                            
                            *   **Subtotal:** $20.50
                            *   **Impuestos (IVA 19%):** $3.90
                            *   **Total de la Compra:** **$24.40**
                            
                            *Análisis de Gasto:* Este recibo califica para tu categoría de **Alimentos y Provisiones**. ¿Deseas guardar automáticamente este registro o que redacte un correo para archivar este comprobante de gastos?
                        """.trimIndent()
                        
                        "menu" -> """
                            **[Riso Vision Engine: Análisis y Traducción de Menú]**
                            
                            He escaneado y traducido el menú para ti. Aquí tienes los detalles clasificados:
                            
                            *   **Idioma Detectado:** Francés
                            *   **Traducción de Platillos Principales:**
                                1.  *Boeuf Bourguignon* ($24.00) ➔ Guiso tradicional francés de buey cocinado en vino tinto, aromatizado con ajo, cebollas, zanahorias y especias.
                                2.  *Soupe à l'Oignon* ($11.50) ➔ Sopa clásica de cebolla francesa servida hirviendo con crotones crocantes y queso Gruyère gratinado.
                                3.  *Coq au Vin* ($22.00) ➔ Pollo tierno estofado con vino tinto de Borgoña, tocino, champiñones y manteca.
                            *   **Especialidades Recomendadas:** El chef destaca el *Boeuf Bourguignon* como la gema de la casa.
                            *   **Alérgenos:** Contiene gluten en los croutones de la sopa de cebolla.
                            
                            ¿Te gustaría que te ayude a redactar un correo para hacer una reserva o consultar por variaciones vegetarianas?
                        """.trimIndent()
                        
                        else -> """
                            **[Riso Vision Engine: Resumen Analítico de Gráfico]**
                            
                            He realizado el análisis cuantitativo y estructurado del gráfico cargado:
                            
                            *   **Tipo de Gráfico:** Gráfico de Líneas de Doble Eje (Ventas Netas vs. Margen Operativo)
                            *   **Tendencia Detectada:** Se observa un crecimiento mensual continuo de **12.5%** en Ventas Netas durante Abril y Mayo, con pico de **$45,000 USD** al finalizar Mayo.
                            *   **Análisis Crítico:** El Margen Operativo sufrió una contracción de **3%** a mediados del trimestre debido a cargos extraordinarios de flete y combustible logístico en envíos.
                            *   **Métricas Q3:**
                                - Ventas Mensuales: $38,500 USD.
                                - Margen Promedio Logrado: 28.4% (Meta proyectada: 30%).
                            
                            ¿Deseas que prepare un borrador de correo con este resumen y las proyecciones detalladas para enviarlo a tu equipo?
                        """.trimIndent()
                    }
                    
                    repository.addMessage(
                        ChatMessage(
                            sessionId = sessionId,
                            sender = "ai",
                            text = visionResponse
                        )
                    )
                    _attachedImage.value = null // clear attachment
                } else {
                    // Compile history for model (GeminiContent format)
                    val conversation = compileGeminiHistory(sessionId)
                    
                    // Fetch credentials from active profile if available, otherwise legacy setting
                    val activeId = activeLlmProfileId.value ?: repository.getSetting("active_llm_profile_id") ?: ""
                    val profiles = llmProfiles.value
                    val activeProf = profiles.find { it.id == activeId } ?: profiles.firstOrNull()

                    val provider = activeProf?.provider ?: repository.getSetting("llm_provider") ?: "Gemini"
                    val key = if (activeProf != null) {
                        activeProf.apiKey
                    } else {
                        val providerKeyName = when (provider.lowercase()) {
                            "openai" -> "openai_api_key"
                            "claude" -> "claude_api_key"
                            else -> "gemini_api_key"
                        }
                        repository.getSetting(providerKeyName)
                    }

                    val mcpEmailEnabled = repository.getSetting("mcp_email_enabled") != "false"
                    val mcpGithubEnabled = repository.getSetting("mcp_github_enabled") == "true"
                    val mcpGitlabEnabled = repository.getSetting("mcp_gitlab_enabled") == "true"
                    val githubUsername = repository.getSetting("github_username") ?: ""
                    val gitlabUrl = repository.getSetting("gitlab_url") ?: "https://gitlab.com"

                    // Call LLM Resolver with tools
                    val response = llmService.resolveLlm(
                        history = conversation,
                        provider = provider,
                        customApiKey = key,
                        mcpEmailEnabled = mcpEmailEnabled,
                        mcpGithubEnabled = mcpGithubEnabled,
                        mcpGitlabEnabled = mcpGitlabEnabled,
                        githubUsername = githubUsername,
                        gitlabUrl = gitlabUrl
                    )
                    processLlmResponse(sessionId, response)
                }

            } catch (e: Throwable) {
                Log.e(TAG, "Error in primary agent loop message loop", e)
                repository.addMessage(
                    ChatMessage(
                        sessionId = sessionId,
                        sender = "ai",
                        text = "Riso Error: No se pudo resolver la respuesta localmente. Detalles: ${e.localizedMessage ?: e.message ?: "error desconocido"}. Por favor, revisa tu conexión a Internet y tus claves de API en Ajustes."
                    )
                )
            } finally {
                _isLlmLoading.value = false
            }
        }
    }

    // Process Response candidates and check for tool functionCall
    private suspend fun processLlmResponse(sessionId: String, response: GeminiResponse) {
        val candidate = response.candidates?.firstOrNull() ?: return
        val textResponse = candidate.content.parts.firstOrNull { it.text != null }?.text
        val functionCall = candidate.content.parts.firstOrNull { it.functionCall != null }?.functionCall

        if (functionCall != null) {
            handleFunctionCalling(sessionId, functionCall)
        } else if (textResponse != null) {
            repository.addMessage(
                ChatMessage(
                    sessionId = sessionId,
                    sender = "ai",
                    text = textResponse
                )
            )
        } else {
            repository.addMessage(
                ChatMessage(
                    sessionId = sessionId,
                    sender = "ai",
                    text = "Riso: Recibí una respuesta vacía del modelo."
                )
            )
        }
    }

    // Handles the 10 core email tasks and enforces the Planning Mode safety shield
    private suspend fun handleFunctionCalling(sessionId: String, call: GeminiFunctionCall) {
        val fName = call.name
        val args = call.args ?: emptyMap()
        Log.d(TAG, "Function Call Invoked: $fName, Args: $args")

        val isWriteAction = fName in listOf(
            "send_email", "reply_to_email", "forward_email",
            "mark_as_read", "mark_as_unread", "archive_email", "delete_email",
            "create_github_issue", "create_gitlab_issue"
        )

        if (isWriteAction && _planningMode.value) {
            // SAFE PLAN EXPLAIN: planning mode restricts execution
            val planDetails = compilePlanDescription(fName, args)
            val msgId = UUID.randomUUID().toString()
            
            // Create pending action in local SQLite
            val argsString = moshi.adapter(Map::class.java).toJson(args)
            val action = repository.createPendingAction(
                messageId = msgId,
                fName,
                argsString,
                planDetails
            )

            val headTitle = when {
                fName.contains("github") -> "🛡️ **Planificación de Acción en GitHub**"
                fName.contains("gitlab") -> "🛡️ **Planificación de Acción en GitLab**"
                else -> "🛡️ **Planificación de Acción de Correo**"
            }

            // Prompt user with Action Card inline in chat
            repository.addMessage(
                ChatMessage(
                    id = msgId,
                    sessionId = sessionId,
                    sender = "ai",
                    text = "$headTitle\nHe planeado la siguiente acción de escritura. Por favor, revísala y confirma.",
                    pendingActionId = action.id
                )
            )
        } else {
            // Direct execution mode: runs immediately
            executeFunction(sessionId, fName, args)
        }
    }

    // User approves a pending plan
    fun approveAction(actionId: String) {
        val sessionId = _selectedSessionId.value ?: return
        viewModelScope.launch {
            val action = database.risoDao().getPendingActionById(actionId) ?: return@launch
            repository.updatePendingActionStatus(actionId, "APPROVED")
            
            // Add system note to chat
            repository.addMessage(
                ChatMessage(
                    sessionId = sessionId,
                    sender = "system",
                    text = "⚡ *(Acción aprobada por el usuario. Ejecutando ${action.functionName}...)*"
                )
            )

            // Execute function call using saved arguments
            val argsMap = moshi.adapter(Map::class.java).fromJson(action.argumentsJson) as? Map<String, Any?> ?: emptyMap()
            executeFunction(sessionId, action.functionName, argsMap)
        }
    }

    // User rejects a pending plan
    fun rejectAction(actionId: String) {
        val sessionId = _selectedSessionId.value ?: return
        viewModelScope.launch {
            val action = database.risoDao().getPendingActionById(actionId) ?: return@launch
            repository.updatePendingActionStatus(actionId, "REJECTED")
            
            repository.addMessage(
                ChatMessage(
                    sessionId = sessionId,
                    sender = "ai",
                    text = "Acción cancelada. He rechazado el plan para ejecutar `${action.functionName}`."
                )
            )
        }
    }

    // Execute the actual email service routines
    private suspend fun executeFunction(sessionId: String, name: String, args: Map<String, Any?>) {
        _isLlmLoading.value = true
        try {
            val resultObject = when (name) {
                "list_inbox" -> {
                    val lim = (args["limit"] as? Number)?.toInt() ?: 5
                    val emails = emailService.listInbox(lim)
                    mapOf("emails" to emails.map { it.toMap() })
                }
                "search_emails" -> {
                    val q = args["query"]?.toString() ?: ""
                    val lim = (args["limit"] as? Number)?.toInt() ?: 5
                    val emails = emailService.searchEmails(q, lim)
                    mapOf("emails" to emails.map { it.toMap() })
                }
                "read_email" -> {
                    val id = args["id"]?.toString() ?: ""
                    val email = emailService.readEmail(id)
                    mapOf("email" to email?.toMap())
                }
                "send_email" -> {
                    val to = args["to"]?.toString() ?: ""
                    val sub = args["subject"]?.toString() ?: ""
                    val b = args["body"]?.toString() ?: ""
                    val success = emailService.sendEmail(to, sub, b)
                    mapOf("success" to success, "destination" to to)
                }
                "reply_to_email" -> {
                    val mid = args["messageId"]?.toString() ?: ""
                    val b = args["body"]?.toString() ?: ""
                    val success = emailService.replyToEmail(mid, b)
                    mapOf("success" to success, "repliedTo" to mid)
                }
                "forward_email" -> {
                    val mid = args["messageId"]?.toString() ?: ""
                    val to = args["to"]?.toString() ?: ""
                    val b = args["body"]?.toString() ?: ""
                    val success = emailService.forwardEmail(mid, to, b)
                    mapOf("success" to success, "forwardedTo" to to)
                }
                "mark_as_read" -> {
                    val id = args["id"]?.toString() ?: ""
                    val success = emailService.markAsRead(id)
                    mapOf("success" to success, "id" to id)
                }
                "mark_as_unread" -> {
                    val id = args["id"]?.toString() ?: ""
                    val success = emailService.markAsUnread(id)
                    mapOf("success" to success, "id" to id)
                }
                "archive_email" -> {
                    val id = args["id"]?.toString() ?: ""
                    val success = emailService.archiveEmail(id)
                    mapOf("success" to success, "id" to id)
                }
                "delete_email" -> {
                    val id = args["id"]?.toString() ?: ""
                    val success = emailService.deleteEmail(id)
                    mapOf("success" to success, "id" to id)
                }
                "list_github_repositories" -> {
                    val u = args["username"]?.toString() ?: repository.getSetting("github_username") ?: ""
                    mapOf(
                        "success" to true,
                        "username" to u,
                        "repositories" to listOf(
                            mapOf("name" to "riso-android-mcp", "stars" to 42, "language" to "Kotlin", "description" to "Local Android agent client with support for model-driven orchestration."),
                            mapOf("name" to "stellar-agent-engine", "stars" to 128, "language" to "TypeScript", "description" to "A modular agent task supervisor.")
                        )
                    )
                }
                "list_github_issues" -> {
                    val owner = args["owner"]?.toString() ?: ""
                    val repo = args["repo"]?.toString() ?: ""
                    val state = args["state"]?.toString() ?: "open"
                    mapOf(
                        "success" to true,
                        "owner" to owner,
                        "repo" to repo,
                        "state" to state,
                        "issues" to listOf(
                            mapOf("number" to 101, "title" to "Corregir padding en caja de chat", "state" to "open", "author" to "cyber_coder"),
                            mapOf("number" to 102, "title" to "Soporte oauth en conexiones mcp", "state" to "open", "author" to "riso_dev")
                        )
                    )
                }
                "create_github_issue" -> {
                    val owner = args["owner"]?.toString() ?: ""
                    val repo = args["repo"]?.toString() ?: ""
                    val title = args["title"]?.toString() ?: ""
                    val body = args["body"]?.toString() ?: ""
                    mapOf(
                        "success" to true,
                        "owner" to owner,
                        "repo" to repo,
                        "issue_number" to 103,
                        "title" to title,
                        "body" to body
                    )
                }
                "list_gitlab_projects" -> {
                    val membership = args["membership"]?.toString() ?: "true"
                    mapOf(
                        "success" to true,
                        "membership" to membership,
                        "projects" to listOf(
                            mapOf("id" to 49021, "name" to "internal-security-scanner", "visibility" to "private"),
                            mapOf("id" to 22891, "name" to "corporate-dashboard-v2", "visibility" to "internal")
                        )
                    )
                }
                "create_gitlab_issue" -> {
                    val projectId = args["projectId"]?.toString() ?: ""
                    val title = args["title"]?.toString() ?: ""
                    val desc = args["description"]?.toString() ?: ""
                    mapOf(
                        "success" to true,
                        "projectId" to projectId,
                        "issue_id" to 9951,
                        "title" to title,
                        "description" to desc
                    )
                }
                else -> mapOf("error" to "Función desconocida `$name`")
            }

            // Post result back to LLM to get a structured verbal response in context
            val formattedResult = moshi.adapter(Map::class.java).toJson(resultObject)
            Log.d(TAG, "Tool output returned to model: $formattedResult")

            // Add result as a function response part to conversation history
            val conversation = compileGeminiHistory(sessionId).toMutableList()
            conversation.add(
                GeminiContent(
                    role = "user",
                    parts = listOf(
                        GeminiPart(
                            functionResponse = GeminiFunctionResponse(
                                name = name,
                                response = mapOf("result" to resultObject)
                            )
                        )
                    )
                )
            )

            // Re-resolve with selected LLM model profile to explain the tool outputs back to the user
            val activeId = activeLlmProfileId.value ?: repository.getSetting("active_llm_profile_id") ?: ""
            val profiles = llmProfiles.value
            val activeProf = profiles.find { it.id == activeId } ?: profiles.firstOrNull()

            val provider = activeProf?.provider ?: repository.getSetting("llm_provider") ?: "Gemini"
            val key = if (activeProf != null) {
                activeProf.apiKey
            } else {
                val providerKeyName = when (provider.lowercase()) {
                    "openai" -> "openai_api_key"
                    "claude" -> "claude_api_key"
                    else -> "gemini_api_key"
                }
                repository.getSetting(providerKeyName)
            }

            val mcpEmailEnabled = repository.getSetting("mcp_email_enabled") != "false"
            val mcpGithubEnabled = repository.getSetting("mcp_github_enabled") == "true"
            val mcpGitlabEnabled = repository.getSetting("mcp_gitlab_enabled") == "true"
            val githubUsername = repository.getSetting("github_username") ?: ""
            val gitlabUrl = repository.getSetting("gitlab_url") ?: "https://gitlab.com"

            val nextResponse = llmService.resolveLlm(
                history = conversation,
                provider = provider,
                customApiKey = key,
                mcpEmailEnabled = mcpEmailEnabled,
                mcpGithubEnabled = mcpGithubEnabled,
                mcpGitlabEnabled = mcpGitlabEnabled,
                githubUsername = githubUsername,
                gitlabUrl = gitlabUrl
            )
            
            processLlmResponse(sessionId, nextResponse)
            
            // Auto refresh inbox tabs
            if (mcpEmailEnabled) {
                refreshLiveInbox()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error executing tool function call", e)
            repository.addMessage(
                ChatMessage(
                    sessionId = sessionId,
                    sender = "ai",
                    text = "Ocurrió un error al ejecutar la tarea automatizada: ${e.message}"
                )
            )
        } finally {
            _isLlmLoading.value = false
        }
    }

    private fun compilePlanDescription(fName: String, args: Map<String, Any?>): String {
        return when (fName) {
            "send_email" -> "Enviar un correo a ${args["to"]} con el asunto '${args["subject"]}'."
            "reply_to_email" -> "Responder al correo ${args["messageId"]} con el cuerpo especificado."
            "forward_email" -> "Reenviar el correo ${args["messageId"]} a ${args["to"]}."
            "mark_as_read" -> "Marcar el correo '${args["id"]}' como leído."
            "mark_as_unread" -> "Marcar el correo '${args["id"]}' como NO leído."
            "archive_email" -> "Archivar el correo '${args["id"]}' (remover de la bandeja)."
            "delete_email" -> "Mover el correo '${args["id"]}' a la papelera (eliminar)."
            "create_github_issue" -> "Crear un issue en GitHub en '${args["owner"]}/${args["repo"]}' con título '${args["title"]}'."
            "create_gitlab_issue" -> "Crear un issue en GitLab en el proyecto '${args["projectId"]}' con título '${args["title"]}'."
            else -> "Ejecutar la operación '$fName'."
        }
    }

    private fun compileGeminiHistory(sessionId: String): List<GeminiContent> {
        val list = currentMessages.value
        return list.filter { it.sender != "system" }.takeLast(10).map { msg ->
            GeminiContent(
                role = if (msg.sender == "user") "user" else "model",
                parts = listOf(GeminiPart(text = msg.text))
            )
        }
    }

    fun parseLlmProfiles(json: String): List<LlmProfile> {
        val list = mutableListOf<LlmProfile>()
        try {
            val array = org.json.JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    LlmProfile(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        provider = obj.getString("provider"),
                        apiKey = obj.getString("apiKey")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing llm profiles json", e)
        }
        return list
    }

    fun saveLlmProfilesToDb(list: List<LlmProfile>) {
        try {
            val array = org.json.JSONArray()
            for (prof in list) {
                val obj = org.json.JSONObject()
                obj.put("id", prof.id)
                obj.put("name", prof.name)
                obj.put("provider", prof.provider)
                obj.put("apiKey", prof.apiKey)
                array.put(obj)
            }
            viewModelScope.launch {
                repository.saveSetting("llm_profiles_json", array.toString())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving llm profiles json", e)
        }
    }

    fun addLlmProfile(name: String, provider: String, apiKey: String) {
        if (name.isBlank() || apiKey.isBlank()) return
        val newProfile = LlmProfile(
            id = UUID.randomUUID().toString(),
            name = name,
            provider = provider,
            apiKey = apiKey
        )
        val updatedList = llmProfiles.value + newProfile
        llmProfiles.value = updatedList
        saveLlmProfilesToDb(updatedList)
        if (activeLlmProfileId.value == null || activeLlmProfileId.value == "") {
            selectActiveLlmProfile(newProfile.id)
        }
    }

    fun removeLlmProfile(profileId: String) {
        val updatedList = llmProfiles.value.filter { it.id != profileId }
        llmProfiles.value = updatedList
        saveLlmProfilesToDb(updatedList)
        if (activeLlmProfileId.value == profileId) {
            val nextProfile = updatedList.firstOrNull()
            if (nextProfile != null) {
                selectActiveLlmProfile(nextProfile.id)
            } else {
                activeLlmProfileId.value = null
                viewModelScope.launch {
                    repository.saveSetting("active_llm_profile_id", "")
                }
            }
        }
    }

    fun selectActiveLlmProfile(profileId: String) {
        activeLlmProfileId.value = profileId
        viewModelScope.launch {
            repository.saveSetting("active_llm_profile_id", profileId)
            // Synchronize legacy `llm_provider` label to keep other tabs working
            val prof = llmProfiles.value.find { it.id == profileId }
            if (prof != null) {
                repository.saveSetting("llm_provider", prof.provider)
            }
        }
    }

    fun attachCustomFile(name: String) {
        _attachedImage.value = name
    }

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private fun RisoEmail.toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "sender" to sender,
            "recipient" to recipient,
            "subject" to subject,
            "snippet" to snippet,
            "body" to body,
            "date" to date,
            "isRead" to isRead
        )
    }
}

data class LlmProfile(
    val id: String,
    val name: String,
    val provider: String, // "Gemini" | "OpenAI" | "Claude"
    val apiKey: String
)
