package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.GithubAccount
import com.example.data.model.GitlabAccount
import com.example.service.email.EmailAccount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McpConnectionsDialog(
    viewModel: RisoViewModel,
    emailAccounts: List<EmailAccount>,
    githubAccounts: List<GithubAccount>,
    gitlabAccounts: List<GitlabAccount>,
    settings: Map<String, String>,
    onLaunchCamera: () -> Unit,
    onLaunchGallery: () -> Unit,
    onLaunchFilePicker: () -> Unit,
    onDismiss: () -> Unit
) {
    var mcpTabSelection by remember { mutableIntStateOf(0) } // 0: Correo, 1: GitHub & GitLab, 2: Adjuntos & Web
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

    var tempBraveApiKey by remember(settings) { mutableStateOf(settings["brave_search_api_key"] ?: "") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
                val (headerTitle, headerSubtitle) = when (mcpTabSelection) {
                    0 -> "Cuentas de Correo" to "Gestiona tus cuentas IMAP y SMTP para consulta y envío"
                    1 -> if (gitSubTabSelection == 0) "Cuentas de GitHub" to "Gestión de repositorios y actividad en GitHub"
                         else "Cuentas de GitLab" to "Gestión de repositorios y proyectos en GitLab"
                    2 -> "Búsqueda Web" to "Configuración del motor de búsqueda en tiempo real"
                    3 -> "Archivos y Adjuntos" to "Captura de cámara, galería y documentos"
                    else -> "Conexiones MCP" to "Herramientas y servicios conectados"
                }

                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = headerTitle,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = headerSubtitle,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Primary TabRow for MCP Sections
                TabRow(
                    selectedTabIndex = mcpTabSelection,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = mcpTabSelection == 0,
                        onClick = { mcpTabSelection = 0 },
                        text = {
                            Text(
                                text = "Correo (${emailAccounts.size})",
                                fontSize = 10.sp,
                                fontWeight = if (mcpTabSelection == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = mcpTabSelection == 1,
                        onClick = { mcpTabSelection = 1 },
                        text = {
                            Text(
                                text = "Git (${githubAccounts.size + gitlabAccounts.size})",
                                fontSize = 10.sp,
                                fontWeight = if (mcpTabSelection == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = mcpTabSelection == 2,
                        onClick = { mcpTabSelection = 2 },
                        text = {
                            Text(
                                text = "Web",
                                fontSize = 10.sp,
                                fontWeight = if (mcpTabSelection == 2) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = mcpTabSelection == 3,
                        onClick = { mcpTabSelection = 3 },
                        text = {
                            Text(
                                text = "Adjuntos",
                                fontSize = 10.sp,
                                fontWeight = if (mcpTabSelection == 3) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (mcpTabSelection) {
                        // ----------------- TAB 0: CORREO ELECTRONICO -----------------
                        0 -> {
                            val allEmailsSelected = emailAccounts.isNotEmpty() && emailAccounts.all { it.isEnabled }

                            // Action buttons bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.setAllEmailAccountsEnabled(!allEmailsSelected) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp),
                                    modifier = Modifier.height(30.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (allEmailsSelected) "Deseleccionar todos" else "Seleccionar todos",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Button(
                                    onClick = { showAddEmailForm = !showAddEmailForm },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp),
                                    modifier = Modifier.height(30.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (showAddEmailForm && emailAccounts.isNotEmpty()) "Ocultar" else "+ Añadir Cuenta",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Form to add email account
                            if (emailAccounts.isEmpty() || showAddEmailForm) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Nueva Cuenta de Correo (IMAP/SMTP)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        OutlinedTextField(
                                            value = emailInputAddress,
                                            onValueChange = { emailInputAddress = it },
                                            label = { Text("Correo (ej: usuario@gmail.com)", fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                        )
                                        OutlinedTextField(
                                            value = emailInputPassword,
                                            onValueChange = { emailInputPassword = it },
                                            label = { Text("Contraseña o App Password", fontSize = 11.sp) },
                                            singleLine = true,
                                            visualTransformation = PasswordVisualTransformation(),
                                            modifier = Modifier.fillMaxWidth(),
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = emailInputImapServer,
                                                onValueChange = { emailInputImapServer = it },
                                                label = { Text("Servidor IMAP", fontSize = 10.sp) },
                                                singleLine = true,
                                                modifier = Modifier.weight(0.68f),
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                                            )
                                            OutlinedTextField(
                                                value = emailInputImapPort,
                                                onValueChange = { emailInputImapPort = it },
                                                label = { Text("Puerto", fontSize = 10.sp) },
                                                singleLine = true,
                                                modifier = Modifier.weight(0.32f),
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = emailInputSmtpServer,
                                                onValueChange = { emailInputSmtpServer = it },
                                                label = { Text("Servidor SMTP", fontSize = 10.sp) },
                                                singleLine = true,
                                                modifier = Modifier.weight(0.68f),
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                                            )
                                            OutlinedTextField(
                                                value = emailInputSmtpPort,
                                                onValueChange = { emailInputSmtpPort = it },
                                                label = { Text("Puerto", fontSize = 10.sp) },
                                                singleLine = true,
                                                modifier = Modifier.weight(0.32f),
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                if (emailInputAddress.isNotBlank() && emailInputPassword.isNotBlank()) {
                                                    viewModel.addEmailAccount(
                                                        emailAddress = emailInputAddress.trim(),
                                                        imapServer = emailInputImapServer.trim().ifBlank { "imap.gmail.com" },
                                                        imapPort = emailInputImapPort.trim().ifBlank { "993" },
                                                        smtpServer = emailInputSmtpServer.trim().ifBlank { "smtp.gmail.com" },
                                                        smtpPort = emailInputSmtpPort.trim().ifBlank { "587" },
                                                        passwordVal = emailInputPassword.trim()
                                                    )
                                                    emailInputAddress = ""
                                                    emailInputPassword = ""
                                                    showAddEmailForm = false
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Guardar y Conectar Cuenta", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // List of email accounts
                            if (emailAccounts.isEmpty() && !showAddEmailForm) {
                                Text(
                                    text = "No tienes ninguna cuenta de correo agregada.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                emailAccounts.forEach { acc ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (acc.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (acc.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Checkbox(
                                                        checked = acc.isEnabled,
                                                        onCheckedChange = { viewModel.toggleEmailAccount(acc.id) }
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Column {
                                                        Text(
                                                            text = acc.emailAddress,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = "IMAP: ${acc.imapServer}:${acc.imapPort} • SMTP: ${acc.smtpServer}:${acc.smtpPort}",
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                        )
                                                    }
                                                }

                                                IconButton(
                                                    onClick = { viewModel.removeEmailAccount(acc.id) },
                                                    modifier = Modifier.size(30.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Eliminar",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                }
                                            }

                                            // Test connection row
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val status = emailTestResults[acc.id]
                                                if (status != null) {
                                                    Text(
                                                        text = status,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = if (status.startsWith("🟢")) Color(0xFF047857)
                                                                else if (status.startsWith("⏳")) MaterialTheme.colorScheme.primary
                                                                else MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.weight(1f).padding(end = 6.dp)
                                                    )
                                                } else {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        emailTestResults = emailTestResults + (acc.id to "⏳ Probando...")
                                                        viewModel.testEmailAccount(acc) { success, msg ->
                                                            emailTestResults = emailTestResults + (acc.id to if (success) "🟢 Conexión exitosa" else "🔴 $msg")
                                                        }
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(26.dp),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text("Probar Conexión", fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ----------------- TAB 1: GITHUB & GITLAB -----------------
                        1 -> {
                            // Sub-tabs for GitHub and GitLab
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = { gitSubTabSelection = 0 },
                                    modifier = Modifier.weight(1f).height(30.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (gitSubTabSelection == 0) MaterialTheme.colorScheme.primary
                                                         else Color.Transparent,
                                        contentColor = if (gitSubTabSelection == 0) MaterialTheme.colorScheme.onPrimary
                                                       else MaterialTheme.colorScheme.onSurface
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("GitHub (${githubAccounts.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { gitSubTabSelection = 1 },
                                    modifier = Modifier.weight(1f).height(30.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (gitSubTabSelection == 1) MaterialTheme.colorScheme.primary
                                                         else Color.Transparent,
                                        contentColor = if (gitSubTabSelection == 1) MaterialTheme.colorScheme.onPrimary
                                                       else MaterialTheme.colorScheme.onSurface
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("GitLab (${gitlabAccounts.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (gitSubTabSelection == 0) {
                                // --- GITHUB SECTION ---
                                val allGhSelected = githubAccounts.isNotEmpty() && githubAccounts.all { it.isEnabled }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.setAllGithubAccountsEnabled(!allGhSelected) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp),
                                        modifier = Modifier.height(30.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (allGhSelected) "Deseleccionar todos" else "Seleccionar todos",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Button(
                                        onClick = { showAddGithubForm = !showAddGithubForm },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp),
                                        modifier = Modifier.height(30.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (showAddGithubForm && githubAccounts.isNotEmpty()) "Ocultar" else "+ Añadir GitHub",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Add GitHub Form
                                if (githubAccounts.isEmpty() || showAddGithubForm) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Nueva Cuenta GitHub",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            OutlinedTextField(
                                                value = githubInputLabel,
                                                onValueChange = { githubInputLabel = it },
                                                label = { Text("Nombre / Etiqueta (ej: Personal / Trabajo)", fontSize = 11.sp) },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                            )
                                            OutlinedTextField(
                                                value = githubInputUsername,
                                                onValueChange = { githubInputUsername = it },
                                                label = { Text("Usuario GitHub (ej: octocat)", fontSize = 11.sp) },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                            )
                                            OutlinedTextField(
                                                value = githubInputToken,
                                                onValueChange = { githubInputToken = it },
                                                label = { Text("Personal Access Token (PAT ghp_...)", fontSize = 11.sp) },
                                                singleLine = true,
                                                visualTransformation = PasswordVisualTransformation(),
                                                modifier = Modifier.fillMaxWidth(),
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                            )

                                            Button(
                                                onClick = {
                                                    if (githubInputUsername.isNotBlank() && githubInputToken.isNotBlank()) {
                                                        viewModel.addGithubAccount(
                                                            username = githubInputUsername.trim(),
                                                            token = githubInputToken.trim(),
                                                            label = githubInputLabel.trim().ifBlank { "GitHub @${githubInputUsername.trim()}" }
                                                        )
                                                        githubInputUsername = ""
                                                        githubInputToken = ""
                                                        githubInputLabel = ""
                                                        showAddGithubForm = false
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Guardar Cuenta GitHub", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                // List of GitHub Accounts
                                if (githubAccounts.isEmpty() && !showAddGithubForm) {
                                    Text(
                                        text = "No tienes ninguna cuenta de GitHub agregada.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    githubAccounts.forEach { acc ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (acc.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                                                 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                            ),
                                            border = BorderStroke(
                                                1.dp,
                                                if (acc.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Checkbox(
                                                            checked = acc.isEnabled,
                                                            onCheckedChange = { viewModel.toggleGithubAccount(acc.id) }
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Column {
                                                            Text(
                                                                text = acc.label.ifBlank { "@${acc.username}" },
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 12.sp,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                text = "Usuario: @${acc.username} • Token configurado",
                                                                fontSize = 10.sp,
                                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                            )
                                                        }
                                                    }

                                                    IconButton(
                                                        onClick = { viewModel.removeGithubAccount(acc.id) },
                                                        modifier = Modifier.size(30.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Eliminar",
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(17.dp)
                                                        )
                                                    }
                                                }

                                                // Test connection row
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val status = githubTestResults[acc.id]
                                                    if (status != null) {
                                                        Text(
                                                            text = status,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = if (status.startsWith("🟢")) Color(0xFF047857)
                                                                    else if (status.startsWith("⏳")) MaterialTheme.colorScheme.primary
                                                                    else MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                                                        )
                                                    } else {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }

                                                    OutlinedButton(
                                                        onClick = {
                                                            githubTestResults = githubTestResults + (acc.id to "⏳ Probando...")
                                                            viewModel.testGithubConnection(acc.token, acc.username) { success, msg ->
                                                                githubTestResults = githubTestResults + (acc.id to if (success) "🟢 Conexión exitosa (@${acc.username})" else "🔴 $msg")
                                                            }
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(26.dp),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text("Probar Conexión", fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // --- GITLAB SECTION ---
                                val allGlSelected = gitlabAccounts.isNotEmpty() && gitlabAccounts.all { it.isEnabled }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.setAllGitlabAccountsEnabled(!allGlSelected) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp),
                                        modifier = Modifier.height(30.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (allGlSelected) "Deseleccionar todos" else "Seleccionar todos",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Button(
                                        onClick = { showAddGitlabForm = !showAddGitlabForm },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp),
                                        modifier = Modifier.height(30.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (showAddGitlabForm && gitlabAccounts.isNotEmpty()) "Ocultar" else "+ Añadir GitLab",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Add GitLab Form
                                if (gitlabAccounts.isEmpty() || showAddGitlabForm) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Nueva Cuenta GitLab",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            OutlinedTextField(
                                                value = gitlabInputUrl,
                                                onValueChange = { gitlabInputUrl = it },
                                                label = { Text("URL de Servidor (ej: https://gitlab.com)", fontSize = 11.sp) },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                            )
                                            OutlinedTextField(
                                                value = gitlabInputLabel,
                                                onValueChange = { gitlabInputLabel = it },
                                                label = { Text("Nombre / Etiqueta (ej: GitLab Trabajo)", fontSize = 11.sp) },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                            )
                                            OutlinedTextField(
                                                value = gitlabInputUsername,
                                                onValueChange = { gitlabInputUsername = it },
                                                label = { Text("Usuario GitLab", fontSize = 11.sp) },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                            )
                                            OutlinedTextField(
                                                value = gitlabInputToken,
                                                onValueChange = { gitlabInputToken = it },
                                                label = { Text("Personal Access Token (PAT glpat-...)", fontSize = 11.sp) },
                                                singleLine = true,
                                                visualTransformation = PasswordVisualTransformation(),
                                                modifier = Modifier.fillMaxWidth(),
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                            )

                                            Button(
                                                onClick = {
                                                    if (gitlabInputUsername.isNotBlank() && gitlabInputToken.isNotBlank()) {
                                                        viewModel.addGitlabAccount(
                                                            instanceUrl = gitlabInputUrl.trim().ifBlank { "https://gitlab.com" },
                                                            username = gitlabInputUsername.trim(),
                                                            token = gitlabInputToken.trim(),
                                                            label = gitlabInputLabel.trim().ifBlank { "GitLab @${gitlabInputUsername.trim()}" }
                                                        )
                                                        gitlabInputUsername = ""
                                                        gitlabInputToken = ""
                                                        gitlabInputLabel = ""
                                                        showAddGitlabForm = false
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Guardar Cuenta GitLab", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                // List of GitLab Accounts
                                if (gitlabAccounts.isEmpty() && !showAddGitlabForm) {
                                    Text(
                                        text = "No tienes ninguna cuenta de GitLab agregada.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    gitlabAccounts.forEach { acc ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (acc.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                                                 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                            ),
                                            border = BorderStroke(
                                                1.dp,
                                                if (acc.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Checkbox(
                                                            checked = acc.isEnabled,
                                                            onCheckedChange = { viewModel.toggleGitlabAccount(acc.id) }
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Column {
                                                            Text(
                                                                text = acc.label.ifBlank { "@${acc.username}" },
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 12.sp,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                text = "${acc.instanceUrl} • @${acc.username}",
                                                                fontSize = 10.sp,
                                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                            )
                                                        }
                                                    }

                                                    IconButton(
                                                        onClick = { viewModel.removeGitlabAccount(acc.id) },
                                                        modifier = Modifier.size(30.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Eliminar",
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(17.dp)
                                                        )
                                                    }
                                                }

                                                // Test connection row
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val status = gitlabTestResults[acc.id]
                                                    if (status != null) {
                                                        Text(
                                                            text = status,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = if (status.startsWith("🟢")) Color(0xFF047857)
                                                                    else if (status.startsWith("⏳")) MaterialTheme.colorScheme.primary
                                                                    else MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                                                        )
                                                    } else {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }

                                                    OutlinedButton(
                                                        onClick = {
                                                            gitlabTestResults = gitlabTestResults + (acc.id to "⏳ Probando...")
                                                            viewModel.testGitlabConnection(acc.instanceUrl, acc.token, acc.username) { success, msg ->
                                                                gitlabTestResults = gitlabTestResults + (acc.id to if (success) "🟢 Conexión exitosa (@${acc.username})" else "🔴 $msg")
                                                            }
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(26.dp),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text("Probar Conexión", fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ----------------- TAB 2: BUSQUEDA EN INTERNET & SCRAPER -----------------
                        2 -> {
                            val internetSearchEnabled = settings["internet_search_enabled"] == "true"
                            val searchProvider = settings["search_provider"] ?: "duckduckgo_scraper"

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Text("🌐", fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text("Búsqueda en Internet & Web Scraper", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("Permite a Riso buscar en tiempo real y leer páginas web", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                            }
                                        }
                                        Switch(
                                            checked = internetSearchEnabled,
                                            onCheckedChange = { isChecked ->
                                                val value = if (isChecked) "true" else "false"
                                                viewModel.updateSetting("internet_search_enabled", value)
                                                viewModel.updateSetting("mcp_web_search_enabled", value)
                                            },
                                            modifier = Modifier.scale(0.8f)
                                        )
                                    }

                                    if (internetSearchEnabled) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = "Motor de Búsqueda & Scraper:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )

                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            // 1. DuckDuckGo Scraper (Gratis y sin API key)
                                            Card(
                                                shape = RoundedCornerShape(10.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (searchProvider == "duckduckgo_scraper") MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                                                     else MaterialTheme.colorScheme.surface
                                                ),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (searchProvider == "duckduckgo_scraper") MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { viewModel.updateSetting("search_provider", "duckduckgo_scraper") }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = searchProvider == "duckduckgo_scraper",
                                                        onClick = { viewModel.updateSetting("search_provider", "duckduckgo_scraper") },
                                                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Column {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text("DuckDuckGo Scraper (Libre)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text("✨ Sin API Key", fontSize = 9.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                                        }
                                                        Text("Búsquedas web y extracción de contenido de URLs 100% libre, sin registro ni costos.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                                    }
                                                }
                                            }

                                            // 2. Brave Search API
                                            val hasBraveKey = !settings["brave_search_api_key"].isNullOrBlank()
                                            Card(
                                                shape = RoundedCornerShape(10.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (searchProvider == "brave") MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                                                     else MaterialTheme.colorScheme.surface
                                                ),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (searchProvider == "brave") MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { viewModel.updateSetting("search_provider", "brave") }
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        RadioButton(
                                                            selected = searchProvider == "brave",
                                                            onClick = { viewModel.updateSetting("search_provider", "brave") },
                                                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text("Brave Search API", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Text(
                                                                    text = if (hasBraveKey) "🔑 Configurado" else "⚠️ Requiere API Key",
                                                                    fontSize = 9.sp,
                                                                    color = if (hasBraveKey) Color(0xFF10B981) else Color(0xFFEF4444),
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                            Text("Búsqueda indexada web independiente usando la API de Brave.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                                        }
                                                    }

                                                    if (searchProvider == "brave") {
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        OutlinedTextField(
                                                            value = tempBraveApiKey,
                                                            onValueChange = {
                                                                tempBraveApiKey = it
                                                                viewModel.updateSetting("brave_search_api_key", it)
                                                            },
                                                            label = { Text("Brave Search API Key", fontSize = 11.sp) },
                                                            placeholder = { Text("BSA...", fontSize = 11.sp) },
                                                            visualTransformation = PasswordVisualTransformation(),
                                                            shape = RoundedCornerShape(8.dp),
                                                            modifier = Modifier.fillMaxWidth(),
                                                            singleLine = true,
                                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                                                        )
                                                    }
                                                }
                                            }

                                            // Web page scraper indicator
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
                                                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)), RoundedCornerShape(8.dp))
                                                    .padding(10.dp)
                                            ) {
                                                Text(
                                                    text = "⚡ Scraper automático incluido: Riso puede acceder en tiempo real y extraer texto limpio de cualquier página web o enlace URL que compartas en el chat.",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ----------------- TAB 3: ADJUNTOS & ARCHIVOS -----------------
                        3 -> {
                            Text(
                                text = "Adjuntar Elementos al Chat",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            onLaunchCamera()
                                            onDismiss()
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("📷", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text("Cámara", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            onLaunchGallery()
                                            onDismiss()
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("🖼️", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text("Galería", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            onLaunchFilePicker()
                                            onDismiss()
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("📂", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text("Archivo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Aceptar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
    }
}
