package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["alias"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val alias: String,
    val email: String,
    val password: String,
    val nativeLanguageCode: String,
    val avatarEmoji: String = "👤",
    val photoUri: String? = null,
    val bio: String = "Explorador de idiomas y comunicación en tiempo real."
) {
    fun toUserProfile(): UserProfile {
        return UserProfile(
            id = id.toString(),
            alias = alias,
            email = email,
            nativeLanguageCode = nativeLanguageCode,
            avatarEmoji = avatarEmoji,
            photoUri = photoUri,
            bio = bio
        )
    }
}
