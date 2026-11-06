package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.RisoDao
import com.example.data.model.ChatSession
import com.example.data.model.ChatMessage
import com.example.data.model.PendingAction
import com.example.data.model.AppSetting
import com.example.data.secure.SecureStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class RisoRepository(
    private val risoDao: RisoDao,
    val secureStore: SecureStore
) {
    constructor(risoDao: RisoDao, context: Context) : this(risoDao, SecureStore(context))

    private val secretUpdates = MutableStateFlow(0L)

    companion object {
        private const val TAG = "RisoRepository"
        const val MIGRATION_FLAG = "secrets_migrated_v1"

        private val SENSITIVE_KEYS = setOf(
            "email_password",
            "whisper_api_key",
            "brave_search_api_key",
            "github_pat",
            "gitlab_pat",
            "github_token"
        )
    }

    private fun isSensitiveKey(key: String): Boolean {
        return SENSITIVE_KEYS.contains(key) ||
                key.startsWith("email_pass_") ||
                key.startsWith("llm_key_") ||
                key.startsWith("stt_key_") ||
                key.startsWith("github_token_") ||
                key.startsWith("gitlab_token_")
    }

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

    // SecureStore Operations
    suspend fun saveSecret(key: String, value: String) {
        secureStore.putSecret(key, value)
        // Ensure secret is wiped from Room if it existed previously
        risoDao.deleteSetting(key)
        secretUpdates.value = System.currentTimeMillis()
    }

    suspend fun getSecret(key: String): String? {
        return secureStore.getSecret(key)
    }

    suspend fun deleteSecret(key: String) {
        secureStore.deleteSecret(key)
        risoDao.deleteSetting(key)
        secretUpdates.value = System.currentTimeMillis()
    }

    suspend fun wipeSecureStore() {
        secureStore.wipeAll()
        secretUpdates.value = System.currentTimeMillis()
    }

    // App Settings Keys
    suspend fun getSetting(key: String): String? {
        if (isSensitiveKey(key)) {
            val secret = secureStore.getSecret(key)
            if (secret != null) return secret
        }
        return risoDao.getSettingValue(key)
    }

    suspend fun saveSetting(key: String, value: String) {
        if (isSensitiveKey(key)) {
            saveSecret(key, value)
            return
        }
        risoDao.insertSetting(AppSetting(key, value))
    }

    suspend fun deleteSetting(key: String) {
        if (isSensitiveKey(key)) {
            deleteSecret(key)
            return
        }
        risoDao.deleteSetting(key)
    }

    suspend fun clearAllUserData() {
        risoDao.deleteAllMessages()
        risoDao.deleteAllSessions()
        risoDao.deleteAllPendingActions()
        risoDao.deleteAllSettings()
        secureStore.wipeAll()
        secretUpdates.value = System.currentTimeMillis()
    }

    /**
     * One-time migration on startup:
     * If sensitive values exist in app_settings (Room), moves them to SecureStore and deletes them from Room.
     * Sets "secrets_migrated_v1" = "true".
     */
    suspend fun migrateSecretsIfNeeded() {
        try {
            val isMigrated = risoDao.getSettingValue(MIGRATION_FLAG)
            if (isMigrated == "true") {
                return
            }

            if (com.example.BuildConfig.DEBUG) {
                Log.i(TAG, "migrateSecretsIfNeeded")
            }

            // 1. Single sensitive keys
            val singleKeys = listOf(
                "email_password",
                "whisper_api_key",
                "brave_search_api_key",
                "github_pat",
                "gitlab_pat",
                "github_token"
            )
            for (key in singleKeys) {
                val value = risoDao.getSettingValue(key)
                if (!value.isNullOrBlank()) {
                    secureStore.putSecret(key, value)
                    risoDao.deleteSetting(key)
                }
            }

            // 2. Email accounts (strip passwordVal, store as email_pass_{id})
            val emailAccountsJson = risoDao.getSettingValue("email_accounts_json")
            if (!emailAccountsJson.isNullOrBlank()) {
                try {
                    val arr = JSONArray(emailAccountsJson)
                    var modified = false
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.optString("id")
                        val pass = obj.optString("passwordVal")
                        if (id.isNotBlank() && pass.isNotBlank()) {
                            secureStore.putSecret("email_pass_$id", pass)
                            obj.put("passwordVal", "")
                            modified = true
                        }
                    }
                    if (modified) {
                        risoDao.insertSetting(AppSetting("email_accounts_json", arr.toString()))
                        if (com.example.BuildConfig.DEBUG) {
                            Log.d(TAG, "migrateEmailAccounts")
                        }
                    }
                } catch (e: Exception) {
                    if (com.example.BuildConfig.DEBUG) {
                        Log.e(TAG, "Error migrating email_accounts_json", e)
                    }
                }
            }

            // 3. LLM Profiles (strip apiKey, store as llm_key_{id})
            val llmProfilesJson = risoDao.getSettingValue("llm_profiles_json")
            if (!llmProfilesJson.isNullOrBlank()) {
                try {
                    val arr = JSONArray(llmProfilesJson)
                    var modified = false
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.optString("id")
                        val apiKey = obj.optString("apiKey")
                        if (id.isNotBlank() && apiKey.isNotBlank()) {
                            secureStore.putSecret("llm_key_$id", apiKey)
                            obj.put("apiKey", "")
                            modified = true
                        }
                    }
                    if (modified) {
                        risoDao.insertSetting(AppSetting("llm_profiles_json", arr.toString()))
                        if (com.example.BuildConfig.DEBUG) {
                            Log.d(TAG, "migrateLlmProfiles")
                        }
                    }
                } catch (e: Exception) {
                    if (com.example.BuildConfig.DEBUG) {
                        Log.e(TAG, "Error migrating llm_profiles_json", e)
                    }
                }
            }

            // 4. STT Profiles (strip apiKey, store as stt_key_{id})
            val sttProfilesJson = risoDao.getSettingValue("stt_profiles_json")
            if (!sttProfilesJson.isNullOrBlank()) {
                try {
                    val arr = JSONArray(sttProfilesJson)
                    var modified = false
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.optString("id")
                        val apiKey = obj.optString("apiKey")
                        if (id.isNotBlank() && apiKey.isNotBlank()) {
                            secureStore.putSecret("stt_key_$id", apiKey)
                            obj.put("apiKey", "")
                            modified = true
                        }
                    }
                    if (modified) {
                        risoDao.insertSetting(AppSetting("stt_profiles_json", arr.toString()))
                        if (com.example.BuildConfig.DEBUG) {
                            Log.d(TAG, "migrateSttProfiles")
                        }
                    }
                } catch (e: Exception) {
                    if (com.example.BuildConfig.DEBUG) {
                        Log.e(TAG, "Error migrating stt_profiles_json", e)
                    }
                }
            }

            // 5. GitHub Accounts (strip token, store as github_token_{id})
            val githubAccountsJson = risoDao.getSettingValue("github_accounts_json")
            if (!githubAccountsJson.isNullOrBlank()) {
                try {
                    val arr = JSONArray(githubAccountsJson)
                    var modified = false
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.optString("id")
                        val token = obj.optString("token")
                        if (id.isNotBlank() && token.isNotBlank()) {
                            secureStore.putSecret("github_token_$id", token)
                            obj.put("token", "")
                            modified = true
                        }
                    }
                    if (modified) {
                        risoDao.insertSetting(AppSetting("github_accounts_json", arr.toString()))
                        if (com.example.BuildConfig.DEBUG) {
                            Log.d(TAG, "migrateGithubAccounts")
                        }
                    }
                } catch (e: Exception) {
                    if (com.example.BuildConfig.DEBUG) {
                        Log.e(TAG, "Error migrating github_accounts_json", e)
                    }
                }
            }

            // 6. GitLab Accounts (strip token, store as gitlab_token_{id})
            val gitlabAccountsJson = risoDao.getSettingValue("gitlab_accounts_json")
            if (!gitlabAccountsJson.isNullOrBlank()) {
                try {
                    val arr = JSONArray(gitlabAccountsJson)
                    var modified = false
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.optString("id")
                        val token = obj.optString("token")
                        if (id.isNotBlank() && token.isNotBlank()) {
                            secureStore.putSecret("gitlab_token_$id", token)
                            obj.put("token", "")
                            modified = true
                        }
                    }
                    if (modified) {
                        risoDao.insertSetting(AppSetting("gitlab_accounts_json", arr.toString()))
                        if (com.example.BuildConfig.DEBUG) {
                            Log.d(TAG, "migrateGitlabAccounts")
                        }
                    }
                } catch (e: Exception) {
                    if (com.example.BuildConfig.DEBUG) {
                        Log.e(TAG, "Error migrating gitlab_accounts_json", e)
                    }
                }
            }

            // Mark migration completed
            risoDao.insertSetting(AppSetting(MIGRATION_FLAG, "true"))
            secretUpdates.value = System.currentTimeMillis()
            if (com.example.BuildConfig.DEBUG) {
                Log.i(TAG, "migrateSecretsIfNeeded: completed")
            }
        } catch (e: Exception) {
            if (com.example.BuildConfig.DEBUG) {
                Log.e(TAG, "Failed during migrateSecretsIfNeeded", e)
            }
        }
    }

    // Reactive settings map Helper (merges non-sensitive Room settings with SecureStore keys for UI reactivity)
    val allSettings: Flow<Map<String, String>> = combine(
        risoDao.getAllSettingsReactive(),
        secretUpdates
    ) { list, _ ->
        val map = list.associate { it.key to it.value }.toMutableMap()
        // Inject sensitive keys from SecureStore if present
        secureStore.getSecret("brave_search_api_key")?.let { map["brave_search_api_key"] = it }
        secureStore.getSecret("whisper_api_key")?.let { map["whisper_api_key"] = it }
        secureStore.getSecret("email_password")?.let { map["email_password"] = it }
        secureStore.getSecret("github_pat")?.let { map["github_pat"] = it }
        secureStore.getSecret("gitlab_pat")?.let { map["gitlab_pat"] = it }
        secureStore.getSecret("github_token")?.let { map["github_token"] = it }
        map
    }
}
