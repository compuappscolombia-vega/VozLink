package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "translations")
data class TranslationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val senderId: String,
    val senderAlias: String,
    val receiverId: String,
    val receiverAlias: String,
    val sourceLanguageCode: String,
    val targetLanguageCode: String,
    val originalText: String,
    val translatedText: String,
    val isVoiceNote: Boolean = false,
    val durationSeconds: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isSavedToFavorites: Boolean = false
)
