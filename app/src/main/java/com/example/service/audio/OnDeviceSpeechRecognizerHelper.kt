package com.example.service.audio

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.ModelDownloadListener
import android.speech.RecognitionListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import java.util.Locale

class OnDeviceSpeechRecognizerHelper(private val context: Context) {

    enum class EngineMode {
        ON_DEVICE,
        SYSTEM_MAYBE_ONLINE
    }

    private val TAG = "OnDeviceSpeech"
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var lastRecognizedText: String = ""

    var engineMode: EngineMode = detectEngineMode()
        private set

    fun isAvailable(): Boolean {
        return try {
            SpeechRecognizer.isRecognitionAvailable(context)
        } catch (_: Exception) {
            false
        }
    }

    fun isOnDeviceAvailable(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            try {
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
            } catch (_: Exception) {
                false
            }
    }

    fun detectEngineMode(): EngineMode {
        return if (isOnDeviceAvailable()) EngineMode.ON_DEVICE else EngineMode.SYSTEM_MAYBE_ONLINE
    }

    fun resolveLanguageTag(appLanguageCode: String?): String {
        val defaultLocale = Locale.getDefault()
        return when (appLanguageCode?.lowercase()?.trim()) {
            "es" -> if (defaultLocale.language == "es") defaultLocale.toLanguageTag() else "es-ES"
            "en" -> if (defaultLocale.language == "en") defaultLocale.toLanguageTag() else "en-US"
            null, "" -> defaultLocale.toLanguageTag()
            else -> appLanguageCode
        }
    }

