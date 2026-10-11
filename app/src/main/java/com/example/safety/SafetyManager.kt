package com.example.safety

object SafetyManager {

    private val blockedPhrases = listOf(
        "idiota", "estupido", "imbecil", "maldito", "puta", "mierda", "fuck", "bitch", "shit", "arschloch", "scheiße"
    )

    private val blockedUserIds = mutableSetOf<String>()

    fun sanitizeText(input: String): Pair<String, Boolean> {
        var sanitized = input
        var foundViolation = false

        for (word in blockedPhrases) {
            val regex = Regex("(?i)\\b$word\\b")
            if (regex.containsMatchIn(sanitized)) {
                foundViolation = true
                val mask = "*".repeat(word.length)
                sanitized = sanitized.replace(regex, mask)
            }
        }
        return Pair(sanitized, foundViolation)
    }

    fun blockUser(userId: String) {
        blockedUserIds.add(userId)
    }

    fun isUserBlocked(userId: String): Boolean {
        return blockedUserIds.contains(userId)
    }
}
