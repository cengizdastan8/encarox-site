package com.encarox.kodo.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CodeDao {

    @Query("SELECT * FROM codes WHERE isArchived = 0 ORDER BY receivedAt DESC")
    fun observeActive(): Flow<List<CodeEntity>>

    @Query("SELECT * FROM codes WHERE isArchived = 1 ORDER BY receivedAt DESC")
    fun observeArchived(): Flow<List<CodeEntity>>

    @Query("SELECT COUNT(*) FROM codes WHERE isRead = 0 AND isArchived = 0")
    fun observeUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoring(entity: CodeEntity): Long

    @Query("UPDATE codes SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE codes SET isRead = 1 WHERE isArchived = 0")
    suspend fun markAllRead()

    @Query("UPDATE codes SET isArchived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

    @Query("DELETE FROM codes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM codes WHERE receivedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("SELECT * FROM codes WHERE dedupeKey = :key LIMIT 1")
    suspend fun findByDedupeKey(key: String): CodeEntity?
}
