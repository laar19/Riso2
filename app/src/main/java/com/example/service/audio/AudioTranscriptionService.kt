package com.example.service.audio

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class AudioTranscriptionService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun transcribeAudio(
        file: File,
        endpointUrl: String,
        apiKey: String,
        modelName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanKey = apiKey.trim()
            if (cleanKey.isBlank()) {
                return@withContext Result.failure(Exception("Clave API no configurada para el servicio STT."))
            }

            val raw = endpointUrl.trim().trimEnd('/')
            val targetUrl = when {
                raw.isBlank() -> "https://api.openai.com/v1/audio/transcriptions"
                raw.endsWith("/audio/transcriptions") -> raw
                raw.endsWith("/models") -> raw.removeSuffix("/models").trimEnd('/') + "/audio/transcriptions"
                else -> "$raw/audio/transcriptions"
            }

            val chosenModel = if (modelName.isNotBlank()) modelName.trim() else "whisper-large-v3-turbo"

            val mediaType = "audio/m4a".toMediaType()
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, file.asRequestBody(mediaType))
                .addFormDataPart("model", chosenModel)
                .addFormDataPart("response_format", "json")
                .build()

            val request = Request.Builder()
                .url(targetUrl)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("User-Agent", "RisoApp/1.0 (Android; okhttp)")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(responseBodyStr)
                    val text = json.optString("text", "").trim()
                    if (text.isNotBlank()) {
                        Result.success(text)
                    } else {
                        Result.failure(Exception("No se detectó voz clara en el audio."))
                    }
                } else {
                    val detail = try {
                        val errObj = JSONObject(responseBodyStr).optJSONObject("error")
                        errObj?.optString("message") ?: responseBodyStr
                    } catch (_: Exception) {
                        responseBodyStr.take(120)
                    }
                    val msg = if (detail.isNotBlank()) detail else "HTTP ${response.code}"
                    Log.e("AudioTranscription", "Transcription error $msg on $targetUrl")
                    Result.failure(Exception("Error STT ($msg)"))
                }
            }
        } catch (e: Exception) {
            Log.e("AudioTranscription", "Exception during transcription", e)
            Result.failure(e)
        }
    }
}
