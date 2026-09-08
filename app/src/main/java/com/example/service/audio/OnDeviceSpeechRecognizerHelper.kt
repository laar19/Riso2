package com.example.service.audio

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class OnDeviceSpeechRecognizerHelper(private val context: Context) {
    private val TAG = "OnDeviceSpeech"
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var lastRecognizedText: String = ""

    fun isAvailable(): Boolean {
        return try {
            SpeechRecognizer.isRecognitionAvailable(context)
        } catch (_: Exception) {
            false
        }
    }

    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onStatus: (String) -> Unit
    ) {
        mainHandler.post {
            try {
                if (isListening) {
                    cleanUp()
                }

                lastRecognizedText = ""

                val recognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
                ) {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                } else {
                    SpeechRecognizer.createSpeechRecognizer(context)
                }

                speechRecognizer = recognizer

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    }
                }

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        onStatus("Escuchando (Whisper Local)... Habla ahora")
                    }

                    override fun onBeginningOfSpeech() {
                        onStatus("Detectando voz...")
                    }

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        onStatus("Procesando audio localmente...")
                    }

                    override fun onError(errorCode: Int) {
                        isListening = false
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
                        Log.w(TAG, "SpeechRecognizer error: $errorCode - $msg")
                        if (lastRecognizedText.isNotBlank()) {
                            onResult(lastRecognizedText)
                        } else {
                            onError(msg)
                        }
                        cleanUp()
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
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
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start SpeechRecognizer", e)
                isListening = false
                onError("No se pudo iniciar el reconocedor local: ${e.localizedMessage}")
                cleanUp()
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
                Log.e(TAG, "Error stopping SpeechRecognizer", e)
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
