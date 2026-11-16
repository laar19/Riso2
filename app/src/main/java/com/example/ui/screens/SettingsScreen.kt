package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.L10n
import com.example.ui.RisoIcons
import com.example.ui.RisoViewModel
import com.example.ui.screens.components.simpleVerticalScrollbar

// --- TAB 3: Configuration & Accounts Dashboard Screen ---
@Composable
fun RisoSettingsScreen(viewModel: RisoViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val emailAccounts by viewModel.emailAccounts.collectAsStateWithLifecycle()
    val activeEmailAccountId by viewModel.activeEmailAccountId.collectAsStateWithLifecycle()

    val whisperStatus by viewModel.localWhisperStatus.collectAsStateWithLifecycle()
    val whisperProgress by viewModel.localWhisperProgress.collectAsStateWithLifecycle()

    val isEn = settings["language"] == "en"
    fun t(key: String): String = L10n.t(key, isEn)

    var geminiKey by remember { mutableStateOf("") }
    var braveKey by remember { mutableStateOf("") }

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
                            Text("Español", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
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
                            Text("English", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(RisoIcons.LightMode, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Modo Claro", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            }
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(RisoIcons.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Modo Oscuro", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // APARTADO 1: Configuración de APIs LLM (Modelos de Lenguaje)
        item {
            LlmProfilesSection(viewModel = viewModel)
        }

        // APARTADO 2: Configuración de Speech to Text (STT - Remoto y Local)
        item {
            SttProfilesSection(viewModel = viewModel)
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

        // Section 4: Transparency, Data Privacy & Account Deletion (Google Play Compliance)
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
                        Icon(
                            imageVector = RisoIcons.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
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