    fun buildRecognitionIntent(languageTag: String): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            // Available since API 23
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
    }

    private fun matchesLanguage(targetTag: String, candidates: List<String>): Boolean {
        val targetBase = targetTag.substringBefore('-').lowercase()
        return candidates.any { candidate ->
            candidate.equals(targetTag, ignoreCase = true) ||
                candidate.substringBefore('-').equals(targetBase, ignoreCase = true)
        }
    }

    private fun findMatchingLanguage(targetTag: String, candidates: List<String>): String? {
        val exact = candidates.firstOrNull { it.equals(targetTag, ignoreCase = true) }
        if (exact != null) return exact
        val targetBase = targetTag.substringBefore('-').lowercase()
        return candidates.firstOrNull { it.substringBefore('-').equals(targetBase, ignoreCase = true) }
    }

    fun startListening(
        appLanguage: String? = null,
        strictOfflineOnly: Boolean = false,
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onStatus: (String) -> Unit,
        onRmsChangedLevel: (Float) -> Unit = {},
        onEngineModeChanged: (EngineMode) -> Unit = {},
        onModelDownloadAvailable: ((String) -> Unit)? = null
    ) {
        mainHandler.post {
            try {
                if (isListening) {
                    cleanUp()
                }

                lastRecognizedText = ""
                val mode = detectEngineMode()
                engineMode = mode
                onEngineModeChanged(mode)

                if (strictOfflineOnly && mode != EngineMode.ON_DEVICE) {
                    isListening = false
                    onRmsChangedLevel(0f)
                    onError("Solo offline estricto activo: este dispositivo no tiene motor de reconocimiento en el dispositivo.")
                    return@post
                }

                val recognizer = if (mode == EngineMode.ON_DEVICE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                } else {
                    SpeechRecognizer.createSpeechRecognizer(context)
                }

                speechRecognizer = recognizer

                val langTag = resolveLanguageTag(appLanguage)
                val intent = buildRecognitionIntent(langTag)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val executor = ContextCompat.getMainExecutor(context)
                    recognizer.checkRecognitionSupport(
                        intent,
                        executor,
                        object : RecognitionSupportCallback {
                            override fun onSupportResult(recognitionSupport: RecognitionSupport) {
                                val installed = recognitionSupport.installedOnDeviceLanguages
                                val supported = recognitionSupport.supportedOnDeviceLanguages
                                val pending = recognitionSupport.pendingOnDeviceLanguages

                                val isInstalled = matchesLanguage(langTag, installed)
                                val isPending = matchesLanguage(langTag, pending)
                                val supportedMatch = findMatchingLanguage(langTag, supported)

                                when {
                                    isInstalled -> {
                                        beginRecognition(
                                            recognizer = recognizer,
                                            intent = intent,
                                            mode = mode,
                                            onResult = onResult,
                                            onError = onError,
                                            onStatus = onStatus,
                                            onRmsChangedLevel = onRmsChangedLevel
                                        )
                                    }
                                    isPending -> {
                                        onStatus("Descarga del modelo de idioma ($langTag) en curso...")
                                        if (strictOfflineOnly) {
                                            isListening = false
                                            onError("Modelo de idioma ($langTag) en descarga. Espera a que finalice.")
                                            cleanUp()
                                        } else {
                                            beginRecognition(
                                                recognizer = recognizer,
                                                intent = intent,
                                                mode = mode,
                                                onResult = onResult,
                                                onError = onError,
                                                onStatus = onStatus,
                                                onRmsChangedLevel = onRmsChangedLevel
                                            )
                                        }
                                    }
                                    supportedMatch != null -> {
                                        isListening = false
                                        onRmsChangedLevel(0f)
                                        onStatus("El idioma $supportedMatch está disponible para descargar sin conexión.")
                                        onModelDownloadAvailable?.invoke(supportedMatch)
                                        if (onModelDownloadAvailable == null) {
                                            onError("El idioma $supportedMatch no está instalado offline.")
                                        }
                                        cleanUp()
                                    }
                                    else -> {
                                        if (strictOfflineOnly && mode == EngineMode.ON_DEVICE) {
                                            isListening = false
                                            onRmsChangedLevel(0f)
                                            onError("Idioma ($langTag) no disponible en el motor offline de este dispositivo.")
                                            cleanUp()
                                        } else {
                                            beginRecognition(
                                                recognizer = recognizer,
                                                intent = intent,
                                                mode = mode,
                                                onResult = onResult,
                                                onError = onError,
                                                onStatus = onStatus,
                                                onRmsChangedLevel = onRmsChangedLevel
                                            )
                                        }
                                    }
                                }
                            }

                            override fun onError(error: Int) {
                                if (BuildConfig.DEBUG) {
                                    Log.w(TAG, "checkRecognitionSupport failed: $error")
                                }
                                beginRecognition(
                                    recognizer = recognizer,
                                    intent = intent,
                                    mode = mode,
                                    onResult = onResult,
                                    onError = onError,
                                    onStatus = onStatus,
                                    onRmsChangedLevel = onRmsChangedLevel
                                )
                            }
                        }
                    )
                } else {
                    beginRecognition(
                        recognizer = recognizer,
                        intent = intent,
                        mode = mode,
                        onResult = onResult,
                        onError = onError,
                        onStatus = onStatus,
                        onRmsChangedLevel = onRmsChangedLevel
                    )
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Failed to start SpeechRecognizer", e)
                }
                isListening = false
                onRmsChangedLevel(0f)
                onError("No se pudo iniciar el reconocedor: ${e.localizedMessage}")
                cleanUp()
            }
        }
    }

    private fun beginRecognition(
        recognizer: SpeechRecognizer,
        intent: Intent,
        mode: EngineMode,
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onStatus: (String) -> Unit,
        onRmsChangedLevel: (Float) -> Unit
    ) {
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                val label = when (mode) {
                    EngineMode.ON_DEVICE -> "Escuchando (en el dispositivo)... Habla ahora"
                    EngineMode.SYSTEM_MAYBE_ONLINE -> "Escuchando (sistema)... Habla ahora"
                }
                onStatus(label)
            }

            override fun onBeginningOfSpeech() {
                onStatus("Detectando voz...")
            }

            override fun onRmsChanged(rmsdB: Float) {
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                onRmsChangedLevel(normalized)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                onRmsChangedLevel(0f)
                val procLabel = when (mode) {
                    EngineMode.ON_DEVICE -> "Procesando audio en el dispositivo..."
                    EngineMode.SYSTEM_MAYBE_ONLINE -> "Procesando audio en el sistema..."
                }
                onStatus(procLabel)
            }

            override fun onError(errorCode: Int) {
                isListening = false
                onRmsChangedLevel(0f)
                val msg = when (errorCode) {
                    SpeechRecognizer.ERROR_AUDIO -> "Error de grabación de audio"
                    SpeechRecognizer.ERROR_CLIENT -> "Error del cliente de voz"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permiso de micrófono no concedido"
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                        "Error de conexión en reconocimiento"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No se detectó voz clara"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconocedor de voz ocupado"
                    SpeechRecognizer.ERROR_SERVER -> "Error en servicio de reconocimiento"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No se detectó voz a tiempo"
                    else -> "Aviso de voz ($errorCode)"
                }
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "SpeechRecognizer error: $errorCode")
                }
                if (lastRecognizedText.isNotBlank()) {
                    onResult(lastRecognizedText)
                } else {
                    onError(msg)
                }
                cleanUp()
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                onRmsChangedLevel(0f)
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim() ?: lastRecognizedText
                if (text.isNotBlank()) {
                    onResult(text)
                } else {
                    onError("No se detectó voz en el audio")
                }
                cleanUp()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = partials?.firstOrNull()?.trim()
                if (!partial.isNullOrBlank()) {
                    lastRecognizedText = partial
                    onStatus("Reconociendo: $partial")
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer.startListening(intent)
        isListening = true
    }

    fun triggerModelDownload(
        appLanguage: String?,
        onStatus: (String) -> Unit,
        onComplete: (Boolean, String) -> Unit
    ) {
        mainHandler.post {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                onComplete(false, "La descarga de paquetes offline requiere Android 13 o superior.")
                return@post
            }

            val langTag = resolveLanguageTag(appLanguage)
            val downloadIntent = buildRecognitionIntent(langTag)
            val recognizer = try {
                if (isOnDeviceAvailable()) {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                } else {
                    SpeechRecognizer.createSpeechRecognizer(context)
                }
            } catch (e: Exception) {
                onComplete(false, "No se pudo iniciar la descarga: ${e.localizedMessage}")
                return@post
            }

            onStatus("Solicitando descarga del modelo de voz ($langTag)...")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val executor = ContextCompat.getMainExecutor(context)
                recognizer.triggerModelDownload(
                    downloadIntent,
                    executor,
                    object : ModelDownloadListener {
                        override fun onProgress(completedPercent: Int) {
                            onStatus("Descargando modelo de voz ($langTag): $completedPercent%")
                        }

                        override fun onSuccess() {
                            recognizer.destroy()
                            onComplete(true, "✓ Modelo de voz ($langTag) descargado en el dispositivo.")
                        }

                        override fun onScheduled() {
                            recognizer.destroy()
                            onComplete(true, "Descarga de modelo ($langTag) programada en segundo plano.")
                        }

                        override fun onError(error: Int) {
                            recognizer.destroy()
                            onComplete(false, "Error al descargar modelo ($langTag): código $error")
                        }
                    }
                )
            } else {
                try {
                    recognizer.triggerModelDownload(downloadIntent)
                    onComplete(true, "Descarga del modelo ($langTag) iniciada en el dispositivo.")
                } catch (e: Exception) {
                    onComplete(false, "No se pudo solicitar descarga: ${e.localizedMessage}")
                } finally {
                    recognizer.destroy()
                }
            }
        }
    }

    fun stopListening(onResult: ((String) -> Unit)? = null) {
        mainHandler.post {
            try {
                if (isListening && speechRecognizer != null) {
                    speechRecognizer?.stopListening()
                    if (onResult != null && lastRecognizedText.isNotBlank()) {
                        onResult(lastRecognizedText)
                    }
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Error stopping SpeechRecognizer", e)
                }
            } finally {
                isListening = false
            }
        }
    }

    fun cancel() {
        mainHandler.post { cleanUp() }
    }

    private fun cleanUp() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        isListening = false
    }
}
