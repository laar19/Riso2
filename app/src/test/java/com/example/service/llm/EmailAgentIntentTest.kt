package com.example.service.llm

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EmailAgentIntentTest {

    private fun createLlmService(): LlmService {
        return LlmService()
    }

    @Test
    fun `check my inbox triggers list_inbox tool call`() = runBlocking {
        val service = createLlmService()
        val history = listOf(
            GeminiContent(
                role = "user",
                parts = listOf(GeminiPart(text = "check my inbox"))
            )
        )

        // Without a valid API key and with mcpEmailEnabled = true, should route to list_inbox function call
        val response = service.resolveLlm(
            history = history,
            provider = "Gemini",
            customApiKey = "",
            mcpEmailEnabled = true
        )

        val candidate = response.candidates?.firstOrNull()
        assertNotNull("Should return a candidate", candidate)
        val funcCall = candidate?.content?.parts?.firstOrNull { it.functionCall != null }?.functionCall
        assertNotNull("Should contain a functionCall", funcCall)
        assertEquals("list_inbox", funcCall?.name)
    }

    @Test
    fun `spanish inbox check triggers list_inbox tool call`() = runBlocking {
        val service = createLlmService()
        val queries = listOf(
            "revisa mi bandeja de entrada",
            "ver mis correos nuevos",
            "consultar inbox",
            "¿tengo algún correo nuevo?"
        )

        for (query in queries) {
            val history = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = query))
                )
            )

            val response = service.resolveLlm(
                history = history,
                provider = "Gemini",
                customApiKey = "",
                mcpEmailEnabled = true
            )

            val funcCall = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.functionCall != null }?.functionCall
            assertNotNull("Query '$query' should trigger functionCall", funcCall)
            assertEquals("list_inbox", funcCall?.name)
        }
    }

    @Test
    fun `english variations of email queries trigger list_inbox`() = runBlocking {
        val service = createLlmService()
        val queries = listOf(
            "check my inbox",
            "check my email",
            "read my emails",
            "show my inbox",
            "list unread messages",
            "inbox"
        )

        for (query in queries) {
            val history = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = query))
                )
            )

            val response = service.resolveLlm(
                history = history,
                provider = "Gemini",
                customApiKey = "",
                mcpEmailEnabled = true
            )

            val funcCall = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.functionCall != null }?.functionCall
            assertNotNull("Query '$query' should trigger functionCall", funcCall)
            assertEquals("list_inbox", funcCall?.name)
        }
    }
}
