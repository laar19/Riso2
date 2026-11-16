package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import com.example.ui.RisoViewModel

@Composable
fun LlmProfilesSection(viewModel: RisoViewModel) {
    val llmProfiles by viewModel.llmProfiles.collectAsStateWithLifecycle()
    val activeLlmProfileId by viewModel.activeLlmProfileId.collectAsStateWithLifecycle()

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
