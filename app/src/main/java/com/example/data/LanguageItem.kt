package com.example.data

import java.util.Locale

data class LanguageItem(
    val code: String,          // e.g. "es", "de", "en"
    val localeTag: String,     // e.g. "es-ES", "de-DE", "en-US"
    val displayName: String,   // e.g. "Español", "Alemán"
    val flagEmoji: String,     // e.g. "🇪🇸", "🇩🇪"
    val locale: Locale
) {
    companion object {
        val SUPPORTED_LANGUAGES = listOf(
            LanguageItem("es", "es-ES", "Español", "🇪🇸", Locale("es", "ES")),
            LanguageItem("de", "de-DE", "Alemán", "🇩🇪", Locale.GERMAN),
            LanguageItem("en", "en-US", "Inglés", "🇺🇸", Locale.US),
            LanguageItem("fr", "fr-FR", "Francés", "🇫🇷", Locale.FRENCH),
            LanguageItem("it", "it-IT", "Italiano", "🇮🇹", Locale.ITALIAN),
            LanguageItem("pt", "pt-BR", "Portugués", "🇧🇷", Locale("pt", "BR")),
            LanguageItem("ja", "ja-JP", "Japonés", "🇯🇵", Locale.JAPANESE),
            LanguageItem("hi", "hi-IN", "Hindi", "🇮🇳", Locale("hi", "IN")),
            LanguageItem("ar", "ar-SA", "Árabe", "🇸🇦", Locale("ar", "SA")),
            LanguageItem("zh", "zh-CN", "Chino", "🇨🇳", Locale.SIMPLIFIED_CHINESE)
        )

        fun findByCode(code: String): LanguageItem {
            val prefix = code.split("-", "_")[0].lowercase()
            return SUPPORTED_LANGUAGES.firstOrNull { it.code.lowercase() == prefix }
                ?: SUPPORTED_LANGUAGES.first()
        }

        fun findByLocaleTag(tag: String): LanguageItem {
            return SUPPORTED_LANGUAGES.firstOrNull { it.localeTag.equals(tag, ignoreCase = true) }
                ?: findByCode(tag)
        }
    }
}
