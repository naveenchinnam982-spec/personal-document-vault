package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.HiddenHiddenItem
import kotlinx.coroutines.flow.Flow

@Dao
interface HiddenHiddenDao {

    @Query("SELECT * FROM hidden_hidden_items ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<HiddenHiddenItem>>

    @Query("SELECT * FROM hidden_hidden_items WHERE itemType = 'PHOTO' ORDER BY createdAt DESC")
    fun getPhotos(): Flow<List<HiddenHiddenItem>>

    @Query("SELECT * FROM hidden_hidden_items WHERE itemType = 'VIDEO' ORDER BY createdAt DESC")
    fun getVideos(): Flow<List<HiddenHiddenItem>>

    @Query("SELECT * FROM hidden_hidden_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): HiddenHiddenItem?

    @Query("SELECT * FROM hidden_hidden_items WHERE fileHash = :hash LIMIT 1")
    suspend fun getItemByHash(hash: String): HiddenHiddenItem?

    @Query("SELECT COUNT(*) FROM hidden_hidden_items WHERE itemType = 'PHOTO'")
    fun getPhotosCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM hidden_hidden_items WHERE itemType = 'VIDEO'")
    fun getVideosCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM hidden_hidden_items")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT SUM(fileSizeBytes) FROM hidden_hidden_items")
    fun getTotalSizeBytes(): Flow<Long?>

    @Query("SELECT * FROM hidden_hidden_items WHERE name LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchItems(query: String): Flow<List<HiddenHiddenItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: HiddenHiddenItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<HiddenHiddenItem>): List<Long>

    @Delete
    suspend fun deleteItem(item: HiddenHiddenItem)

    @Delete
    suspend fun deleteItems(items: List<HiddenHiddenItem>)

    @Query("DELETE FROM hidden_hidden_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM hidden_hidden_items")
    suspend fun clearAll()
}
