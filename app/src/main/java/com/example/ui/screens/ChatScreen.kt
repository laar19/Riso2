package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChatMessage
import com.example.data.model.PendingAction
import com.example.ui.L10n
import com.example.ui.MarkdownRenderer
import com.example.ui.McpConnectionsDialog
import com.example.ui.RisoViewModel
import com.example.ui.screens.components.EmptyChatState
import com.example.ui.screens.components.LlmStreamingLoader
import com.example.ui.screens.components.RisoLogo
import com.example.ui.screens.components.safeCopyText
import com.example.ui.screens.components.safeShareText
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

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
    val sttRmsLevel by viewModel.sttRmsLevel.collectAsStateWithLifecycle()
    val sttEngineMode by viewModel.sttEngineMode.collectAsStateWithLifecycle()
    val sttSystemWarning by viewModel.sttSystemWarning.collectAsStateWithLifecycle()
    val pendingSttDownloadLang by viewModel.pendingSttDownloadLang.collectAsStateWithLifecycle()
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
        // System STT fallback privacy warning banner
        val activeSttWarning = sttSystemWarning
        if (activeSttWarning != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .testTag("stt_system_warning_banner"),
                color = StatusWarning.copy(alpha = 0.14f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Aviso STT",
                        tint = StatusWarning,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = activeSttWarning,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.dismissSttSystemWarning() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar aviso",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
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
                    if (pendingSttDownloadLang != null && !isRecordingAudio) {
                        TextButton(
                            onClick = { viewModel.triggerOnDeviceSpeechModelDownload() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("stt_download_language_btn")
                        ) {
                            Text(
                                text = "Descargar ($pendingSttDownloadLang)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (isRecordingAudio) {
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

                    // STT Microphone button (48dp touch target) animated by onRmsChanged voice level
                    val animatedRmsScale by animateFloatAsState(
                        targetValue = if (isRecordingAudio) 1f + (sttRmsLevel * 0.55f) else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "mic_rms_halo_scale"
                    )
                    val animatedMicScale by animateFloatAsState(
                        targetValue = if (isRecordingAudio) 1f + (sttRmsLevel * 0.16f) else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "mic_rms_inner_scale"
                    )
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
                        Box(contentAlignment = Alignment.Center) {
                            if (isRecordingAudio) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .scale(animatedRmsScale)
                                        .clip(CircleShape)
                                        .background(
                                            MaterialTheme.colorScheme.error.copy(
                                                alpha = (0.14f + sttRmsLevel * 0.30f).coerceIn(0.14f, 0.45f)
                                            )
                                        )
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .scale(animatedMicScale)
                                    .clip(CircleShape)
                                    .background(
                                        if (isRecordingAudio) MaterialTheme.colorScheme.error.copy(alpha = 0.22f)
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

            val parsedArgs = remember(action.argumentsJson) {
                try {
                    org.json.JSONObject(action.argumentsJson)
                } catch (_: Exception) {
                    null
                }
            }
            val recipientPreview = remember(parsedArgs) {
                parsedArgs?.optString("to", "")?.takeIf { it.isNotBlank() }
                    ?: parsedArgs?.optString("messageId", "")?.takeIf { it.isNotBlank() }?.let { "Mensaje #$it" }
                    ?: parsedArgs?.let { j ->
                        val owner = j.optString("owner", "")
                        val repo = j.optString("repo", "")
                        val projectId = j.optString("projectId", "")
                        when {
                            owner.isNotBlank() && repo.isNotBlank() -> "$owner/$repo"
                            projectId.isNotBlank() -> projectId
                            else -> null
                        }
                    }
            }
            val subjectPreview = remember(parsedArgs) {
                parsedArgs?.optString("subject", "")?.takeIf { it.isNotBlank() }
                    ?: parsedArgs?.optString("title", "")?.takeIf { it.isNotBlank() }
            }
            val bodyPreview = remember(parsedArgs) {
                val raw = parsedArgs?.optString("body", "")?.takeIf { it.isNotBlank() }
                    ?: parsedArgs?.optString("description", "")?.takeIf { it.isNotBlank() }
                raw?.let { if (it.length > 200) "${it.take(200)}..." else it }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = action.details,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (recipientPreview != null || subjectPreview != null || bodyPreview != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (recipientPreview != null) {
                            Text(
                                text = "Destinatario: $recipientPreview",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("pending_action_recipient")
                            )
                        }
                        if (subjectPreview != null) {
                            Text(
                                text = "Asunto: $subjectPreview",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("pending_action_subject")
                            )
                        }
                        if (bodyPreview != null) {
                            Text(
                                text = "Cuerpo: $bodyPreview",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                modifier = Modifier.testTag("pending_action_body_preview")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
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
