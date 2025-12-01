package com.firstapp.langtranslate.data

data class Language(
    val code: String,
    val name: String,
    val nativeName: String,
    val isDownloaded: Boolean = false
)

object SupportedLanguages {
    // Only 11 supported languages with translation dictionaries
    val languages = listOf(
        Language("en", "English", "English", true),
        Language("es", "Spanish", "Español", true),
        Language("fr", "French", "Français", true),
        Language("de", "German", "Deutsch", true),
        Language("it", "Italian", "Italiano", true),
        Language("pt", "Portuguese", "Português", true),
        Language("ru", "Russian", "Русский", true),
        Language("zh", "Chinese", "中文", true),
        Language("ja", "Japanese", "日本語", true),
        Language("ko", "Korean", "한국어", true),
        Language("ar", "Arabic", "العربية", true),
        Language("hi", "Hindi", "हिन्दी", true)
    )

    fun getLanguageByCode(code: String): Language? {
        return languages.find { it.code == code }
    }
}
