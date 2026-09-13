package com.example.data.repository

import com.example.data.local.RisoDao
import com.example.data.model.ChatSession
import com.example.data.model.ChatMessage
import com.example.data.model.PendingAction
import com.example.data.model.AppSetting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RisoRepository(private val risoDao: RisoDao) {

    // Chat Sessions
    val allSessions: Flow<List<ChatSession>> = risoDao.getAllSessions()

    suspend fun createSession(title: String, provider: String): ChatSession {
        val session = ChatSession(title = title, llmProvider = provider)
        risoDao.insertSession(session)
        return session
    }

    suspend fun updateSessionTitle(sessionId: String, title: String) {
        risoDao.updateSessionTitle(sessionId, title)
    }

    suspend fun updateSessionPinned(sessionId: String, isPinned: Boolean) {
        risoDao.updateSessionPinned(sessionId, isPinned)
    }

    suspend fun deleteSession(sessionId: String) {
        risoDao.deleteMessagesBySessionId(sessionId)
        risoDao.deleteSessionById(sessionId)
    }

    suspend fun getMessageCountForSession(sessionId: String): Int {
        return risoDao.getMessageCountForSession(sessionId)
    }

    suspend fun getMessagesListForSession(sessionId: String): List<ChatMessage> {
        return risoDao.getMessagesListForSession(sessionId)
    }

    suspend fun deleteEmptySessions() {
        risoDao.deleteEmptySessions()
    }

    suspend fun deleteEmptySessionsExcept(exceptSessionId: String) {
        risoDao.deleteEmptySessionsExcept(exceptSessionId)
    }

    // Chat Messages
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>> {
        return risoDao.getMessagesForSession(sessionId)
    }

    suspend fun addMessage(message: ChatMessage) {
        risoDao.insertMessage(message)
    }

    // Pending Actions (Planning Mode)
    val allPendingActions: Flow<List<PendingAction>> = risoDao.getAllPendingActionsReactive()

    suspend fun createPendingAction(messageId: String, functionName: String, argumentsJson: String, details: String): PendingAction {
        val action = PendingAction(
            messageId = messageId,
            functionName = functionName,
            argumentsJson = argumentsJson,
            status = "PENDING",
            details = details
        )
        risoDao.insertPendingAction(action)
        return action
    }

    suspend fun updatePendingActionStatus(actionId: String, status: String) {
        risoDao.updatePendingActionStatus(actionId, status)
    }

    suspend fun getPendingActionByMessageId(messageId: String): PendingAction? {
        return risoDao.getPendingActionForMessage(messageId)
    }

    // App Settings Keys
    suspend fun getSetting(key: String): String? {
        return risoDao.getSettingValue(key)
    }

    suspend fun saveSetting(key: String, value: String) {
        risoDao.insertSetting(AppSetting(key, value))
    }

    suspend fun deleteSetting(key: String) {
        risoDao.deleteSetting(key)
    }

    suspend fun clearAllUserData() {
        risoDao.deleteAllMessages()
        risoDao.deleteAllSessions()
        risoDao.deleteAllPendingActions()
        risoDao.deleteAllSettings()
    }

    // Reactive settings map Helper
    val allSettings: Flow<Map<String, String>> = risoDao.getAllSettingsReactive().map { list ->
        list.associate { it.key to it.value }
    }
}
