package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val llmProvider: String, // "Gemini" | "OpenAI" | "Claude"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val sender: String, // "user" | "ai" | "system" | "tool"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val pendingActionId: String? = null,
    val isStreaming: Boolean = false
)

@Entity(tableName = "pending_actions")
data class PendingAction(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val messageId: String,
    val functionName: String, // list_inbox, send_email, reply_to_email, forward_email, archive_email, etc.
    val argumentsJson: String,
    val status: String, // "PENDING" | "APPROVED" | "REJECTED"
    val details: String = ""
)

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String
)
