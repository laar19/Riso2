package com.example.service.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class AudioRecorderHelper(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentAudioFile: File? = null
    private var isRecording = false

    fun isRecording(): Boolean = isRecording

    fun startRecording(): Result<File> {
        return try {
            if (isRecording) {
                stopRecording()
            }

            val outputFile = File(context.cacheDir, "voice_input_${System.currentTimeMillis()}.m4a")
            currentAudioFile = outputFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            isRecording = true
            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e("AudioRecorderHelper", "Failed to start audio recording", e)
            cleanUp()
            Result.failure(e)
        }
    }

    fun stopRecording(): File? {
        if (!isRecording) return null
        return try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    Log.w("AudioRecorderHelper", "Error stopping recorder (audio might be too short)", e)
                }
                release()
            }
            mediaRecorder = null
            isRecording = false

            val file = currentAudioFile
            if (file != null && file.exists() && file.length() > 0) {
                file
            } else {
                file?.delete()
                null
            }
        } catch (e: Exception) {
            Log.e("AudioRecorderHelper", "Error finalizing recording", e)
            cleanUp()
            null
        }
    }

    fun cancelRecording() {
        cleanUp()
    }

    private fun cleanUp() {
        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (_: Exception) {}
                release()
            }
        } catch (_: Exception) {}
        mediaRecorder = null
        isRecording = false
        try {
            currentAudioFile?.delete()
        } catch (_: Exception) {}
        currentAudioFile = null
    }
}
