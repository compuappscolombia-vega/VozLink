package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(translation: TranslationEntity): Long

    @Update
    suspend fun update(translation: TranslationEntity)

    @Delete
    suspend fun delete(translation: TranslationEntity)

    @Query("DELETE FROM translations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM translations ORDER BY timestamp DESC")
    fun getAllTranslations(): Flow<List<TranslationEntity>>

    @Query("""
        SELECT * FROM translations 
        WHERE (senderId = :userId1 AND receiverId = :userId2) 
           OR (senderId = :userId2 AND receiverId = :userId1) 
        ORDER BY timestamp ASC
    """)
    fun getChatBetweenUsers(userId1: String, userId2: String): Flow<List<TranslationEntity>>

    @Query("""
        SELECT * FROM translations 
        WHERE originalText LIKE '%' || :query || '%' 
           OR translatedText LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchTranslations(query: String): Flow<List<TranslationEntity>>

    @Query("DELETE FROM translations")
    suspend fun clearAll()
}
