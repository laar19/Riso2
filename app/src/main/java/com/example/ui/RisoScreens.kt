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
import com.example.data.model.GithubAccount
import com.example.data.model.GitlabAccount
import com.example.service.email.EmailAccount
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
                    
                    if (activeAccount != null && activeAccount.emailAddress.isNotBlank() && !activeAccount.emailAddress.contains("riso.local", ignoreCase = true)) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = t("active_account") + activeAccount.emailAddress,
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
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
                                fontSize = 15.sp
                            )
                            if (currentTab == "chat" && activeAccount != null && activeAccount.emailAddress.isNotBlank() && !activeAccount.emailAddress.contains("riso.local", ignoreCase = true)) {
                                Text(
                                    text = activeAccount.emailAddress,
                                    fontSize = 10.sp,
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RisoChatScreen(viewModel: RisoViewModel) {
    val messages by viewModel.currentMessages.collectAsStateWithLifecycle()
    val isLlmLoading by viewModel.isLlmLoading.collectAsStateWithLifecycle()
    val planningMode by viewModel.planningMode.collectAsStateWithLifecycle()
    val pendingActions by viewModel.pendingActions.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val emailAccounts by viewModel.emailAccounts.collectAsStateWithLifecycle()
    val activeEmailAccountId by viewModel.activeEmailAccountId.collectAsStateWithLifecycle()
    val githubAccounts by viewModel.githubAccounts.collectAsStateWithLifecycle()
    val gitlabAccounts by viewModel.gitlabAccounts.collectAsStateWithLifecycle()

    var mcpTabSelection by remember { mutableIntStateOf(0) } // 0: Correo, 1: GitHub & GitLab, 2: Herramientas & Adjuntos
    var gitSubTabSelection by remember { mutableIntStateOf(0) } // 0: GitHub, 1: GitLab

    var showAddEmailForm by remember { mutableStateOf(false) }
    var emailInputAddress by remember { mutableStateOf("") }
    var emailInputPassword by remember { mutableStateOf("") }
    var emailInputImapServer by remember { mutableStateOf("imap.gmail.com") }
    var emailInputImapPort by remember { mutableStateOf("993") }
    var emailInputSmtpServer by remember { mutableStateOf("smtp.gmail.com") }
    var emailInputSmtpPort by remember { mutableStateOf("587") }

    var showAddGithubForm by remember { mutableStateOf(false) }
    var githubInputUsername by remember { mutableStateOf("") }
    var githubInputToken by remember { mutableStateOf("") }
    var githubInputLabel by remember { mutableStateOf("") }

    var showAddGitlabForm by remember { mutableStateOf(false) }
    var gitlabInputUrl by remember { mutableStateOf("https://gitlab.com") }
    var gitlabInputUsername by remember { mutableStateOf("") }
    var gitlabInputToken by remember { mutableStateOf("") }
    var gitlabInputLabel by remember { mutableStateOf("") }

    var emailTestResults by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var githubTestResults by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var gitlabTestResults by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

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

        // Model selector Bottom Sheet displaying configured LLM profiles (sliding from bottom)
        if (showModelSelector) {
            val llmProfiles by viewModel.llmProfiles.collectAsStateWithLifecycle()
            val activeLlmProfileId by viewModel.activeLlmProfileId.collectAsStateWithLifecycle()
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { showModelSelector = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .padding(bottom = 28.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Modelos LLM",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Selecciona el modelo activo para el chat",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        IconButton(
                            onClick = { showModelSelector = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(12.dp))

                    if (llmProfiles.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("⚠️", fontSize = 24.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "No tienes ningún modelo configurado",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "No hay ningún modelo predeterminado. Puedes añadir tus proveedores y claves de API en la pestaña de Ajustes.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 340.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(llmProfiles) { profile ->
                                val isSel = profile.id == activeLlmProfileId
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.selectActiveLlmProfile(profile.id)
                                            showModelSelector = false
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                         else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    ),
                                    border = if (isSel) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                             else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = profile.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(top = 3.dp)
                                            ) {
                                                Text(
                                                    text = profile.provider,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                                    modifier = Modifier
                                                        .background(
                                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                                if (profile.modelName.isNotBlank()) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = profile.modelName,
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                                    )
                                                }
                                            }
                                        }
                                        RadioButton(
                                            selected = isSel,
                                            onClick = {
                                                viewModel.selectActiveLlmProfile(profile.id)
                                                showModelSelector = false
                                            },
                                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = { showModelSelector = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cerrar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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

        // Row right above the chat input box: LLM Model selector on the left, Planning mode switch on the right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val llmProfiles by viewModel.llmProfiles.collectAsStateWithLifecycle()
            val activeLlmId by viewModel.activeLlmProfileId.collectAsStateWithLifecycle()
            val activeProf = llmProfiles.find { it.id == activeLlmId }
            val activeDisplayName = activeProf?.name

            // Select active LLM directly from chat box (opens sliding bottom sheet)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (activeProf != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                    )
                    .border(
                        1.dp,
                        if (activeProf != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { showModelSelector = true }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("chat_box_llm_selector")
            ) {
                Text(
                    text = if (activeDisplayName != null) "🤖 $activeDisplayName ▾" else "⚠️ Seleccionar Modelo ▾",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (activeProf != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }

            // Planning Mode / Execution Mode toggle with dynamic radio position and color
            // Planning Mode Active: Green color, radio button on the LEFT
            // Execution Mode Active: Red color, radio button on the RIGHT
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (planningMode) Color(0xFF10B981).copy(alpha = 0.12f)
                        else Color(0xFFEF4444).copy(alpha = 0.12f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (planningMode) Color(0xFF10B981).copy(alpha = 0.6f)
                                else Color(0xFFEF4444).copy(alpha = 0.6f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { viewModel.togglePlanningMode() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("toggle_planning_mode_chat"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (planningMode) {
                    // Planning Mode: GREEN, Radio button on the LEFT
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Planificación",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857)
                    )
                } else {
                    // Execution Mode: RED, Radio button on the RIGHT
                    Text(
                        text = "Ejecución",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB91C1C)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    )
                }
            }
        }

        // Bottom Input Row - clean 4-element layout matching modern compact distribution
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 3.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // MCP & Attachments button
            IconButton(
                onClick = { showAttachMenu = true },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .testTag("mcp_attachments_plus_button")
            ) {
                Text("➕", fontSize = 15.sp)
            }

            // Chat input text field
            val canSend = textInput.isNotBlank() || attachedImage != null
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text(t("chat_input_placeholder"), fontSize = 13.sp) },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                maxLines = 3,
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_text_field"),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (canSend) {
                        viewModel.sendMessage(textInput)
                        textInput = ""
                        keyboardController?.hide()
                    }
                })
            )

            // STT Microphone button
            IconButton(
                onClick = {
                    viewModel.triggerMicrophoneTranscription { transcription ->
                        textInput = transcription
                    }
                },
                enabled = !isRecordingAudio,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRecordingAudio) MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .testTag("chat_stt_microphone")
            ) {
                Text(
                    text = "🎙️",
                    fontSize = 15.sp,
                    color = if (isRecordingAudio) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Send button
            IconButton(
                onClick = {
                    if (canSend) {
                        viewModel.sendMessage(textInput)
                        textInput = ""
                        keyboardController?.hide()
                    }
                },
                enabled = canSend,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                    .testTag("chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }

    // Unified MCP Connections + Attachments Dialog
    if (showAttachMenu) {
        McpConnectionsDialog(
            viewModel = viewModel,
            emailAccounts = emailAccounts,
            githubAccounts = githubAccounts,
            gitlabAccounts = gitlabAccounts,
            settings = settings,
            onLaunchCamera = {
                try {
                    cameraLauncher.launch(null)
                } catch (e: Exception) {
                    viewModel.attachCustomFile("Cámara: foto_capturada.png")
                }
            },
            onLaunchGallery = {
                try {
                    galleryLauncher.launch("image/*")
                } catch (e: Exception) {
                    viewModel.attachCustomFile("Galería: imagen.png")
                }
            },
            onLaunchFilePicker = {
                try {
                    filePickerLauncher.launch("*/*")
                } catch (e: Exception) {
                    viewModel.attachCustomFile("Archivo: documento.pdf")
                }
            },
            onDismiss = { showAttachMenu = false }
        )
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
                    .fillMaxWidth()
                    .wrapContentWidth(if (isUser) Alignment.End else Alignment.Start)
            ) {
                Card(
                    shape = RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isUser) 14.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 14.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    border = if (isUser) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .widthIn(min = 36.dp, max = 295.dp)
                        .padding(vertical = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)) {
                        if (!isUser) {
                            Row(
                                modifier = Modifier.padding(bottom = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Riso Agent",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Text(
                            text = message.text,
                            fontSize = 13.sp,
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            lineHeight = 17.5.sp,
                        )

                        // If message is linked to a planning Mode Pending Action
                        if (message.pendingActionId != null) {
                            val action = pendingActions.find { it.id == message.pendingActionId }
                            if (action != null) {
                                Spacer(modifier = Modifier.height(8.dp))
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
    var braveKey by remember { mutableStateOf("") }

    // LLM Profiles & Testing states
    var llmTestStatuses by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var testingLlmId by remember { mutableStateOf<String?>(null) }
    var showAddLlmForm by remember { mutableStateOf(false) }
    var newLlmName by remember { mutableStateOf("") }
    var newLlmProvider by remember { mutableStateOf("Google") }
    var newLlmEndpoint by remember { mutableStateOf("https://generativelanguage.googleapis.com") }
    var newLlmModel by remember { mutableStateOf("gemini-3.5-flash") }
    var newLlmKey by remember { mutableStateOf("") }
    var newLlmTestMsg by remember { mutableStateOf<String?>(null) }
    var isTestingNewLlm by remember { mutableStateOf(false) }

    // STT Profiles & Testing states
    var sttTestStatuses by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var testingSttId by remember { mutableStateOf<String?>(null) }
    var showAddSttForm by remember { mutableStateOf(false) }
    var newSttName by remember { mutableStateOf("") }
    var newSttIsLocal by remember { mutableStateOf(false) }
    var newSttEndpoint by remember { mutableStateOf("https://api.openai.com/v1/audio/transcriptions") }
    var newSttModel by remember { mutableStateOf("whisper-1") }
    var newSttKey by remember { mutableStateOf("") }
    var newSttTestMsg by remember { mutableStateOf<String?>(null) }
    var isTestingNewStt by remember { mutableStateOf(false) }

    // New email account addition fields temp state
    var newEmailAddress by remember { mutableStateOf("") }
    var newEmailPass by remember { mutableStateOf("") }
    var newImapHost by remember { mutableStateOf("") }
    var newSmtpHost by remember { mutableStateOf("") }

    var keyMasked by remember { mutableStateOf(true) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showDeleteDataDialog by remember { mutableStateOf(false) }

    // Init inputs from database values
    LaunchedEffect(settings) {
        if (settings.isNotEmpty()) {
            braveKey = settings["brave_search_api_key"] ?: ""
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
            Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                Text(
                    text = t("settings_title_bold"),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = t("settings_sub"),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Section: Language Selection Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = t("language_settings"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = t("language_settings_sub"),
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.updateSetting("language", "es") },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isEn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                contentColor = if (!isEn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            border = if (!isEn) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Text("🇪🇸 Español", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { viewModel.updateSetting("language", "en") },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                contentColor = if (isEn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            border = if (isEn) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Text("🇬🇧 English", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Section: Theme Selection Card
        item {
            val themeMode = settings["theme_mode"] ?: "light"
            val isDark = themeMode == "dark"

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Tema Visual",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Selecciona el aspecto visual de la interfaz",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.updateSetting("theme_mode", "light") },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                contentColor = if (!isDark) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            border = if (!isDark) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Text("☀️ Modo Claro", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { viewModel.updateSetting("theme_mode", "dark") },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                contentColor = if (isDark) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            border = if (isDark) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Text("🌙 Modo Oscuro", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // APARTADO 1: Configuración de APIs LLM (Modelos de Lenguaje)
        item {
            val llmProfiles by viewModel.llmProfiles.collectAsStateWithLifecycle()
            val activeLlmProfileId by viewModel.activeLlmProfileId.collectAsStateWithLifecycle()

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Modelos LLM (APIs)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Configura proveedores (Google, Anthropic, OpenAI o compatible), endpoint y modelo. Elige cuál usar por defecto.",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // List of existing LLM profiles
                    if (llmProfiles.isEmpty()) {
                        Text(
                            text = "No hay modelos configurados.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        llmProfiles.forEach { profile ->
                            val isActive = profile.id == activeLlmProfileId
                            val testMsg = llmTestStatuses[profile.id]
                            val isTestingThis = testingLlmId == profile.id

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                                     else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Row 1: Radio + Name + Active Badge on Left, Actions on Right
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
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
                                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = profile.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isActive) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = Color(0xFF10B981).copy(alpha = 0.14f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "Activo",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF059669),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    testingLlmId = profile.id
                                                    viewModel.testLlmConnection(profile.provider, profile.apiEndpoint, profile.apiKey, profile.modelName) { ok, msg ->
                                                        llmTestStatuses = llmTestStatuses + (profile.id to if (ok) "✓ Conexión OK" else "✕ $msg")
                                                        testingLlmId = null
                                                    }
                                                },
                                                modifier = Modifier
                                                    .height(28.dp)
                                                    .testTag("test_llm_btn_${profile.id}"),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.primary
                                                )
                                            ) {
                                                if (isTestingThis) {
                                                    CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp, color = MaterialTheme.colorScheme.primary)
                                                } else {
                                                    Text("Probar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            IconButton(
                                                onClick = { viewModel.removeLlmProfile(profile.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Borrar",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.65f),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 7.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                                    )

                                    // Row 2: Symmetric Chips for Provider & Model
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = profile.provider,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Surface(
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = profile.modelName.ifBlank { "default" },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // Row 3: API Endpoint (if configured)
                                    if (profile.apiEndpoint.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "🔗 ${profile.apiEndpoint}",
                                            fontSize = 9.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                        )
                                    }

                                    // Row 4: Status result strip
                                    if (testMsg != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        val isOk = testMsg.startsWith("✓")
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = if (isOk) Color(0xFF10B981).copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(0.5.dp, if (isOk) Color(0xFF10B981).copy(alpha = 0.3f) else MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (isOk) "✓" else "✕",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isOk) Color(0xFF059669) else MaterialTheme.colorScheme.error
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = testMsg,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isOk) Color(0xFF059669) else MaterialTheme.colorScheme.error,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!showAddLlmForm) {
                        OutlinedButton(
                            onClick = { showAddLlmForm = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .testTag("show_add_llm_form_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Agregar Modelo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Text("Nuevo Modelo LLM", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)

                            OutlinedTextField(
                                value = newLlmName,
                                onValueChange = { newLlmName = it },
                                label = { Text("Nombre (ej: Gemini 3.5 Flash)", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("new_llm_name_input"),
                                singleLine = true
                            )

                            // Endpoint Provider Selector
                            Text("Tipo de Endpoint:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    "Google" to ("https://generativelanguage.googleapis.com" to "gemini-3.5-flash"),
                                    "Anthropic" to ("https://api.anthropic.com/v1" to "claude-3-5-sonnet-20240620"),
                                    "OpenAI" to ("https://api.openai.com/v1" to "gpt-4o-mini"),
                                    "Compatible" to ("https://api.openai.com/v1" to "")
                                ).forEach { (prov, defaults) ->
                                    val isSel = newLlmProvider == prov
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ),
                                        border = if (isSel) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                newLlmProvider = prov
                                                newLlmEndpoint = defaults.first
                                                if (defaults.second.isNotBlank()) newLlmModel = defaults.second
                                            },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(prov, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = newLlmEndpoint,
                                onValueChange = { newLlmEndpoint = it },
                                label = { Text("API Endpoint", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("new_llm_endpoint_input"),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = newLlmModel,
                                onValueChange = { newLlmModel = it },
                                label = { Text("Nombre del Modelo (ej: gemini-1.5-flash, gpt-4o)", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("new_llm_model_input"),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = newLlmKey,
                                onValueChange = { newLlmKey = it },
                                label = { Text("API Key", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("new_llm_key_input"),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation()
                            )

                            if (newLlmTestMsg != null) {
                                Text(
                                    text = newLlmTestMsg!!,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (newLlmTestMsg!!.startsWith("✓")) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (newLlmKey.isNotBlank()) {
                                            isTestingNewLlm = true
                                            viewModel.testLlmConnection(newLlmProvider, newLlmEndpoint, newLlmKey, newLlmModel) { ok, msg ->
                                                newLlmTestMsg = if (ok) "✓ Conexión OK" else "✕ $msg"
                                                isTestingNewLlm = false
                                            }
                                        } else {
                                            newLlmTestMsg = "✕ Ingresa la API Key primero"
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .testTag("test_new_llm_btn"),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                        contentColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    if (isTestingNewLlm) {
                                        CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp, color = MaterialTheme.colorScheme.secondary)
                                    } else {
                                        Text("Probar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        showAddLlmForm = false
                                        newLlmTestMsg = null
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                ) {
                                    Text("Cancelar", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        if (newLlmName.isNotBlank() && newLlmKey.isNotBlank()) {
                                            viewModel.addLlmProfile(
                                                name = newLlmName,
                                                provider = newLlmProvider,
                                                apiKey = newLlmKey,
                                                apiEndpoint = newLlmEndpoint,
                                                modelName = newLlmModel
                                            )
                                            newLlmName = ""
                                            newLlmKey = ""
                                            newLlmTestMsg = null
                                            showAddLlmForm = false
                                        }
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .testTag("save_new_llm_btn")
                                ) {
                                    Text("Guardar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // APARTADO 2: Configuración de Speech to Text (STT - Remoto y Local)
        item {
            val sttProfiles by viewModel.sttProfiles.collectAsStateWithLifecycle()
            val activeSttProfileId by viewModel.activeSttProfileId.collectAsStateWithLifecycle()

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Reconocimiento de Voz (STT)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Configura Whisper API Remoto o Whisper Local sin conexión. Elige cuál usar por defecto.",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // List of existing STT profiles
                    if (sttProfiles.isEmpty()) {
                        Text(
                            text = "No hay perfiles STT configurados.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        sttProfiles.forEach { profile ->
                            val isActive = profile.id == activeSttProfileId
                            val testMsg = sttTestStatuses[profile.id]
                            val isTestingThis = testingSttId == profile.id

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isActive) MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
                                                     else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isActive) MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            RadioButton(
                                                selected = isActive,
                                                onClick = { viewModel.selectActiveSttProfile(profile.id) },
                                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.secondary),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = profile.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = if (isActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isActive) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = Color(0xFF10B981).copy(alpha = 0.14f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "Activo",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF059669),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (profile.isLocal && whisperStatus != "Ready") {
                                                Button(
                                                    onClick = { viewModel.downloadLocalWhisper() },
                                                    enabled = whisperStatus != "Downloading",
                                                    modifier = Modifier.height(28.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                    shape = RoundedCornerShape(6.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.tertiary
                                                    )
                                                ) {
                                                    Text(
                                                        text = if (whisperStatus == "Downloading") "${whisperProgress}%" else "Descargar",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    testingSttId = profile.id
                                                    viewModel.testSttConnection(profile.isLocal, profile.apiEndpoint, profile.apiKey, profile.modelName) { ok, msg ->
                                                        sttTestStatuses = sttTestStatuses + (profile.id to if (ok) "✓ Conexión OK" else "✕ $msg")
                                                        testingSttId = null
                                                    }
                                                },
                                                modifier = Modifier
                                                    .height(28.dp)
                                                    .testTag("test_stt_btn_${profile.id}"),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.secondary
                                                )
                                            ) {
                                                if (isTestingThis) {
                                                    CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp, color = MaterialTheme.colorScheme.secondary)
                                                } else {
                                                    Text("Probar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            IconButton(
                                                onClick = { viewModel.removeSttProfile(profile.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Borrar",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.65f),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 7.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = if (profile.isLocal) "Local (Offline)" else "Remoto (API)",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Surface(
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = profile.modelName.ifBlank { "default" },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    if (!profile.isLocal && profile.apiEndpoint.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "🔗 ${profile.apiEndpoint}",
                                            fontSize = 9.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                        )
                                    }

                                    if (testMsg != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        val isOk = testMsg.startsWith("✓")
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = if (isOk) Color(0xFF10B981).copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(0.5.dp, if (isOk) Color(0xFF10B981).copy(alpha = 0.3f) else MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (isOk) "✓" else "✕",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isOk) Color(0xFF059669) else MaterialTheme.colorScheme.error
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = testMsg,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isOk) Color(0xFF059669) else MaterialTheme.colorScheme.error,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!showAddSttForm) {
                        OutlinedButton(
                            onClick = { showAddSttForm = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .testTag("show_add_stt_form_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Agregar STT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Text("Nuevo Perfil STT", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)

                            OutlinedTextField(
                                value = newSttName,
                                onValueChange = { newSttName = it },
                                label = { Text("Nombre (ej: Whisper Remoto)", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("new_stt_name_input"),
                                singleLine = true
                            )

                            // Type selector: Remoto vs Local
                            Text("Tipo de STT:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "Remoto (API)" to false,
                                    "Local (Offline)" to true
                                ).forEach { (label, isLoc) ->
                                    val isSel = newSttIsLocal == isLoc
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSel) MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                                                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ),
                                        border = if (isSel) BorderStroke(1.dp, MaterialTheme.colorScheme.secondary) else null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { newSttIsLocal = isLoc },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            if (!newSttIsLocal) {
                                OutlinedTextField(
                                    value = newSttEndpoint,
                                    onValueChange = { newSttEndpoint = it },
                                    label = { Text("API Endpoint", fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("new_stt_endpoint_input"),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = newSttModel,
                                    onValueChange = { newSttModel = it },
                                    label = { Text("Nombre del Modelo (ej: whisper-1)", fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("new_stt_model_input"),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = newSttKey,
                                    onValueChange = { newSttKey = it },
                                    label = { Text("API Key", fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("new_stt_key_input"),
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation()
                                )
                            } else {
                                OutlinedTextField(
                                    value = newSttModel,
                                    onValueChange = { newSttModel = it },
                                    label = { Text("Nombre del Modelo Local (ej: whisper-small-v3)", fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("new_stt_model_input"),
                                    singleLine = true
                                )
                            }

                            if (newSttTestMsg != null) {
                                Text(
                                    text = newSttTestMsg!!,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (newSttTestMsg!!.startsWith("✓")) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        isTestingNewStt = true
                                        viewModel.testSttConnection(newSttIsLocal, newSttEndpoint, newSttKey, newSttModel) { ok, msg ->
                                            newSttTestMsg = if (ok) "✓ Conexión OK" else "✕ $msg"
                                            isTestingNewStt = false
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .testTag("test_new_stt_btn"),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                        contentColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    if (isTestingNewStt) {
                                        CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp, color = MaterialTheme.colorScheme.secondary)
                                    } else {
                                        Text("Probar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        showAddSttForm = false
                                        newSttTestMsg = null
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                ) {
                                    Text("Cancelar", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        if (newSttName.isNotBlank() && (newSttIsLocal || newSttKey.isNotBlank())) {
                                            viewModel.addSttProfile(
                                                name = newSttName,
                                                isLocal = newSttIsLocal,
                                                apiEndpoint = newSttEndpoint,
                                                modelName = newSttModel,
                                                apiKey = newSttKey
                                            )
                                            newSttName = ""
                                            newSttKey = ""
                                            newSttTestMsg = null
                                            showAddSttForm = false
                                        }
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .testTag("save_new_stt_btn")
                                ) {
                                    Text("Guardar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // APARTADO 3: Búsqueda Web (Opcional - Brave Search)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Búsqueda Web (Opcional)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "API Key de Brave Search para consultar internet en tiempo real.",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = braveKey,
                        onValueChange = {
                            braveKey = it
                            viewModel.updateSetting("brave_search_api_key", it)
                        },
                        label = { Text("Brave Search API Key", fontSize = 11.sp) },
                        placeholder = { Text("BSA-...") },
                        shape = RoundedCornerShape(8.dp),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("brave_search_key_input"),
                        singleLine = true
                    )
                }
            }
        }

        // Section 4: Connection tester
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Probar Cuenta de Correo Activa",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Verifica autenticación IMAP/SMTP",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        Button(
                            onClick = { viewModel.testEmailAuth() },
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("settings_test_connection_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
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
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (testResult!!.startsWith("¡Conexión IMAP Exitosa")) Color(0xFF10B981) else MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }

        // Section 5: Transparency, Data Privacy & Account Deletion (Google Play Compliance)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🛡️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = t("privacy_policy_title"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = t("privacy_policy_sub"),
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPrivacyDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("btn_view_privacy_policy"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(t("privacy_btn"), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { showDeleteDataDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("btn_delete_all_data"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text(t("delete_all_data_btn"), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🛡️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isEn) "Riso Privacy Policy" else "Política de Privacidad de Riso", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 350.dp)) {
                    item {
                        Text(
                            text = if (isEn) {
                                """
                                Riso Privacy & Data Protection Commitment:
                                
                                1. Local-First Storage: All email credentials, IMAP/SMTP passwords, and session histories are stored locally on your device in a secure SQLite/Room database. Riso operates no external servers, cloud databases, or tracking telemetry.
                                
                                2. Direct API Integration: When sending queries to AI providers (Gemini, OpenAI, Claude), requests are transmitted directly and securely via HTTPS using the API keys you provide.
                                
                                3. No Unsolicited Data Collection: We do not sell, rent, track, or share your personal data, contacts, or emails with third parties or advertising networks.
                                
                                4. Complete Data Erasure (Right to be Forgotten): You have full control over your data. You can delete individual chats, accounts, or wipe all app data at any time via the button below.
                                
                                5. Google Play Compliance: This application complies with Google Play Developer Program policies regarding User Data and Privacy transparency.
                                """.trimIndent()
                            } else {
                                """
                                Compromiso de Privacidad y Protección de Datos de Riso:
                                
                                1. Almacenamiento 100% Local: Todas las credenciales de correo (IMAP/SMTP), contraseñas y el historial de conversaciones se guardan exclusivamente en la base de datos interna de tu dispositivo. Riso no tiene servidores intermedios, bases de datos en la nube ni telemetría de rastreo.
                                
                                2. Integración Directa con APIs: Las consultas enviadas a modelos de IA (Gemini, OpenAI, Claude) se efectúan de forma directa y cifrada vía HTTPS utilizando exclusivamente las claves API que tú configures.
                                
                                3. Cero Recopilación de Datos Personales: No vendemos, transferimos, rastreamos ni compartimos tus correos, contactos o datos personales con redes publicitarias ni terceros.
                                
                                4. Eliminación Total de Datos (Derecho al Olvido): Tienes control total. Puedes borrar sesiones individuales, cuentas de correo o eliminar permanentemente todos los datos de la app con un solo toque.
                                
                                5. Cumplimiento Google Play: Esta app cumple con las directrices y políticas de protección de datos de usuario de Google Play Store.
                                """.trimIndent()
                            },
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isEn) "Close" else "Entendido")
                }
            }
        )
    }

    // Confirm Delete All Data Dialog
    if (showDeleteDataDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDataDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚠️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isEn) "Delete All Data" else "Eliminar Todos los Datos", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    text = t("delete_all_data_confirm"),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllUserData {
                            showDeleteDataDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isEn) "Confirm & Wipe" else "Sí, Eliminar Todo", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDataDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isEn) "Cancel" else "Cancelar")
                }
            }
        )
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
