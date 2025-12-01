package com.firstapp.langtranslate.utils

import android.media.AudioFormat
import kotlin.math.sqrt

/**
 * Utility functions for audio processing
 */
object AudioUtils {

    /**
     * Calculate RMS (Root Mean Square) energy of audio buffer
     */
    fun calculateRMS(audioBuffer: ShortArray, size: Int): Double {
        var sum = 0.0
        for (i in 0 until size) {
            sum += (audioBuffer[i] * audioBuffer[i]).toDouble()
        }
        return sqrt(sum / size)
    }

    /**
     * Calculate decibel level
     */
    fun calculateDB(audioBuffer: ShortArray, size: Int): Double {
        val rms = calculateRMS(audioBuffer, size)
        return if (rms > 0) {
            20 * kotlin.math.log10(rms)
        } else {
            0.0
        }
    }

    /**
     * Detect voice activity
     */
    fun detectVoiceActivity(
        audioBuffer: ShortArray,
        size: Int,
        threshold: Double = 1000000.0
    ): Boolean {
        var energy = 0.0
        for (i in 0 until size) {
            energy += (audioBuffer[i] * audioBuffer[i]).toDouble()
        }
        energy /= size
        return energy > threshold
    }

    /**
     * Apply noise gate
     */
    fun applyNoiseGate(audioBuffer: ShortArray, threshold: Short): ShortArray {
        return audioBuffer.map { sample ->
            if (kotlin.math.abs(sample.toInt()) < threshold) {
                0.toShort()
            } else {
                sample
            }
        }.toShortArray()
    }

    /**
     * Normalize audio levels
     */
    fun normalizeAudio(audioBuffer: ShortArray): ShortArray {
        val maxAbs = audioBuffer.maxOfOrNull { kotlin.math.abs(it.toInt()) } ?: 1
        if (maxAbs == 0) return audioBuffer

        val scaleFactor = Short.MAX_VALUE.toFloat() / maxAbs
        return audioBuffer.map { sample ->
            (sample * scaleFactor).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }.toShortArray()
    }

    /**
     * Resample audio
     */
    fun resample(
        inputBuffer: ShortArray,
        inputSampleRate: Int,
        outputSampleRate: Int
    ): ShortArray {
        if (inputSampleRate == outputSampleRate) return inputBuffer

        val ratio = outputSampleRate.toFloat() / inputSampleRate.toFloat()
        val outputSize = (inputBuffer.size * ratio).toInt()
        val outputBuffer = ShortArray(outputSize)

        for (i in outputBuffer.indices) {
            val inputIndex = (i / ratio).toInt()
            outputBuffer[i] = if (inputIndex < inputBuffer.size) {
                inputBuffer[inputIndex]
            } else {
                0
            }
        }

        return outputBuffer
    }

    /**
     * Convert float buffer to short buffer
     */
    fun floatToShort(floatBuffer: FloatArray): ShortArray {
        return floatBuffer.map { sample ->
            (sample * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }.toShortArray()
    }

    /**
     * Convert short buffer to float buffer
     */
    fun shortToFloat(shortBuffer: ShortArray): FloatArray {
        return shortBuffer.map { sample ->
            sample.toFloat() / Short.MAX_VALUE
        }.toFloatArray()
    }

    /**
     * Get channel configuration from channel count
     */
    fun getChannelConfig(channelCount: Int): Int {
        return when (channelCount) {
            1 -> AudioFormat.CHANNEL_IN_MONO
            2 -> AudioFormat.CHANNEL_IN_STEREO
            else -> AudioFormat.CHANNEL_IN_MONO
        }
    }

    /**
     * Calculate optimal buffer size
     */
    fun calculateBufferSize(
        sampleRate: Int,
        channelConfig: Int,
        audioFormat: Int,
        durationMs: Int = 100
    ): Int {
        val minBufferSize = android.media.AudioRecord.getMinBufferSize(
            sampleRate,
            channelConfig,
            audioFormat
        )

        val desiredBufferSize = (sampleRate * durationMs / 1000) *
                (if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) 2 else 1)

        return maxOf(minBufferSize, desiredBufferSize)
    }
}
