package com.example.ui

import com.example.BuildConfig
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.data.model.ChatMessage
import com.example.data.model.PendingAction
import com.example.service.email.RisoEmail
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.graphics.Bitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RisoMainScreen(
    viewModel: RisoViewModel,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf("chat") } // "chat" | "inbox" | "settings"
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    
    val emailAccounts by viewModel.emailAccounts.collectAsStateWithLifecycle()
    val activeEmailAccountId by viewModel.activeEmailAccountId.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val selectedSessionId by viewModel.selectedSessionId.collectAsStateWithLifecycle()
    
    val isEn = settings["language"] == "en"
    fun t(key: String): String = L10n.t(key, isEn)
    
    val activeAccount = emailAccounts.find { it.id == activeEmailAccountId }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Drawer Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 20.dp, top = 8.dp)
                    ) {
                        RisoLogo(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF151522)) // Meets modern deep slate aesthetic
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Riso Chatbot",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = t("local_offline"),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Chat History Section
                    Text(
                        text = t("chat_history").uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 4.dp)
                    )

                    // "+ Nuevo Chat" Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .clickable {
                                viewModel.createNewSession()
                                currentTab = "chat"
                                coroutineScope.launch { drawerState.close() }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("➕", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = t("new_chat"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Scrollable list of sessions
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        sessions.forEach { session ->
                            val isSelected = session.id == selectedSessionId && currentTab == "chat"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable {
                                        viewModel.selectSession(session.id)
                                        currentTab = "chat"
                                        coroutineScope.launch { drawerState.close() }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("💬", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = session.title ?: "Chat Riso",
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (sessions.size > 1) {
                                    IconButton(
                                        onClick = { viewModel.deleteSession(session.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text("🗑️", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Settings option at the bottom
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings option") },
                        label = { Text(t("config_drawer"), fontWeight = FontWeight.SemiBold) },
                        selected = currentTab == "settings",
                        onClick = {
                            currentTab = "settings"
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .testTag("nav_settings_drawer")
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Drawer Footer: Quick Theme Mode and Active Info
                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val themeMode = settings["theme_mode"] ?: "light"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (themeMode == "light") t("theme_light") else t("theme_dark"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box(
                            modifier = Modifier.size(54.dp, 34.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Switch(
                                checked = themeMode == "light",
                                onCheckedChange = { isLight ->
                                    viewModel.updateSetting("theme_mode", if (isLight) "light" else "dark")
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = t("active_account") + (activeAccount?.emailAddress ?: t("offline")),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = when (currentTab) {
                                    "chat" -> t("chat_agent")
                                    else -> t("settings_title")
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            if (currentTab == "chat" && activeAccount != null) {
                                Text(
                                    text = activeAccount.emailAddress,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Hamburguer menu")
                        }
                    },
                    actions = {
                        if (currentTab == "chat") {
                            IconButton(onClick = { viewModel.createNewSession() }) {
                                Icon(Icons.Default.Add, contentDescription = "Nueva conversación")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (currentTab) {
                    "chat" -> RisoChatScreen(viewModel = viewModel)
                    else -> RisoSettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}

// --- TAB 1: Chat Automation Agent Screen ---
@Composable
fun RisoChatScreen(viewModel: RisoViewModel) {
    val messages by viewModel.currentMessages.collectAsStateWithLifecycle()
    val isLlmLoading by viewModel.isLlmLoading.collectAsStateWithLifecycle()
    val planningMode by viewModel.planningMode.collectAsStateWithLifecycle()
    val pendingActions by viewModel.pendingActions.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val emailAccounts by viewModel.emailAccounts.collectAsStateWithLifecycle()
    val activeEmailAccountId by viewModel.activeEmailAccountId.collectAsStateWithLifecycle()

    val sttProvider by viewModel.sttProvider.collectAsStateWithLifecycle()
    val whisperStatus by viewModel.localWhisperStatus.collectAsStateWithLifecycle()
    val isRecordingAudio by viewModel.isRecordingAudio.collectAsStateWithLifecycle()
    val recordingFeedback by viewModel.recordingFeedback.collectAsStateWithLifecycle()
    val attachedImage by viewModel.attachedImage.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf("") }
    var showAttachMenu by remember { mutableStateOf(false) }
    var showModelSelector by remember { mutableStateOf(false) }

    // Launcher integrations for physical device testing as requested
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.attachCustomFile("Galería: ${uri.lastPathSegment ?: "imagen.png"}")
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.attachCustomFile("Cámara: foto_capturada.png")
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.attachCustomFile("Archivo: ${uri.lastPathSegment ?: "documento.pdf"}")
        }
    }

    var tempGithubUsername by remember(settings) { mutableStateOf(settings["github_username"] ?: "") }
    var tempGithubPat by remember(settings) { mutableStateOf(settings["github_pat"] ?: "") }
    var tempGitlabUrl by remember(settings) { mutableStateOf(settings["gitlab_url"] ?: "https://gitlab.com") }
    var tempGitlabPat by remember(settings) { mutableStateOf(settings["gitlab_pat"] ?: "") }

    var oauthGithubWorking by remember { mutableStateOf(false) }
    var oauthGitlabWorking by remember { mutableStateOf(false) }

    var showMailManager by remember { mutableStateOf(false) }
    var newEmailAddress by remember { mutableStateOf("") }
    var newEmailPass by remember { mutableStateOf("") }
    var newImapHost by remember { mutableStateOf("") }
    var newSmtpHost by remember { mutableStateOf("") }

    val mcpEmailEnabled = settings["mcp_email_enabled"] != "false"
    val mcpGithubEnabled = settings["mcp_github_enabled"] == "true"
    val mcpGitlabEnabled = settings["mcp_gitlab_enabled"] == "true"
    
    val isEn = settings["language"] == "en"
    fun t(key: String): String = L10n.t(key, isEn)
    val currentProvider = settings["llm_provider"] ?: "Gemini"

    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    // Auto scroll down with messages
    LaunchedEffect(messages.size, isLlmLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Ultra streamlined model picker selector inspired by Gemini Android App
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { showModelSelector = true }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val activeLLM = currentProvider
                val activeWhisper = if (sttProvider == "Whisper Local small-v3") "Local" else "API"
                Text(
                    text = "✨ $activeLLM + Whisper ($activeWhisper) ▾",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Warning bar for downloading local Whisper models Offline
        if (sttProvider == "Whisper Local small-v3" && whisperStatus != "Ready") {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = t("download_whisper_warn"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = t("download_now"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { viewModel.downloadLocalWhisper() }
                        .padding(horizontal = 4.dp)
                )
            }
        }

        // Model selector custom Dialog matching the Gemini Advanced picker
        if (showModelSelector) {
            Dialog(onDismissRequest = { showModelSelector = false }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = t("model_selector_title"),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = t("model_selector_sub"),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            IconButton(
                                onClick = { showModelSelector = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Section 1: LLM Engine
                        Text(
                            text = t("llm_header"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val availableModels = mutableListOf<Triple<String, String, String>>()
                        
                        // Gemini is always available if global compiled key, or any configured
                        val geminiKey = settings["gemini_api_key"]
                        val hasGemini = !geminiKey.isNullOrBlank() || BuildConfig.GEMINI_API_KEY.isNotBlank()
                        if (hasGemini) {
                            availableModels.add(Triple("Gemini", "🤖 Gemini", t("llm_gemini_desc")))
                        }
                        
                        val openaiKey = settings["openai_api_key"]
                        if (!openaiKey.isNullOrBlank()) {
                            availableModels.add(Triple("OpenAI", "⚡ OpenAI", t("llm_openai_desc")))
                        }
                        
                        val claudeKey = settings["claude_api_key"]
                        if (!claudeKey.isNullOrBlank()) {
                            availableModels.add(Triple("Claude", "🔮 Claude", t("llm_claude_desc")))
                        }
                        
                        if (availableModels.isEmpty()) {
                            availableModels.add(Triple("Gemini", "🤖 Gemini", t("llm_gemini_desc")))
                        }

                        availableModels.forEach { (id, title, desc) ->
                            val isSel = currentProvider == id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                                    .clickable { viewModel.updateSetting("llm_provider", id) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                RadioButton(
                                    selected = isSel,
                                    onClick = { viewModel.updateSetting("llm_provider", id) },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.scale(0.85f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Section 2: Speech to Text (STT) Engine
                        Text(
                            text = t("voice_header"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        listOf(
                            Triple("Whisper API", "🎙️ Whisper API", t("stt_whisper_api_desc")),
                            Triple("Whisper Local small-v3", "📦 Whisper Local", t("stt_whisper_local_desc"))
                        ).forEach { (id, title, desc) ->
                            val isSel = sttProvider == id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f) else Color.Transparent)
                                    .clickable { viewModel.setSttProvider(id) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                RadioButton(
                                    selected = isSel,
                                    onClick = { viewModel.setSttProvider(id) },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.secondary),
                                    modifier = Modifier.scale(0.85f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showModelSelector = false },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("OK", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        // Chat Conversation window
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                ChatMessageItem(
                    message = msg,
                    pendingActions = pendingActions,
                    onApprove = { viewModel.approveAction(it) },
                    onReject = { viewModel.rejectAction(it) }
                )
            }

            if (isLlmLoading) {
                item {
                    LlmStreamingLoader()
                }
            }
        }

        // Recording Feedback Banner
        if (isRecordingAudio) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = recordingFeedback,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Image Attachment Thumbnail Preview Box
        if (attachedImage != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("📷", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Archivo: $attachedImage.png",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = when(attachedImage) {
                            "recibo" -> "Recibo de Compra"
                            "menu" -> "Menú de Restaurant"
                            else -> "Gráfico Analítico Q3"
                        },
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remover archivo",
                    tint = Color.Red,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { viewModel.attachSampleImage(null) }
                )
            }
        }

        // Subtle Planning Mode switch right over the chat input box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = if (planningMode) t("planning_mode_active") else t("planning_mode_inactive"),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (planningMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(end = 6.dp)
            )
            Box(
                modifier = Modifier.size(54.dp, 34.dp),
                contentAlignment = Alignment.Center
            ) {
                Switch(
                    checked = planningMode,
                    onCheckedChange = { viewModel.togglePlanningMode() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.tertiary,
                        checkedTrackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f),
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    ),
                    modifier = Modifier
                        .scale(0.7f)
                        .testTag("toggle_planning_mode_chat")
                )
            }
        }

        // Bottom Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .background(Color.Transparent),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text(t("chat_input_placeholder"), fontSize = 14.sp) },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                maxLines = 4,
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_text_field"),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (textInput.isNotBlank() || attachedImage != null) {
                        viewModel.sendMessage(textInput)
                        textInput = ""
                        keyboardController?.hide()
                    }
                }),
                leadingIcon = {
                    IconButton(
                        onClick = { showAttachMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("➕", fontSize = 18.sp, modifier = Modifier.testTag("mcp_attachments_plus_button"))
                    }
                },
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp) // Beautiful border on the safe right side
                    ) {
                        // Plus (+) attachment action for convenient bottom-right trigger!
                        IconButton(
                            onClick = { showAttachMenu = true },
                            modifier = Modifier.size(36.dp).testTag("chat_attach_button_right")
                        ) {
                            Text("➕", fontSize = 16.sp)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Microphone button integrated directly inside the text input
                        IconButton(
                            onClick = {
                                viewModel.triggerMicrophoneTranscription { transcription ->
                                    textInput = transcription
                                }
                            },
                            enabled = !isRecordingAudio,
                            modifier = Modifier.size(36.dp).testTag("chat_stt_microphone")
                        ) {
                            Text(
                                text = "🎙️",
                                fontSize = 16.sp,
                                color = if (isRecordingAudio) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        // Send button embedded inside the text field on the safe RHS
                        IconButton(
                            onClick = {
                                if (textInput.isNotBlank() || attachedImage != null) {
                                    viewModel.sendMessage(textInput)
                                    textInput = ""
                                    keyboardController?.hide()
                                }
                            },
                            enabled = textInput.isNotBlank() || attachedImage != null,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (textInput.isNotBlank() || attachedImage != null)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                )
                                .testTag("chat_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (textInput.isNotBlank() || attachedImage != null) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            )
        }
    }

    // Unified MCP Connections + Attachments Dialog
    if (showAttachMenu) {
        Dialog(onDismissRequest = { showAttachMenu = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "MCP Conexiones & Herramientas",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Activa y configura conectores del chat local",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        IconButton(
                            onClick = { showAttachMenu = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // SECTION 1: MCP EMAIL
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("📬", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Conector MCP Correo (IMAP/SMTP)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Sincroniza y redacta emails desde el chat", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                        }
                                    }
                                    Box(
                                        modifier = Modifier.size(54.dp, 34.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Switch(
                                            checked = mcpEmailEnabled,
                                            onCheckedChange = { isChecked ->
                                                viewModel.updateSetting("mcp_email_enabled", if (isChecked) "true" else "false")
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                            ),
                                            modifier = Modifier.scale(0.75f).testTag("toggle_mcp_email")
                                        )
                                    }
                                }

                                if (mcpEmailEnabled) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Render connected accounts
                                    if (emailAccounts.isEmpty()) {
                                        Text("No hay cuentas configuradas.", fontSize = 11.sp, color = Color.Red, modifier = Modifier.padding(vertical = 4.dp))
                                    } else {
                                        emailAccounts.forEach { acc ->
                                            val isActive = acc.id == activeEmailAccountId
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 2.dp)
                                                    .background(if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent, RoundedCornerShape(6.dp))
                                                    .padding(4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.clickable { viewModel.selectActiveEmailAccount(acc.id) }) {
                                                    Text(acc.emailAddress, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text("Active: $isActive • IMAP: ${acc.imapServer}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                                }
                                                IconButton(
                                                    onClick = { viewModel.removeEmailAccount(acc.id) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Text("🗑️", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = { showMailManager = !showMailManager },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(if (showMailManager) "▲ Ocultar formulario" else "▼ Añadir nueva cuenta de correo...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    if (showMailManager) {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedTextField(
                                                value = newEmailAddress,
                                                onValueChange = { newEmailAddress = it },
                                                label = { Text("Correo", fontSize = 11.sp) },
                                                placeholder = { Text("ejemplo@gmail.com") },
                                                shape = RoundedCornerShape(8.dp),
                                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = newEmailPass,
                                                onValueChange = { newEmailPass = it },
                                                label = { Text("Contraseña / App Pass", fontSize = 11.sp) },
                                                visualTransformation = PasswordVisualTransformation(),
                                                shape = RoundedCornerShape(8.dp),
                                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                OutlinedTextField(
                                                    value = newImapHost,
                                                    onValueChange = { newImapHost = it },
                                                    label = { Text("IMAP", fontSize = 11.sp) },
                                                    placeholder = { Text("imap.gmail.com") },
                                                    shape = RoundedCornerShape(8.dp),
                                                    textStyle = LocalTextStyle.current.copy(fontSize = 9.sp),
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true
                                                )
                                                OutlinedTextField(
                                                    value = newSmtpHost,
                                                    onValueChange = { newSmtpHost = it },
                                                    label = { Text("SMTP", fontSize = 11.sp) },
                                                    placeholder = { Text("smtp.gmail.com") },
                                                    shape = RoundedCornerShape(8.dp),
                                                    textStyle = LocalTextStyle.current.copy(fontSize = 9.sp),
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true
                                                )
                                            }
                                            Button(
                                                onClick = {
                                                    if (newEmailAddress.isNotBlank() && newEmailPass.isNotBlank()) {
                                                        viewModel.addEmailAccount(
                                                            emailAddress = newEmailAddress,
                                                            imapServer = if (newImapHost.isBlank()) "imap.gmail.com" else newImapHost,
                                                            imapPort = "993",
                                                            smtpServer = if (newSmtpHost.isBlank()) "smtp.gmail.com" else newSmtpHost,
                                                            smtpPort = "587",
                                                            passwordVal = newEmailPass
                                                        )
                                                        newEmailAddress = ""
                                                        newEmailPass = ""
                                                        newImapHost = ""
                                                        newSmtpHost = ""
                                                        showMailManager = false
                                                    }
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                modifier = Modifier.fillMaxWidth().height(32.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text("Guardar Cuenta de Correo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // SECTION 2: MCP GITHUB
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🐙", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Conector MCP GitHub (PAT / OAuth)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Administra repos y crea issues", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                        }
                                    }
                                    Box(
                                        modifier = Modifier.size(54.dp, 34.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Switch(
                                            checked = mcpGithubEnabled,
                                            onCheckedChange = { isChecked ->
                                                viewModel.updateSetting("mcp_github_enabled", if (isChecked) "true" else "false")
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                            ),
                                            modifier = Modifier.scale(0.75f).testTag("toggle_mcp_github")
                                        )
                                    }
                                }

                                if (mcpGithubEnabled) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                                    Spacer(modifier = Modifier.height(6.dp))

                                    OutlinedTextField(
                                        value = tempGithubUsername,
                                        onValueChange = {
                                            tempGithubUsername = it
                                            viewModel.updateSetting("github_username", it)
                                        },
                                        label = { Text("Usuario GitHub", fontSize = 11.sp) },
                                        shape = RoundedCornerShape(8.dp),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = tempGithubPat,
                                        onValueChange = {
                                            tempGithubPat = it
                                            viewModel.updateSetting("github_pat", it)
                                        },
                                        label = { Text("Personal Access Token (PAT)", fontSize = 11.sp) },
                                        visualTransformation = PasswordVisualTransformation(),
                                        shape = RoundedCornerShape(8.dp),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        TextButton(
                                            onClick = {
                                                oauthGithubWorking = true
                                                coroutineScope.launch {
                                                    kotlinx.coroutines.delay(1200)
                                                    oauthGithubWorking = false
                                                    tempGithubUsername = "user_github_oauth"
                                                    tempGithubPat = "gho_simulated_oauth_secret_token"
                                                    viewModel.updateSetting("github_username", "user_github_oauth")
                                                    viewModel.updateSetting("github_pat", "gho_simulated_oauth_secret_token")
                                                }
                                            },
                                            modifier = Modifier.height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text("Conectar vía OAuth (Simulado) 🔗", fontSize = 11.sp)
                                        }

                                        if (oauthGithubWorking) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        } else if (tempGithubPat.startsWith("gho_")) {
                                            Text("🟢 OAuth Conectado", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // SECTION 3: MCP GITLAB
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🦊", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Conector MCP GitLab (PAT / OAuth)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Interactúa con servidores GitLab", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                        }
                                    }
                                    Box(
                                        modifier = Modifier.size(54.dp, 34.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Switch(
                                            checked = mcpGitlabEnabled,
                                            onCheckedChange = { isChecked ->
                                                viewModel.updateSetting("mcp_gitlab_enabled", if (isChecked) "true" else "false")
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                            ),
                                            modifier = Modifier.scale(0.75f).testTag("toggle_mcp_gitlab")
                                        )
                                    }
                                }

                                if (mcpGitlabEnabled) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                                    Spacer(modifier = Modifier.height(6.dp))

                                    OutlinedTextField(
                                        value = tempGitlabUrl,
                                        onValueChange = {
                                            tempGitlabUrl = it
                                            viewModel.updateSetting("gitlab_url", it)
                                        },
                                        label = { Text("URL de GitLab", fontSize = 11.sp) },
                                        placeholder = { Text("https://gitlab.com") },
                                        shape = RoundedCornerShape(8.dp),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = tempGitlabPat,
                                        onValueChange = {
                                            tempGitlabPat = it
                                            viewModel.updateSetting("gitlab_pat", it)
                                        },
                                        label = { Text("Personal Access Token (PAT)", fontSize = 11.sp) },
                                        visualTransformation = PasswordVisualTransformation(),
                                        shape = RoundedCornerShape(8.dp),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        TextButton(
                                            onClick = {
                                                oauthGitlabWorking = true
                                                coroutineScope.launch {
                                                    kotlinx.coroutines.delay(1200)
                                                    oauthGitlabWorking = false
                                                    tempGitlabPat = "glo_simulated_oauth_secret_token"
                                                    viewModel.updateSetting("gitlab_pat", "glo_simulated_oauth_secret_token")
                                                }
                                            },
                                            modifier = Modifier.height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text("Conectar vía OAuth (Simulado) 🔗", fontSize = 11.sp)
                                        }

                                        if (oauthGitlabWorking) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        } else if (tempGitlabPat.startsWith("glo_")) {
                                            Text("🟢 OAuth Conectado", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // SECTION: ADJUNTAR DESDE TU TELÉFONO (REAL DEVICE LAUNCHERS)
                        Column {
                            Text(
                                text = "Adjuntar desde tu Teléfono",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Camera Option
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            try {
                                                cameraLauncher.launch(null)
                                            } catch (e: Exception) {
                                                viewModel.attachCustomFile("Foto Simulada (Cámara)")
                                            }
                                            showAttachMenu = false
                                        }
                                        .padding(2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("📸", fontSize = 20.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Cámara", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Gallery Option
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            try {
                                                galleryLauncher.launch("image/*")
                                            } catch (e: Exception) {
                                                viewModel.attachCustomFile("Imagen Simulada (Galería)")
                                            }
                                            showAttachMenu = false
                                        }
                                        .padding(2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("🖼️", fontSize = 20.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Galería", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // File Option
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            try {
                                                filePickerLauncher.launch("*/*")
                                            } catch (e: Exception) {
                                                viewModel.attachCustomFile("Documento Simulado")
                                            }
                                            showAttachMenu = false
                                        }
                                        .padding(2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("📂", fontSize = 20.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Archivo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // SECTION 4: ATTACHMENTS FOR IA VISION
                        Column {
                            Text(
                                text = "Archivos de Simulación para IA Visión",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            listOf(
                                Triple("recibo", "🧾 Recibo de Compra.png", "Extrae precios, total e impuestos"),
                                Triple("menu", "🍽️ Menú de Restaurant.png", "Traduce platos clásicos franceses"),
                                Triple("grafico", "📈 Gráfico de Métricas Q3.png", "Análisis cuantitativo de metas")
                            ).forEach { (id, title, desc) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.attachSampleImage(id)
                                            showAttachMenu = false
                                        }
                                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(desc, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showAttachMenu = false },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Aceptar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    pendingActions: List<PendingAction>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    val isUser = message.sender == "user"
    val isSystem = message.sender == "system"

    if (isSystem) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = message.text,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (isUser) 0.85f else 0.9f)
                    .wrapContentWidth(if (isUser) Alignment.End else Alignment.Start)
            ) {
                Card(
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 16.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isUser) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (!isUser) {
                            Row(
                                modifier = Modifier.padding(bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Riso Agent",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Text(
                            text = message.text,
                            fontSize = 14.sp,
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            lineHeight = 19.sp,
                        )

                        // If message is linked to a planning Mode Pending Action
                        if (message.pendingActionId != null) {
                            val action = pendingActions.find { it.id == message.pendingActionId }
                            if (action != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                ActionPlanCard(
                                    action = action,
                                    onApprove = { onApprove(action.id) },
                                    onReject = { onReject(action.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionPlanCard(
    action: PendingAction,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Lock",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Acción Automatizada",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                Badge(
                    containerColor = when (action.status) {
                        "PENDING" -> Color(0xFFD97706)
                        "APPROVED" -> Color(0xFF10B981)
                        else -> Color(0xFFEF4444)
                    }
                ) {
                    Text(
                        text = when (action.status) {
                            "PENDING" -> "Pendiente"
                            "APPROVED" -> "Aprobado"
                            else -> "Rechazado"
                        },
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = action.details,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Metodo: ${action.functionName}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            if (action.status == "PENDING") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("action_reject_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        border = BorderStroke(1.dp, Color(0xFFEF4444))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Desc", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rechazar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onApprove,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("action_approve_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Appr", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Aprobar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LlmStreamingLoader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F35)),
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Riso analizando...",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// --- TAB 2: Inbox Dashboard Client Screen ---
@Composable
fun RisoInboxScreen(viewModel: RisoViewModel) {
    val inbox by viewModel.liveInbox.collectAsStateWithLifecycle()
    val isInboxLoading by viewModel.isInboxLoading.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var emailDropdownFilter by remember { mutableStateOf("all") } // "all" | "unread" | "read"
    var selectedEmailDetail by remember { mutableStateOf<RisoEmail?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshLiveInbox()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mi Bandeja",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row {
                        IconButton(
                            onClick = { viewModel.refreshLiveInbox() },
                            modifier = Modifier.testTag("inbox_refresh_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Mail Search
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar en bandeja...", fontSize = 13.sp) },
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "SearchIcon") },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inbox_search_field"),
                    singleLine = true
                )
            }
        }

        if (isInboxLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val filteredInbox = inbox.filter {
                (searchQuery.isBlank() ||
                        it.subject.contains(searchQuery, ignoreCase = true) ||
                        it.sender.contains(searchQuery, ignoreCase = true) ||
                        it.body.contains(searchQuery, ignoreCase = true))
            }

            if (filteredInbox.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = "EmptyInbox",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No se encontraron correos",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Inserta cuentas o prueba con el emulador local",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredInbox) { email ->
                        EmailItemCard(
                            email = email,
                            onClick = { selectedEmailDetail = email }
                        )
                    }
                }
            }
        }
    }

    // Email content detail dialog overlay
    if (selectedEmailDetail != null) {
        EmailDetailDialog(
            email = selectedEmailDetail!!,
            onDismiss = { selectedEmailDetail = null },
            viewModel = viewModel
        )
    }
}

@Composable
fun EmailItemCard(
    email: RisoEmail,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                1.dp,
                if (!email.isRead) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (!email.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Glowing state marker
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (!email.isRead) MaterialTheme.colorScheme.primary else Color.Transparent)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = email.sender,
                        fontSize = 13.sp,
                        fontWeight = if (!email.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = email.date,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = email.subject,
                    fontSize = 14.sp,
                    fontWeight = if (!email.isRead) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = email.snippet,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EmailDetailDialog(
    email: RisoEmail,
    onDismiss: () -> Unit,
    viewModel: RisoViewModel
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Sender Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = email.sender.firstOrNull()?.uppercase() ?: "R",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(email.sender, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Para: tu correo", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(12.dp))

                // Subject Title
                Text(email.subject, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(email.date, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Email body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text(
                                text = email.body,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Tray: Delete option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.deleteSession(email.id) // simulates removing
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Eliminar", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// --- TAB 3: Configuration & Accounts Dashboard Screen ---
@Composable
fun RisoSettingsScreen(viewModel: RisoViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isTesting by viewModel.isTestingConnection.collectAsStateWithLifecycle()
    val testResult by viewModel.connectionTestResult.collectAsStateWithLifecycle()

    val emailAccounts by viewModel.emailAccounts.collectAsStateWithLifecycle()
    val activeEmailAccountId by viewModel.activeEmailAccountId.collectAsStateWithLifecycle()
    
    val whisperStatus by viewModel.localWhisperStatus.collectAsStateWithLifecycle()
    val whisperProgress by viewModel.localWhisperProgress.collectAsStateWithLifecycle()

    val isEn = settings["language"] == "en"
    fun t(key: String): String = L10n.t(key, isEn)

    var geminiKey by remember { mutableStateOf("") }
    var openaiKey by remember { mutableStateOf("") }
    var claudeKey by remember { mutableStateOf("") }
    var whisperKey by remember { mutableStateOf("") }

    // New email account addition fields temp state
    var newEmailAddress by remember { mutableStateOf("") }
    var newEmailPass by remember { mutableStateOf("") }
    var newImapHost by remember { mutableStateOf("") }
    var newSmtpHost by remember { mutableStateOf("") }

    var keyMasked by remember { mutableStateOf(true) }

    // Init inputs from database values
    LaunchedEffect(settings) {
        if (settings.isNotEmpty()) {
            geminiKey = settings["gemini_api_key"] ?: ""
            openaiKey = settings["openai_api_key"] ?: ""
            claudeKey = settings["claude_api_key"] ?: ""
            whisperKey = settings["whisper_api_key"] ?: ""
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App settings header
        item {
            Column {
                Text(
                    text = t("settings_title_bold"),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = t("settings_sub"),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Section: Language Selection Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = t("language_settings"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = t("language_settings_sub"),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Spanish option
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isEn) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                                .clickable { viewModel.updateSetting("language", "es") }
                                .padding(8.dp)
                        ) {
                            RadioButton(
                                selected = !isEn,
                                onClick = { viewModel.updateSetting("language", "es") },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Español 🇪🇸", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // English option
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isEn) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                                .clickable { viewModel.updateSetting("language", "en") }
                                .padding(8.dp)
                        ) {
                            RadioButton(
                                selected = isEn,
                                onClick = { viewModel.updateSetting("language", "en") },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("English 🇬🇧", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Section 1: LLM APIs Config Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = t("apis_config_header"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Gemini API Key entry
                    OutlinedTextField(
                        value = geminiKey,
                        onValueChange = {
                            geminiKey = it
                            viewModel.updateSetting("gemini_api_key", it)
                        },
                        label = { Text("Google Gemini API Key") },
                        placeholder = { Text(t("enter_api_key")) },
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "LockKey") },
                        trailingIcon = {
                            IconButton(onClick = { keyMasked = !keyMasked }) {
                                Icon(
                                    imageVector = if (keyMasked) Icons.Default.Warning else Icons.Default.Info,
                                    contentDescription = "MaskKey"
                                )
                            }
                        },
                        visualTransformation = if (keyMasked) PasswordVisualTransformation() else VisualTransformation.None,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gemini_key_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // OpenAI API Key
                    OutlinedTextField(
                        value = openaiKey,
                        onValueChange = {
                            openaiKey = it
                            viewModel.updateSetting("openai_api_key", it)
                        },
                        label = { Text(t("openai_api_key_label")) },
                        placeholder = { Text("sk-...") },
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("openai_key_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Claude key
                    OutlinedTextField(
                        value = claudeKey,
                        onValueChange = {
                            claudeKey = it
                            viewModel.updateSetting("claude_api_key", it)
                        },
                        label = { Text(t("claude_api_key_label")) },
                        placeholder = { Text("sk-ant-...") },
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("claude_key_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Whisper API Key
                    OutlinedTextField(
                        value = whisperKey,
                        onValueChange = {
                            whisperKey = it
                            viewModel.updateSetting("whisper_api_key", it)
                        },
                        label = { Text(t("whisper_api_key_label")) },
                        placeholder = { Text("sk-...") },
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("whisper_key_input"),
                        singleLine = true
                    )
                }
            }
        }

        // SECTION: MULTI-MODEL PROFILE MANAGEMENT
        item {
            val llmProfiles by viewModel.llmProfiles.collectAsStateWithLifecycle()
            val activeLlmProfileId by viewModel.activeLlmProfileId.collectAsStateWithLifecycle()

            var newProfileName by remember { mutableStateOf("") }
            var newProfileProvider by remember { mutableStateOf("Gemini") }
            var newProfileKey by remember { mutableStateOf("") }
            var showAddNewForm by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Múltiples Modelos y API Keys (OpenAI, Anthropic, Gemini, etc.)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Agrega y administra múltiples claves API para cada proveedor, y elige cuál quieres usar en cada sesión.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // List existing profiles
                    if (llmProfiles.isEmpty()) {
                        Text(
                            text = "No hay perfiles de modelos adicionales. Usa el formulario de abajo para agregar uno.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        llmProfiles.forEach { profile ->
                            val isActive = profile.id == activeLlmProfileId
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { viewModel.selectActiveLlmProfile(profile.id) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) 
                                                     else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                ),
                                border = if (isActive) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        RadioButton(
                                            selected = isActive,
                                            onClick = { viewModel.selectActiveLlmProfile(profile.id) },
                                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = profile.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = profile.provider,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                    modifier = Modifier
                                                        .background(
                                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (profile.apiKey.length > 8) "...${profile.apiKey.takeLast(6)}" else "***",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                                )
                                            }
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeLlmProfile(profile.id) }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Borrar modelo",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(10.dp))

                    if (!showAddNewForm) {
                        Button(
                            onClick = { showAddNewForm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("show_add_model_form")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Suma", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Agregar Modelo / API Key", fontSize = 12.sp)
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text("Nuevo Perfil de Modelo", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)

                            OutlinedTextField(
                                value = newProfileName,
                                onValueChange = { newProfileName = it },
                                label = { Text("Nombre descriptivo (ej: OpenAI Trabajo)") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("new_model_name_input"),
                                singleLine = true
                            )

                            // Provider Selector Row
                            Text("Proveedor:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Gemini", "OpenAI", "Claude").forEach { prov ->
                                    val isSelected = newProfileProvider == prov
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) 
                                                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                        modifier = Modifier
                                            .weight(1.0f)
                                            .clickable { newProfileProvider = prov }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(prov, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = newProfileKey,
                                onValueChange = { newProfileKey = it },
                                label = { Text("API Key") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("new_model_key_input"),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showAddNewForm = false },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancelar", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        if (newProfileName.isNotBlank() && newProfileKey.isNotBlank()) {
                                            viewModel.addLlmProfile(newProfileName, newProfileProvider, newProfileKey)
                                            newProfileName = ""
                                            newProfileKey = ""
                                            showAddNewForm = false
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("save_model_button")
                                ) {
                                    Text("Guardar", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Whisper Local Offline Control
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = t("local_whisper_header"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = t("local_whisper_sub"),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = t("download_status"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = when (whisperStatus) {
                                    "Ready" -> t("download_ready")
                                    "Downloading" -> t("download_progress_text")
                                    else -> t("download_not_started")
                                },
                                fontSize = 11.sp,
                                color = if (whisperStatus == "Ready") Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { viewModel.downloadLocalWhisper() },
                            enabled = whisperStatus != "Ready" && whisperStatus != "Downloading",
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (whisperStatus == "Downloading") t("downloading_btn") else t("download_btn"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (whisperStatus == "Downloading") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = { whisperProgress / 100f },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$whisperProgress%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }



        // Section 4: Connection tester
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Probar Cuenta de Correo Activa",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Button(
                            onClick = { viewModel.testEmailAuth() },
                            modifier = Modifier.testTag("settings_test_connection_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Probar Conexión", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (testResult != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = testResult!!,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (testResult!!.startsWith("¡Conexión IMAP Exitosa")) Color(0xFF10B981) else MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}

// Manual extension/utility to scale dynamic switches or components smoothly
fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout((placeable.width * scale).toInt(), (placeable.height * scale).toInt()) {
            placeable.placeRelativeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
            }
        }
    }
)

@Composable
fun RisoLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Helper function to draw symmetric curvy petals starting from a common bottom joint
        fun drawPetal(
            c1X: Float, c1Y: Float,
            c2X: Float, c2Y: Float,
            endX: Float, endY: Float,
            c3X: Float, c3Y: Float,
            c4X: Float, c4Y: Float,
            color: Color
        ) {
            val path = Path().apply {
                moveTo(w * 0.5f, h * (78f / 108f))
                cubicTo(w * c1X, h * c1Y, w * c2X, h * c2Y, w * endX, h * endY)
                cubicTo(w * c3X, h * c3Y, w * c4X, h * c4Y, w * 0.5f, h * (78f / 108f))
                close()
            }
            drawPath(path = path, color = color)
        }

        // Draw individual lotus layers from back to front
        
        // 1 & 2: Base Leaf Support / Sepals
        drawPetal(46/108f, 84/108f, 38/108f, 82/108f, 32/108f, 76/108f, 40/108f, 74/108f, 48/108f, 76/108f, Color(0xFF052E21))
        drawPetal(62/108f, 84/108f, 70/108f, 82/108f, 76/108f, 76/108f, 68/108f, 74/108f, 60/108f, 76/108f, Color(0xFF052E21))

        // 3 & 4: Deep Outer Petals
        drawPetal(20/108f, 78/108f, 14/108f, 62/108f, 24/108f, 50/108f, 30/108f, 53/108f, 38/108f, 60/108f, Color(0xFF065F46))
        drawPetal(88/108f, 78/108f, 94/108f, 62/108f, 84/108f, 50/108f, 78/108f, 53/108f, 70/108f, 60/108f, Color(0xFF047857))

        // 5 & 6: Vibrant Inner Petals
        drawPetal(34/108f, 73/108f, 28/108f, 53/108f, 38/108f, 40/108f, 43/108f, 46/108f, 48/108f, 56/108f, Color(0xFF10B981))
        drawPetal(74/108f, 73/108f, 80/108f, 53/108f, 70/108f, 40/108f, 65/108f, 46/108f, 60/108f, 56/108f, Color(0xFF059669))

        // 7: Central Rising Petal (On top, luminous mint highlight)
        drawPetal(44/108f, 63/108f, 44/108f, 43/108f, 54/108f, 30/108f, 64/108f, 43/108f, 64/108f, 63/108f, Color(0xFFA7F3D0))

        // 8: Center Spark Highlight at the core
        val sparkPath = Path().apply {
            moveTo(w * (54f / 108f), h * (42f / 108f))
            lineTo(w * (55f / 108f), h * (44f / 108f))
            lineTo(w * (57f / 108f), h * (44.5f / 108f))
            lineTo(w * (55f / 108f), h * (45f / 108f))
            lineTo(w * (54f / 108f), h * (47f / 108f))
            lineTo(w * (53f / 108f), h * (45f / 108f))
            lineTo(w * (51f / 108f), h * (44.5f / 108f))
            lineTo(w * (53f / 108f), h * (44f / 108f))
            close()
        }
        drawPath(path = sparkPath, color = Color.White)
    }
}
