package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatSession
import com.example.ui.L10n
import com.example.ui.RisoViewModel
import com.example.ui.screens.components.RisoLogo

@Composable
fun SessionsDrawer(
    viewModel: RisoViewModel,
    sessions: List<ChatSession>,
    selectedSessionId: String?,
    currentTab: String,
    settings: Map<String, String>,
    isEn: Boolean,
    onSelectChatTab: () -> Unit,
    onSelectSettingsTab: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    fun t(key: String): String = L10n.t(key, isEn)
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
                    onSelectChatTab()
                    onCloseDrawer()
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
                fun renderSessionItem(session: ChatSession) {
                    val isSelected = session.id == selectedSessionId && currentTab == "chat"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable {
                                viewModel.selectSession(session.id)
                                onSelectChatTab()
                                onCloseDrawer()
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
                    onSelectSettingsTab()
                    onCloseDrawer()
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
