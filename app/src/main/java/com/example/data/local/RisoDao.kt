package com.example.data.local

import androidx.room.*
import com.example.data.model.ChatSession
import com.example.data.model.ChatMessage
import com.example.data.model.PendingAction
import com.example.data.model.AppSetting
import kotlinx.coroutines.flow.Flow

@Dao
interface RisoDao {

    // Chat Sessions
    @Query("SELECT * FROM chat_sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<ChatSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession)

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: String)

    // Chat Messages
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesBySessionId(sessionId: String)

    // Pending Actions (Planning Mode)
    @Query("SELECT * FROM pending_actions")
    fun getAllPendingActionsReactive(): Flow<List<PendingAction>>

    @Query("SELECT * FROM pending_actions WHERE id = :actionId LIMIT 1")
    suspend fun getPendingActionById(actionId: String): PendingAction?

    @Query("SELECT * FROM pending_actions WHERE messageId = :messageId LIMIT 1")
    suspend fun getPendingActionForMessage(messageId: String): PendingAction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingAction(action: PendingAction)

    @Query("UPDATE pending_actions SET status = :status WHERE id = :actionId")
    suspend fun updatePendingActionStatus(actionId: String, status: String)

    // App Settings
    @Query("SELECT * FROM app_settings")
    fun getAllSettingsReactive(): Flow<List<AppSetting>>

    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSettingValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSetting)

    @Query("DELETE FROM app_settings WHERE `key` = :key")
    suspend fun deleteSetting(key: String)
}
