package com.firstapp.langtranslate.utils

import android.content.Context

/**
 * Handles voice commands for hands-free control
 * Commands: "start translation", "stop translation", "switch mode", etc.
 */
class VoiceCommandHandler(private val context: Context) {

    private val commands = mapOf(
        "start" to Command.START,
        "stop" to Command.STOP,
        "pause" to Command.PAUSE,
        "resume" to Command.RESUME,
        "voice mode" to Command.VOICE_MODE,
        "camera mode" to Command.CAMERA_MODE,
        "photo mode" to Command.PHOTO_MODE,
        "conversation mode" to Command.CONVERSATION_MODE,
        "swap languages" to Command.SWAP_LANGUAGES,
        "repeat" to Command.REPEAT
    )

    enum class Command {
        START,
        STOP,
        PAUSE,
        RESUME,
        VOICE_MODE,
        CAMERA_MODE,
        PHOTO_MODE,
        CONVERSATION_MODE,
        SWAP_LANGUAGES,
        REPEAT,
        UNKNOWN
    }

    /**
     * Parse voice input and extract command
     */
    fun parseCommand(text: String): Command {
        val normalized = text.lowercase().trim()

        for ((keyword, command) in commands) {
            if (normalized.contains(keyword)) {
                return command
            }
        }

        return Command.UNKNOWN
    }

    /**
     * Check if text contains a wake word
     */
    fun containsWakeWord(text: String): Boolean {
        val wakeWords = listOf("translate", "translator", "lang translate")
        val normalized = text.lowercase()

        return wakeWords.any { normalized.contains(it) }
    }

    /**
     * Get command description
     */
    fun getCommandDescription(command: Command): String {
        return when (command) {
            Command.START -> "Starting translation"
            Command.STOP -> "Stopping translation"
            Command.PAUSE -> "Pausing translation"
            Command.RESUME -> "Resuming translation"
            Command.VOICE_MODE -> "Switching to voice mode"
            Command.CAMERA_MODE -> "Switching to camera mode"
            Command.PHOTO_MODE -> "Switching to photo mode"
            Command.CONVERSATION_MODE -> "Switching to conversation mode"
            Command.SWAP_LANGUAGES -> "Swapping languages"
            Command.REPEAT -> "Repeating last translation"
            Command.UNKNOWN -> "Unknown command"
        }
    }
}
