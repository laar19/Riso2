package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.audio.OnDeviceSpeechRecognizerHelper
import com.example.ui.RisoViewModel
import com.example.ui.theme.StatusWarning

@Composable
fun SttProfilesSection(viewModel: RisoViewModel) {
    val strictOfflineStt by viewModel.strictOfflineStt.collectAsStateWithLifecycle()
    val sttEngineMode by viewModel.sttEngineMode.collectAsStateWithLifecycle()
    val sttProfiles by viewModel.sttProfiles.collectAsStateWithLifecycle()
    val activeSttProfileId by viewModel.activeSttProfileId.collectAsStateWithLifecycle()

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
                text = "Configura el motor de reconocimiento en el dispositivo o Whisper API remoto. Elige cuál usar por defecto.",
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Strict offline toggle
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Solo offline estricto",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "No iniciar dictado si el dispositivo no tiene motor en el dispositivo.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = strictOfflineStt,
                        onCheckedChange = { viewModel.setStrictOfflineStt(it) },
                        modifier = Modifier.testTag("strict_offline_stt_switch")
                    )
                }
            }

            if (sttEngineMode == OnDeviceSpeechRecognizerHelper.EngineMode.SYSTEM_MAYBE_ONLINE) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("settings_stt_system_warning"),
                    shape = RoundedCornerShape(10.dp),
                    color = StatusWarning.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Aviso motor sistema",
                            tint = StatusWarning,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Este dispositivo no tiene motor offline; el audio puede procesarse en servidores de Google.",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

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
                                        OutlinedButton(
                                            onClick = { viewModel.triggerOnDeviceSpeechModelDownload() },
                                            modifier = Modifier
                                                .height(28.dp)
                                                .testTag("download_on_device_stt_btn"),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.tertiary
                                            ),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f))
                                        ) {
                                            Text(
                                                text = "Descargar idioma",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
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
