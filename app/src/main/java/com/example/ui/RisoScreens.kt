package com.example.ui

import com.example.BuildConfig
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessDark
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusErrorDark
import com.example.ui.theme.StatusWarning
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.PendingAction
import com.example.data.model.GithubAccount
import com.example.data.model.GitlabAccount
import com.example.service.email.EmailAccount
import com.example.service.email.RisoEmail
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.graphics.Bitmap
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.widget.Toast

fun safeCopyText(context: Context, label: String, text: String, toastMsg: String = "Copiado al portapapeles") {
    try {
        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clip != null) {
            clip.setPrimaryClip(ClipData.newPlainText(label, text))
            Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        android.util.Log.e("RisoScreens", "Error al copiar texto", e)
    }
}

fun safeShareText(context: Context, text: String, chooserTitle: String) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val chooserIntent = Intent.createChooser(sendIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    } catch (e: Exception) {
        android.util.Log.e("RisoScreens", "Error al compartir", e)
        Toast.makeText(context, "No se encontró una aplicación para compartir", Toast.LENGTH_SHORT).show()
    }
}

fun Modifier.simpleVerticalScrollbar(
    state: LazyListState,
    width: Dp = 4.dp
): Modifier = composed {
    val targetAlpha = if (state.isScrollInProgress) 1f else 0f
    val duration = if (state.isScrollInProgress) 150 else 500
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = duration),
        label = "scrollbar_alpha"
    )
    val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

    drawWithContent {
        drawContent()

        val firstVisibleElementIndex = state.layoutInfo.visibleItemsInfo.firstOrNull()?.index
        val needDrawScrollbar = state.isScrollInProgress || alpha > 0.0f

        if (needDrawScrollbar && firstVisibleElementIndex != null) {
            val elementHeights = state.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
            val totalElements = state.layoutInfo.totalItemsCount
            val totalHeight = totalElements * elementHeights
            val canvasHeight = size.height

            if (totalElements > 0 && totalHeight > canvasHeight && elementHeights > 0) {
                val scrollbarHeight = (canvasHeight * (canvasHeight / totalHeight)).coerceIn(36.dp.toPx(), canvasHeight)
                val scrollbarOffsetY = (firstVisibleElementIndex.toFloat() / totalElements.toFloat()) * canvasHeight

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(size.width - width.toPx() - 2.dp.toPx(), scrollbarOffsetY),
                    size = Size(width.toPx(), scrollbarHeight),
                    cornerRadius = CornerRadius(width.toPx() / 2, width.toPx() / 2),
                    alpha = alpha
                )
            }
        }
    }
}

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
    val context = LocalContext.current
    var renamingSession by remember { mutableStateOf<ChatSession?>(null) }
    var renamingTitleText by remember { mutableStateOf("") }
    
    if (renamingSession != null) {
        AlertDialog(
            onDismissRequest = { renamingSession = null },
            title = { Text("Renombrar chat", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = renamingTitleText,
                    onValueChange = { renamingTitleText = it },
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = renamingSession
                        if (target != null && renamingTitleText.isNotBlank()) {
                            viewModel.renameSession(target.id, renamingTitleText.trim())
                        }
                        renamingSession = null
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingSession = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
    
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
                        modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            RisoLogo(modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Riso",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = t("local_offline"),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // "+ Nuevo Chat" Button
                    FilledTonalButton(
                        onClick = {
                            viewModel.createNewSession()
                            currentTab = "chat"
                            coroutineScope.launch { drawerState.close() }
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .padding(vertical = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = t("new_chat"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Chronological groupings
                    val now = System.currentTimeMillis()
                    val oneDayMillis = 24 * 60 * 60 * 1000L
                    val startOfToday = now - (now % oneDayMillis)
                    val startOfYesterday = startOfToday - oneDayMillis
                    val startOfWeek = startOfToday - (6 * oneDayMillis)

                    val pinnedSessions = remember(sessions) { sessions.filter { it.isPinned }.sortedByDescending { it.createdAt } }
                    val unpinnedSessions = remember(sessions) { sessions.filter { !it.isPinned }.sortedByDescending { it.createdAt } }
                    val todaySessions = remember(unpinnedSessions) { unpinnedSessions.filter { it.createdAt >= startOfToday } }
                    val yesterdaySessions = remember(unpinnedSessions) { unpinnedSessions.filter { it.createdAt in startOfYesterday until startOfToday } }
                    val weekSessions = remember(unpinnedSessions) { unpinnedSessions.filter { it.createdAt in startOfWeek until startOfYesterday } }
                    val olderSessions = remember(unpinnedSessions) { unpinnedSessions.filter { it.createdAt < startOfWeek } }

                    // Scrollable list of sessions
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        @Composable
                        fun renderSessionItem(session: com.example.data.model.ChatSession) {
                            val isSelected = session.id == selectedSessionId && currentTab == "chat"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable {
                                        viewModel.selectSession(session.id)
                                        currentTab = "chat"
                                        coroutineScope.launch { drawerState.close() }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(20.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = if (session.isPinned) "📌" else "💬",
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = session.title ?: "Chat Riso",
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { viewModel.togglePinSession(session.id) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Text(if (session.isPinned) "📌" else "📍", fontSize = 13.sp)
                                    }
                                    IconButton(
                                        onClick = {
                                            renamingSession = session
                                            renamingTitleText = session.title ?: ""
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Renombrar",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (sessions.size > 1) {
                                        IconButton(
                                            onClick = { viewModel.deleteSession(session.id) },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Eliminar",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        @Composable
                        fun renderGroupHeader(title: String) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 10.dp, top = 10.dp, bottom = 4.dp)
                            )
                        }

                        if (pinnedSessions.isNotEmpty()) {
                            renderGroupHeader(if (isEn) "PINNED" else "FIJADOS")
                            pinnedSessions.forEach { renderSessionItem(it) }
                        }

                        if (todaySessions.isNotEmpty()) {
                            renderGroupHeader(if (isEn) "TODAY" else "HOY")
                            todaySessions.forEach { renderSessionItem(it) }
                        }

                        if (yesterdaySessions.isNotEmpty()) {
                            renderGroupHeader(if (isEn) "YESTERDAY" else "AYER")
                            yesterdaySessions.forEach { renderSessionItem(it) }
                        }

                        if (weekSessions.isNotEmpty()) {
                            renderGroupHeader(if (isEn) "LAST 7 DAYS" else "ÚLTIMOS 7 DÍAS")
                            weekSessions.forEach { renderSessionItem(it) }
                        }

                        if (olderSessions.isNotEmpty()) {
                            renderGroupHeader(if (isEn) "OLDER" else "ANTERIORES")
                            olderSessions.forEach { renderSessionItem(it) }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
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
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Theme mode toggle
                    val themeMode = settings["theme_mode"] ?: "light"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (themeMode == "light") t("theme_light") else t("theme_dark"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = themeMode == "light",
                            onCheckedChange = { isLight ->
                                viewModel.updateSetting("theme_mode", if (isLight) "light" else "dark")
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
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
                        IconButton(
                            onClick = { coroutineScope.launch { drawerState.open() } },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Hamburguer menu")
                        }
                    },
                    actions = {
                        if (currentTab == "chat") {
                            val curSession = sessions.find { it.id == selectedSessionId }
                            if (curSession != null) {
                                IconButton(
                                    onClick = { viewModel.togglePinSession(curSession.id) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Text(if (curSession.isPinned) "📌" else "📍", fontSize = 16.sp)
                                }
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val exportText = viewModel.getSessionExportText(curSession.id)
                                            if (exportText.isNotBlank()) {
                                                safeCopyText(context, "Conversación Riso", exportText, "Conversación copiada al portapapeles")
                                                safeShareText(context, exportText, "Exportar conversación")
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Exportar conversación completa", modifier = Modifier.size(18.dp))
                                }
                            }
                            IconButton(
                                onClick = { viewModel.createNewSession() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Nueva conversación", modifier = Modifier.size(20.dp))
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
    val selectedSessionId by viewModel.selectedSessionId.collectAsStateWithLifecycle()
    val messages by viewModel.currentMessages.collectAsStateWithLifecycle()
    val isLlmLoading by viewModel.isLlmLoading.collectAsStateWithLifecycle()
    val planningMode by viewModel.planningMode.collectAsStateWithLifecycle()
    val pendingActions by viewModel.pendingActions.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val emailAccounts by viewModel.emailAccounts.collectAsStateWithLifecycle()
    val activeEmailAccountId by viewModel.activeEmailAccountId.collectAsStateWithLifecycle()
    val githubAccounts by viewModel.githubAccounts.collectAsStateWithLifecycle()
    val gitlabAccounts by viewModel.gitlabAccounts.collectAsStateWithLifecycle()

    val sttProvider by viewModel.sttProvider.collectAsStateWithLifecycle()
    val whisperStatus by viewModel.localWhisperStatus.collectAsStateWithLifecycle()
    val isRecordingAudio by viewModel.isRecordingAudio.collectAsStateWithLifecycle()
    val isTranscribingAudio by viewModel.isTranscribingAudio.collectAsStateWithLifecycle()
    val recordingFeedback by viewModel.recordingFeedback.collectAsStateWithLifecycle()
    val attachedImage by viewModel.attachedImage.collectAsStateWithLifecycle()
    val attachments by viewModel.attachments.collectAsStateWithLifecycle()
    val loadingStatusText by viewModel.loadingStatusText.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            viewModel.startAudioRecording()
        }
    }

    var textInput by remember { mutableStateOf("") }
    var showAttachMenu by remember { mutableStateOf(false) }
    var showModelSelector by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedMessageIds = remember { mutableStateListOf<String>() }
    var editingMessageText by remember { mutableStateOf<String?>(null) }

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

    // Reset selection and message editor on session change
    LaunchedEffect(selectedSessionId) {
        isSelectionMode = false
        selectedMessageIds.clear()
        editingMessageText = null
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

        // Multi-selection bar for exporting selected messages
        if (isSelectionMode) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                isSelectionMode = false
                                selectedMessageIds.clear()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancelar selección", modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${selectedMessageIds.size} seleccionados",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = {
                                if (selectedMessageIds.size == messages.size) {
                                    selectedMessageIds.clear()
                                } else {
                                    selectedMessageIds.clear()
                                    selectedMessageIds.addAll(messages.map { it.id })
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                if (selectedMessageIds.size == messages.size) "Deseleccionar" else "Todos",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Button(
                            onClick = {
                                if (selectedMessageIds.isNotEmpty()) {
                                    val textToExport = viewModel.formatSelectedMessagesExport(selectedMessageIds.toSet())
                                    safeCopyText(context, "Mensajes Riso", textToExport, "${selectedMessageIds.size} mensajes copiados al portapapeles")
                                    safeShareText(context, textToExport, "Exportar mensajes")
                                    isSelectionMode = false
                                    selectedMessageIds.clear()
                                }
                            },
                            enabled = selectedMessageIds.isNotEmpty(),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("📤 Exportar", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Chat Conversation window / Empty state
        if (messages.isEmpty()) {
            EmptyChatState(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                isEn = isEn,
                onSelectPrompt = { prompt ->
                    textInput = prompt
                }
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatMessageItem(
                        message = msg,
                        pendingActions = pendingActions,
                        onApprove = { viewModel.approveAction(it) },
                        onReject = { viewModel.rejectAction(it) },
                        isSelectionMode = isSelectionMode,
                        isSelected = selectedMessageIds.contains(msg.id),
                        onToggleSelect = {
                            if (selectedMessageIds.contains(msg.id)) {
                                selectedMessageIds.remove(msg.id)
                                if (selectedMessageIds.isEmpty()) isSelectionMode = false
                            } else {
                                selectedMessageIds.add(msg.id)
                            }
                        },
                        onStartSelection = {
                            isSelectionMode = true
                            if (!selectedMessageIds.contains(msg.id)) {
                                selectedMessageIds.add(msg.id)
                            }
                        },
                        onCopyMessage = {
                            safeCopyText(context, "Mensaje Riso", msg.text, "Mensaje copiado al portapapeles")
                        },
                        onEditMessage = {
                            editingMessageText = msg.text
                            textInput = msg.text
                        },
                        onShareMessage = {
                            safeShareText(context, msg.text, "Compartir mensaje")
                        }
                    )
                }

                if (isLlmLoading) {
                    item {
                        LlmStreamingLoader(loadingStatusText)
                    }
                }
            }
        }

        // Recording & Transcription Feedback Banner
        if (isRecordingAudio || isTranscribingAudio || recordingFeedback.isNotBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = if (isRecordingAudio) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        else if (isTranscribingAudio) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    1.dp,
                    if (isRecordingAudio) MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                    else if (isTranscribingAudio) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isRecordingAudio) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        )
                    } else if (isTranscribingAudio) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = recordingFeedback,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (isRecordingAudio) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isRecordingAudio) {
                        TextButton(
                            onClick = {
                                viewModel.stopAudioRecordingAndTranscribe { transcription ->
                                    textInput = transcription
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(
                                text = "Listo ⏹",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else if (!isTranscribingAudio) {
                        IconButton(
                            onClick = { viewModel.clearRecordingFeedback() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Attachment Preview Chips Box
        if (attachments.isNotEmpty() || attachedImage != null) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (attachments.isNotEmpty()) {
                    items(attachments, key = { it.id }) { att ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(if (att.type == "camera" || att.type == "image") "📷" else "📎", fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = att.name,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 150.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { viewModel.removeAttachment(att.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remover archivo",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                } else if (attachedImage != null) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("📷", fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Archivo: $attachedImage.png",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { viewModel.attachSampleImage(null) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remover archivo",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Modern Floating Composer Container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 8.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 6.dp, start = 6.dp, end = 6.dp)
            ) {
                // Top control bar: LLM Model selector & Planning mode switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val llmProfiles by viewModel.llmProfiles.collectAsStateWithLifecycle()
                    val activeLlmId by viewModel.activeLlmProfileId.collectAsStateWithLifecycle()
                    val activeProf = llmProfiles.find { it.id == activeLlmId }
                    val activeDisplayName = activeProf?.name

                    // Select active LLM directly from chat box
                    Surface(
                        onClick = { showModelSelector = true },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        border = BorderStroke(
                            1.dp,
                            if (activeProf != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .heightIn(min = 34.dp)
                            .testTag("chat_box_llm_selector")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (activeDisplayName != null) "🤖 $activeDisplayName ▾" else "⚠️ Seleccionar Modelo ▾",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (activeProf != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // Planning Mode / Execution Mode toggle
                    Surface(
                        onClick = { viewModel.togglePlanningMode() },
                        shape = RoundedCornerShape(16.dp),
                        color = if (planningMode) StatusSuccess.copy(alpha = 0.12f)
                                else StatusError.copy(alpha = 0.12f),
                        border = BorderStroke(
                            1.dp,
                            if (planningMode) StatusSuccess.copy(alpha = 0.45f)
                            else StatusError.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .heightIn(min = 34.dp)
                            .testTag("toggle_planning_mode_chat")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (planningMode) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(StatusSuccess)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Planificación",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StatusSuccess
                                )
                            } else {
                                Text(
                                    text = "Ejecución",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StatusError
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(StatusError)
                                )
                            }
                        }
                    }
                }

                // Banner if editing a message to resend
                if (editingMessageText != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "✏️ Editando mensaje para reenviar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    editingMessageText = null
                                    textInput = ""
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancelar edición",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }

                // Bottom Input Row: (+) button, TextField, Mic, Send
                val canSend = textInput.isNotBlank() || attachments.isNotEmpty() || attachedImage != null
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // MCP & Attachments button (48dp touch target)
                    IconButton(
                        onClick = { showAttachMenu = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("mcp_attachments_plus_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Adjuntar / MCP",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Chat input text field without its own heavy border
                    TextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = {
                            Text(
                                text = if (isEn) "Ask Riso anything..." else "Pregunta a Riso...",
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 4,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_text_field"),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (canSend) {
                                viewModel.sendMessage(textInput)
                                textInput = ""
                                editingMessageText = null
                                keyboardController?.hide()
                            }
                        })
                    )

                    // STT Microphone button (48dp touch target)
                    IconButton(
                        onClick = {
                            if (isRecordingAudio) {
                                viewModel.stopAudioRecordingAndTranscribe { transcription ->
                                    textInput = transcription
                                }
                            } else if (!hasMicPermission) {
                                micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            } else {
                                viewModel.startAudioRecording()
                            }
                        },
                        enabled = !isTranscribingAudio,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_stt_microphone")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isRecordingAudio) MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                    else if (isTranscribingAudio) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isRecordingAudio) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.error)
                                )
                            } else if (isTranscribingAudio) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = "🎙️",
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }

                    // Send button (48dp touch target)
                    IconButton(
                        onClick = {
                            if (canSend) {
                                viewModel.sendMessage(textInput)
                                textInput = ""
                                editingMessageText = null
                                keyboardController?.hide()
                            }
                        },
                        enabled = canSend,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_send_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (canSend) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHighest
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Enviar",
                                tint = if (canSend) MaterialTheme.colorScheme.onPrimary
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
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
fun EmptyChatState(
    modifier: Modifier = Modifier,
    isEn: Boolean,
    onSelectPrompt: (String) -> Unit
) {
    val samplePrompts = if (isEn) {
        listOf(
            "📧 Summarize my recent emails",
            "📝 Draft a weekly status report",
            "🔍 Search tech news on the web",
            "⚡ Plan and organize my daily tasks"
        )
    } else {
        listOf(
            "📧 Resumir mis correos recientes",
            "📝 Redactar un reporte de estado",
            "🔍 Buscar novedades tecnológicas en la web",
            "⚡ Planificar y organizar mis tareas"
        )
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f, fill = false))

        // Riso avatar with gentle radial gradient halo
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                RisoLogo(modifier = Modifier.size(38.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hero greeting
        Text(
            text = if (isEn) "How can I help you today?" else "¿En qué puedo ayudarte hoy?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isEn) "Type a request or try one of these suggestions" else "Escribe tu consulta o prueba una de estas sugerencias",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Suggestion prompt cards
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            samplePrompts.forEach { prompt ->
                Surface(
                    onClick = { onSelectPrompt(prompt.substring(3).trim()) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f, fill = false))
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    pendingActions: List<PendingAction>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onStartSelection: () -> Unit = {},
    onCopyMessage: () -> Unit = {},
    onEditMessage: () -> Unit = {},
    onShareMessage: () -> Unit = {}
) {
    val isUser = message.sender == "user"
    val isSystem = message.sender == "system"
    var showMenu by remember { mutableStateOf(false) }

    if (isSystem) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
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
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = isSelectionMode) { onToggleSelect() }
                .padding(vertical = 3.dp),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode && !isUser) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .wrapContentWidth(if (isUser) Alignment.End else Alignment.Start)
            ) {
                if (isUser) {
                    Card(
                        shape = RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = 20.dp,
                            bottomEnd = 6.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            else MaterialTheme.colorScheme.primary
                        ),
                        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primaryContainer) else null,
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                        modifier = Modifier.widthIn(min = 40.dp, max = 340.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            // Markdown text
                            MarkdownRenderer(
                                text = message.text,
                                textColor = MaterialTheme.colorScheme.onPrimary,
                                isUserMessage = true
                            )

                            // Mini menu for user message
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onCopyMessage,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Copiar",
                                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                IconButton(
                                    onClick = onEditMessage,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Editar",
                                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 6.dp,
                            topEnd = 20.dp,
                            bottomStart = 20.dp,
                            bottomEnd = 20.dp
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                               else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.widthIn(min = 60.dp, max = 340.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            // Header row with Riso Agent badge & More Menu
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        RisoLogo(modifier = Modifier.size(14.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Riso Agent",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                Box {
                                    IconButton(
                                        onClick = { showMenu = true },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Opciones de mensaje",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("📋 Copiar mensaje", fontSize = 12.sp) },
                                            onClick = {
                                                showMenu = false
                                                onCopyMessage()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("✏️ Editar y reenviar", fontSize = 12.sp) },
                                            onClick = {
                                                showMenu = false
                                                onEditMessage()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("☑️ Seleccionar mensajes", fontSize = 12.sp) },
                                            onClick = {
                                                showMenu = false
                                                onStartSelection()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("📤 Compartir", fontSize = 12.sp) },
                                            onClick = {
                                                showMenu = false
                                                onShareMessage()
                                            }
                                        )
                                    }
                                }
                            }

                            // Markdown formatted body
                            MarkdownRenderer(
                                text = message.text,
                                textColor = MaterialTheme.colorScheme.onSurface,
                                isUserMessage = false
                            )

                            // Quick mini action bar under message with 48dp touch targets
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onCopyMessage,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Text("📋", fontSize = 14.sp)
                                }
                                IconButton(
                                    onClick = onEditMessage,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Editar y reenviar",
                                        modifier = Modifier.size(15.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = onShareMessage,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Compartir",
                                        modifier = Modifier.size(15.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

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

            if (isSelectionMode && isUser) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    modifier = Modifier.padding(start = 4.dp)
                )
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Lock",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Acción Automatizada",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                Badge(
                    containerColor = when (action.status) {
                        "PENDING" -> StatusWarning
                        "APPROVED" -> StatusSuccess
                        else -> StatusError
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
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = action.details,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Metodo: ${action.functionName}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
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
                            .heightIn(min = 48.dp)
                            .testTag("action_reject_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                        border = BorderStroke(1.dp, StatusError)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Rechazar", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rechazar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onApprove,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("action_approve_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Aprobar", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Aprobar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LlmStreamingLoader(statusText: String = "Riso analizando...") {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking_dots")
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 0, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 3 pulsating thinking dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = dot1Alpha))
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = dot2Alpha))
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = dot3Alpha))
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

    val settingsListState = rememberLazyListState()

    LazyColumn(
        state = settingsListState,
        modifier = Modifier
            .fillMaxSize()
            .simpleVerticalScrollbar(settingsListState)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
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
                                    .padding(vertical = 4.dp)
                                    .clickable { viewModel.selectActiveLlmProfile(profile.id) },
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
                                                        llmTestStatuses = llmTestStatuses + (profile.id to msg)
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
                                                    text = testMsg.removePrefix("✓").removePrefix("✕").trim(),
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
                            Text("Agregar Modelo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                label = { Text("Nombre (ej: Gemini 2.5 Flash, DeepSeek OpenCode)", fontSize = 11.sp) },
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
                                    "Google" to ("https://generativelanguage.googleapis.com" to "gemini-2.5-flash"),
                                    "OpenCode" to ("https://opencode.ai/zen/go/v1" to "deepseek-v4-flash-free"),
                                    "DeepSeek" to ("https://api.deepseek.com/v1" to "deepseek-chat"),
                                    "Groq" to ("https://api.groq.com/openai/v1" to "llama-3.3-70b-versatile"),
                                    "OpenAI" to ("https://api.openai.com/v1" to "gpt-4o-mini")
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
                                                if (newLlmName.isBlank()) newLlmName = "$prov ${defaults.second}"
                                            },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(prov, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
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
                                val isOk = newLlmTestMsg!!.startsWith("✓")
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = if (isOk) Color(0xFF10B981).copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(0.5.dp, if (isOk) Color(0xFF10B981).copy(alpha = 0.3f) else MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
                                ) {
                                    Text(
                                        text = newLlmTestMsg!!,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isOk) Color(0xFF059669) else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
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
                                                newLlmTestMsg = msg
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
                                    .padding(vertical = 4.dp)
                                    .clickable { viewModel.selectActiveSttProfile(profile.id) },
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
                                            if (profile.isLocal) {
                                                if (whisperStatus == "Ready") {
                                                    OutlinedButton(
                                                        onClick = { viewModel.deleteLocalWhisper() },
                                                        modifier = Modifier.height(28.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                        shape = RoundedCornerShape(6.dp),
                                                        colors = ButtonDefaults.outlinedButtonColors(
                                                            contentColor = MaterialTheme.colorScheme.error
                                                        ),
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Borrar", modifier = Modifier.size(11.dp))
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text("Borrar modelo", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
                                                    }
                                                } else {
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
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    testingSttId = profile.id
                                                    viewModel.testSttConnection(profile.isLocal, profile.apiEndpoint, profile.apiKey, profile.modelName) { ok, msg ->
                                                        sttTestStatuses = sttTestStatuses + (profile.id to msg)
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

                                            if (!profile.isLocal) {
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
                                                    text = testMsg.removePrefix("✓").removePrefix("✕").trim(),
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
                            onClick = {
                                newSttName = ""
                                newSttEndpoint = "https://api.groq.com/openai/v1/audio/transcriptions"
                                newSttModel = "whisper-large-v3-turbo"
                                newSttKey = ""
                                newSttTestMsg = null
                                newSttIsLocal = false
                                showAddSttForm = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .testTag("show_add_stt_form_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Agregar STT Remoto", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Text("Nuevo STT Remoto (Whisper / Groq / OpenAI)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)

                            OutlinedTextField(
                                value = newSttName,
                                onValueChange = { newSttName = it },
                                label = { Text("Nombre (ej: Groq Whisper, OpenAI Whisper)", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("new_stt_name_input"),
                                singleLine = true
                            )

                            // Quick provider defaults
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "Groq" to ("https://api.groq.com/openai/v1/audio/transcriptions" to "whisper-large-v3-turbo"),
                                    "OpenAI" to ("https://api.openai.com/v1/audio/transcriptions" to "whisper-1")
                                ).forEach { (label, defs) ->
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                if (newSttName.isBlank()) newSttName = "$label Whisper"
                                                newSttEndpoint = defs.first
                                                newSttModel = defs.second
                                            },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 5.dp).fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                        }
                                    }
                                }
                            }

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
                                label = { Text("Nombre del Modelo (ej: whisper-large-v3-turbo)", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("new_stt_model_input"),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = newSttKey,
                                onValueChange = { newSttKey = it },
                                label = { Text("API Key (ej: gsk_... o sk-...)", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("new_stt_key_input"),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation()
                            )

                            if (newSttTestMsg != null) {
                                val isOk = newSttTestMsg!!.startsWith("✓")
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = if (isOk) Color(0xFF10B981).copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(0.5.dp, if (isOk) Color(0xFF10B981).copy(alpha = 0.3f) else MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
                                ) {
                                    Text(
                                        text = newSttTestMsg!!,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isOk) Color(0xFF059669) else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        isTestingNewStt = true
                                        viewModel.testSttConnection(false, newSttEndpoint, newSttKey, newSttModel) { ok, msg ->
                                            newSttTestMsg = msg
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
                                        if (newSttName.isNotBlank() && newSttKey.isNotBlank()) {
                                            viewModel.addSttProfile(
                                                name = newSttName,
                                                isLocal = false,
                                                apiEndpoint = newSttEndpoint,
                                                modelName = newSttModel,
                                                apiKey = newSttKey
                                            )
                                            showAddSttForm = false
                                            newSttName = ""
                                            newSttEndpoint = ""
                                            newSttModel = "whisper-large-v3-turbo"
                                            newSttKey = ""
                                            newSttTestMsg = null
                                        }
                                    },
                                    enabled = newSttName.isNotBlank() && newSttKey.isNotBlank(),
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
