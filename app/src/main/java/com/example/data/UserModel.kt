package com.example.data

data class UserProfile(
    val id: String,
    val alias: String,
    val email: String,
    val nativeLanguageCode: String,
    val avatarEmoji: String = "👤",
    val photoUri: String? = null,
    val bio: String = "Explorador de idiomas y comunicación en tiempo real."
) {
    val nativeLanguage: LanguageItem
        get() = LanguageItem.findByCode(nativeLanguageCode)

    companion object {
        val PRESET_USERS = listOf(
            UserProfile(
                id = "2",
                alias = "Diego",
                email = "diego@compuapps.com",
                nativeLanguageCode = "es",
                avatarEmoji = "🧑‍💻",
                bio = "Desarrollador y apasionado de los idiomas. Busco practicar alemán y compartir español."
            ),
            UserProfile(
                id = "8",
                alias = "Hans",
                email = "hans.muller@berlin.de",
                nativeLanguageCode = "de",
                avatarEmoji = "👨‍🔬",
                bio = "Wissenschaftler aus Berlin. Ich lerne gerne Spanisch und liebe Technologie."
            ),
            UserProfile(
                id = "5",
                alias = "Sarah",
                email = "sarah.smith@california.com",
                nativeLanguageCode = "en",
                avatarEmoji = "👩‍🎨",
                bio = "Graphic designer from California. Loving cross-cultural live audio translations!"
            ),
            UserProfile(
                id = "14",
                alias = "Camille",
                email = "camille@paris.fr",
                nativeLanguageCode = "fr",
                avatarEmoji = "👩‍⚕️",
                bio = "Médecin à Paris. Heureuse de discuter et découvrir d'autres cultures."
            ),
            UserProfile(
                id = "9",
                alias = "Kenji",
                email = "kenji.sato@tokyo.jp",
                nativeLanguageCode = "ja",
                avatarEmoji = "🧑‍🚀",
                bio = "東京のエンジニアです。リアルタイム音声翻訳で世界中の人と話したいです。"
            )
        )
    }
}
