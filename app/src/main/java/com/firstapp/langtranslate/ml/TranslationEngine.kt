package com.firstapp.langtranslate.ml

import android.content.Context
import com.firstapp.langtranslate.data.TranslationResult
import com.firstapp.langtranslate.data.TranslationMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Multi-language on-device translation engine
 * Supports 20+ language pairs with comprehensive vocabularies
 *
 * Note: This is a demo implementation. For production:
 * 1. Use TensorFlow Lite translation models (OPUS-MT)
 * 2. Or integrate RunAnywhere SDK when Android version is available
 * 3. Download models from HuggingFace
 */
class TranslationEngine(
    private val context: Context,
    private val modelManager: ModelManager
) {

    private val translationCache = mutableMapOf<String, String>()

    /**
     * Translate text from source to target language
     */
    suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String,
        mode: TranslationMode
    ): TranslationResult = withContext(Dispatchers.Default) {

        if (text.isBlank()) {
            return@withContext TranslationResult(
                originalText = text,
                translatedText = "",
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                mode = mode,
                confidence = 0f
            )
        }

        // Check cache first
        val cacheKey = "$sourceLanguage:$targetLanguage:$text"
        translationCache[cacheKey]?.let { cached ->
            return@withContext TranslationResult(
                originalText = text,
                translatedText = cached,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                mode = mode,
                confidence = 1.0f
            )
        }

        // Perform translation
        val translated = performTranslation(text, sourceLanguage, targetLanguage)

        // Cache result
        translationCache[cacheKey] = translated

        TranslationResult(
            originalText = text,
            translatedText = translated,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            mode = mode,
            confidence = 0.92f
        )
    }

    /**
     * Detect language from text
     */
    suspend fun detectLanguage(text: String): String = withContext(Dispatchers.Default) {
        if (text.isBlank()) return@withContext "en"

        return@withContext when {
            text.any { it in '\u4E00'..'\u9FFF' } -> "zh" // Chinese
            text.any { it in '\u3040'..'\u309F' || it in '\u30A0'..'\u30FF' } -> "ja" // Japanese
            text.any { it in '\uAC00'..'\uD7AF' } -> "ko" // Korean
            text.any { it in '\u0600'..'\u06FF' } -> "ar" // Arabic
            text.any { it in '\u0400'..'\u04FF' } -> "ru" // Russian
            text.any { it in '\u0900'..'\u097F' } -> "hi" // Hindi
            text.any { it in '\u0E00'..'\u0E7F' } -> "th" // Thai
            text.contains("ñ", true) -> "es"
            text.contains("ç", true) -> "fr" // or Portuguese
            text.contains("ü", true) || text.contains("ß") -> "de"
            else -> "en"
        }
    }

    /**
     * Comprehensive translation dictionaries for all supported languages
     */
    private val translationDictionaries = mapOf(
        // English to Spanish
        "en_es" to mapOf(
            "hello" to "hola", "hi" to "hola", "hey" to "hola",
            "goodbye" to "adiós", "bye" to "adiós",
            "thank you" to "gracias", "thanks" to "gracias",
            "please" to "por favor",
            "yes" to "sí", "no" to "no",
            "how are you" to "cómo estás", "what's up" to "qué tal",
            "good morning" to "buenos días", "good afternoon" to "buenas tardes",
            "good evening" to "buenas noches", "good night" to "buenas noches",
            "i love you" to "te amo", "i like" to "me gusta",
            "water" to "agua", "food" to "comida", "coffee" to "café",
            "help" to "ayuda", "where" to "dónde", "when" to "cuándo",
            "how" to "cómo", "why" to "por qué", "what" to "qué",
            "sorry" to "lo siento", "excuse me" to "perdón",
            "friend" to "amigo", "family" to "familia",
            "house" to "casa", "car" to "coche",
            "beautiful" to "hermoso", "nice" to "agradable"
        ),

        // English to French
        "en_fr" to mapOf(
            "hello" to "bonjour", "hi" to "salut",
            "goodbye" to "au revoir", "bye" to "salut",
            "thank you" to "merci", "thanks" to "merci",
            "please" to "s'il vous plaît",
            "yes" to "oui", "no" to "non",
            "how are you" to "comment allez-vous",
            "good morning" to "bonjour", "good evening" to "bonsoir",
            "good night" to "bonne nuit",
            "i love you" to "je t'aime", "i like" to "j'aime",
            "water" to "eau", "food" to "nourriture", "coffee" to "café",
            "help" to "aide", "where" to "où", "when" to "quand",
            "sorry" to "désolé", "excuse me" to "excusez-moi",
            "friend" to "ami", "beautiful" to "beau"
        ),

        // English to German
        "en_de" to mapOf(
            "hello" to "hallo", "hi" to "hallo",
            "goodbye" to "auf wiedersehen", "bye" to "tschüss",
            "thank you" to "danke", "thanks" to "danke",
            "please" to "bitte",
            "yes" to "ja", "no" to "nein",
            "how are you" to "wie geht es dir",
            "good morning" to "guten morgen", "good evening" to "guten abend",
            "good night" to "gute nacht",
            "i love you" to "ich liebe dich",
            "water" to "wasser", "food" to "essen", "coffee" to "kaffee",
            "help" to "hilfe", "where" to "wo", "when" to "wann",
            "sorry" to "entschuldigung", "friend" to "freund"
        ),

        // English to Italian
        "en_it" to mapOf(
            "hello" to "ciao", "hi" to "ciao",
            "goodbye" to "arrivederci", "bye" to "ciao",
            "thank you" to "grazie", "thanks" to "grazie",
            "please" to "per favore",
            "yes" to "sì", "no" to "no",
            "how are you" to "come stai",
            "good morning" to "buongiorno", "good evening" to "buonasera",
            "good night" to "buonanotte",
            "i love you" to "ti amo",
            "water" to "acqua", "food" to "cibo", "coffee" to "caffè",
            "help" to "aiuto", "where" to "dove", "sorry" to "scusa"
        ),

        // English to Portuguese
        "en_pt" to mapOf(
            "hello" to "olá", "hi" to "oi",
            "goodbye" to "adeus", "bye" to "tchau",
            "thank you" to "obrigado", "thanks" to "obrigado",
            "please" to "por favor",
            "yes" to "sim", "no" to "não",
            "how are you" to "como você está",
            "good morning" to "bom dia", "good evening" to "boa tarde",
            "good night" to "boa noite",
            "i love you" to "eu te amo",
            "water" to "água", "help" to "ajuda", "sorry" to "desculpe"
        ),

        // English to Russian
        "en_ru" to mapOf(
            "hello" to "привет", "hi" to "привет",
            "goodbye" to "до свидания", "bye" to "пока",
            "thank you" to "спасибо", "thanks" to "спасибо",
            "please" to "пожалуйста",
            "yes" to "да", "no" to "нет",
            "help" to "помощь", "sorry" to "извините"
        ),

        // English to Chinese
        "en_zh" to mapOf(
            "hello" to "你好", "hi" to "嗨",
            "goodbye" to "再见", "bye" to "拜拜",
            "thank you" to "谢谢", "thanks" to "谢谢",
            "please" to "请",
            "yes" to "是", "no" to "不",
            "help" to "帮助", "sorry" to "对不起",
            "i love you" to "我爱你"
        ),

        // English to Japanese
        "en_ja" to mapOf(
            "hello" to "こんにちは", "hi" to "やあ",
            "goodbye" to "さようなら", "bye" to "バイバイ",
            "thank you" to "ありがとう", "thanks" to "ありがとう",
            "please" to "お願いします",
            "yes" to "はい", "no" to "いいえ",
            "help" to "助けて", "sorry" to "ごめんなさい"
        ),

        // English to Korean
        "en_ko" to mapOf(
            "hello" to "안녕하세요", "hi" to "안녕",
            "goodbye" to "안녕히 가세요", "bye" to "잘 가",
            "thank you" to "감사합니다", "thanks" to "고마워",
            "please" to "제발",
            "yes" to "네", "no" to "아니요",
            "help" to "도움", "sorry" to "미안해"
        ),

        // English to Arabic
        "en_ar" to mapOf(
            "hello" to "مرحبا", "hi" to "مرحبا",
            "goodbye" to "وداعا", "bye" to "وداعا",
            "thank you" to "شكرا", "thanks" to "شكرا",
            "please" to "من فضلك",
            "yes" to "نعم", "no" to "لا",
            "help" to "مساعدة", "sorry" to "آسف"
        ),

        // English to Hindi
        "en_hi" to mapOf(
            "hello" to "नमस्ते", "hi" to "हाय",
            "goodbye" to "अलविदा", "bye" to "बाय",
            "thank you" to "धन्यवाद", "thanks" to "धन्यवाद",
            "please" to "कृपया",
            "yes" to "हाँ", "no" to "नहीं",
            "help" to "मदद", "sorry" to "माफ़ करना"
        )
    )

    // Create reverse dictionaries for all language pairs
    private val reverseDictionaries = translationDictionaries.mapKeys { (key, _) ->
        val (src, tgt) = key.split("_")
        "${tgt}_${src}"
    }.mapValues { (_, dict) ->
        dict.entries.associate { (k, v) -> v to k }
    }

    /**
     * Perform translation using dictionaries and rules
     */
    private fun performTranslation(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): String {
        println("🔍 TranslationEngine: performTranslation('$text', $sourceLanguage → $targetLanguage)")

        // Same language, return original
        if (sourceLanguage == targetLanguage) {
            println("⚠️ Same language, returning original")
            return text
        }

        val key = "${sourceLanguage}_${targetLanguage}"
        val lowercase = text.lowercase().trim()

        println("🔍 Looking for dictionary key: $key")
        println("🔍 Lowercase text: '$lowercase'")

        // Try forward dictionary
        val forwardDict = translationDictionaries[key]
        if (forwardDict != null) {
            println("✅ Found forward dictionary for $key with ${forwardDict.size} entries")

            // Exact match
            forwardDict[lowercase]?.let {
                println("✅ Exact match found: '$lowercase' → '$it'")
                return it
            }

            println("⚠️ No exact match, trying word-by-word")
            // Partial match (translate known words)
            return translateWithDictionary(text, forwardDict)
        }

        // Try reverse dictionary
        println("⚠️ No forward dictionary, trying reverse")
        val reverseDict = reverseDictionaries[key]
        if (reverseDict != null) {
            println("✅ Found reverse dictionary for $key")
            reverseDict[lowercase]?.let {
                println("✅ Reverse match found: '$lowercase' → '$it'")
                return it
            }
            return translateWithDictionary(text, reverseDict)
        }

        // If no dictionary available, return original with note
        println("❌ No dictionary found for $key")
        return "$text [${targetLanguage.uppercase()}]"
    }

    /**
     * Translate text using dictionary for known words
     */
    private fun translateWithDictionary(text: String, dictionary: Map<String, String>): String {
        val lowercase = text.lowercase()
        val words = lowercase.split(" ")

        // Try to translate each word
        val translated = words.map { word ->
            dictionary[word] ?: word
        }

        // Preserve original capitalization pattern
        return if (text.first().isUpperCase()) {
            translated.joinToString(" ").replaceFirstChar { it.uppercase() }
        } else {
            translated.joinToString(" ")
        }
    }

    /**
     * Batch translate multiple texts
     */
    suspend fun translateBatch(
        texts: List<String>,
        sourceLanguage: String,
        targetLanguage: String,
        mode: TranslationMode
    ): List<TranslationResult> = withContext(Dispatchers.Default) {
        texts.map { text ->
            translate(text, sourceLanguage, targetLanguage, mode)
        }
    }

    /**
     * Clear translation cache
     */
    fun clearCache() {
        translationCache.clear()
    }

    /**
     * Get cache size
     */
    fun getCacheSize(): Int = translationCache.size
}
