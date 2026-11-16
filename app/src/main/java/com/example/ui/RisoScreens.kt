package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.RisoChatScreen
import com.example.ui.screens.RisoSettingsScreen
import com.example.ui.screens.SessionsDrawer
import com.example.ui.screens.components.safeCopyText
import com.example.ui.screens.components.safeShareText
import kotlinx.coroutines.launch

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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SessionsDrawer(
                viewModel = viewModel,
                sessions = sessions,
                selectedSessionId = selectedSessionId,
                currentTab = currentTab,
                settings = settings,
                isEn = isEn,
                onSelectChatTab = { currentTab = "chat" },
                onSelectSettingsTab = { currentTab = "settings" },
                onCloseDrawer = { coroutineScope.launch { drawerState.close() } }
            )
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
