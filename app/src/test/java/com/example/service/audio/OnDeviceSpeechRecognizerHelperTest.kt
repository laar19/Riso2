package com.example.service.audio

import android.content.Context
import android.speech.RecognizerIntent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OnDeviceSpeechRecognizerHelperTest {

    @Test
    fun `buildRecognitionIntent always sets EXTRA_PREFER_OFFLINE and chosen app language`() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            val context = ApplicationProvider.getApplicationContext<Context>()
            val helper = OnDeviceSpeechRecognizerHelper(context)

            val esTag = helper.resolveLanguageTag("es")
            assertEquals("es-ES", esTag)

            val intent = helper.buildRecognitionIntent(esTag)
            assertTrue(intent.getBooleanExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false))
            assertEquals("es-ES", intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE))
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun `strictOfflineOnly blocks recognition when engineMode is SYSTEM_MAYBE_ONLINE`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = OnDeviceSpeechRecognizerHelper(context)

        var errorMsg: String? = null
        var resultMsg: String? = null
        helper.startListening(
            appLanguage = "es",
            strictOfflineOnly = true,
            onResult = { resultMsg = it },
            onError = { errorMsg = it },
            onStatus = {}
        )
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        if (helper.engineMode == OnDeviceSpeechRecognizerHelper.EngineMode.SYSTEM_MAYBE_ONLINE) {
            assertTrue(errorMsg?.contains("Solo offline estricto", ignoreCase = true) == true)
            assertEquals(null, resultMsg)
        }
    }
}
