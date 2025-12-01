package com.firstapp.langtranslate.data

data class Language(
    val code: String,
    val name: String,
    val nativeName: String,
    val isDownloaded: Boolean = false
)

object SupportedLanguages {
    val languages = listOf(
        Language("en", "English", "English", true),
        Language("es", "Spanish", "Español", false),
        Language("fr", "French", "Français", false),
        Language("de", "German", "Deutsch", false),
        Language("it", "Italian", "Italiano", false),
        Language("pt", "Portuguese", "Português", false),
        Language("ru", "Russian", "Русский", false),
        Language("zh", "Chinese", "中文", false),
        Language("ja", "Japanese", "日本語", false),
        Language("ko", "Korean", "한국어", false),
        Language("ar", "Arabic", "العربية", false),
        Language("hi", "Hindi", "हिन्दी", false),
        Language("nl", "Dutch", "Nederlands", false),
        Language("pl", "Polish", "Polski", false),
        Language("tr", "Turkish", "Türkçe", false),
        Language("vi", "Vietnamese", "Tiếng Việt", false),
        Language("th", "Thai", "ไทย", false),
        Language("id", "Indonesian", "Bahasa Indonesia", false),
        Language("uk", "Ukrainian", "Українська", false),
        Language("ro", "Romanian", "Română", false)
    )

    fun getLanguageByCode(code: String): Language? {
        return languages.find { it.code == code }
    }
}
