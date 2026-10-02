package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.HiddenAlbum
import com.example.data.model.HiddenItem
import com.example.data.model.HiddenTrashItem
import kotlinx.coroutines.flow.Flow

@Dao
interface HiddenDao {

    // --- Hidden Items ---
    @Query("SELECT * FROM hidden_items ORDER BY createdAt DESC")
    fun getAllHiddenItems(): Flow<List<HiddenItem>>

    @Query("SELECT * FROM hidden_items")
    suspend fun getAllHiddenItemsSync(): List<HiddenItem>

    @Query("SELECT * FROM hidden_items WHERE (itemType = 'PHOTO' OR itemType IS NULL) AND encryptedPath NOT LIKE '%.enc_vid' AND LOWER(name) NOT LIKE '%.mp4' AND LOWER(name) NOT LIKE '%.mov' AND LOWER(name) NOT LIKE '%.mkv' AND LOWER(name) NOT LIKE '%.webm' ORDER BY createdAt DESC")
    fun getHiddenPhotos(): Flow<List<HiddenItem>>

    @Query("SELECT * FROM hidden_items WHERE itemType = 'VIDEO' OR encryptedPath LIKE '%.enc_vid' OR LOWER(name) LIKE '%.mp4' OR LOWER(name) LIKE '%.mov' OR LOWER(name) LIKE '%.mkv' OR LOWER(name) LIKE '%.webm' ORDER BY createdAt DESC")
    fun getHiddenVideos(): Flow<List<HiddenItem>>

    @Query("SELECT * FROM hidden_items WHERE itemType IN ('DOCUMENT', 'PDF', 'SCAN') ORDER BY createdAt DESC")
    fun getHiddenDocuments(): Flow<List<HiddenItem>>

    @Query("SELECT * FROM hidden_items WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteHiddenItems(): Flow<List<HiddenItem>>

    @Query("SELECT * FROM hidden_items WHERE lastOpenedAt IS NOT NULL ORDER BY lastOpenedAt DESC LIMIT 15")
    fun getRecentlyOpenedItems(): Flow<List<HiddenItem>>

    @Query("SELECT * FROM hidden_items WHERE albumId = :albumId ORDER BY createdAt DESC")
    fun getHiddenItemsByAlbum(albumId: Long): Flow<List<HiddenItem>>

    @Query("SELECT * FROM hidden_items WHERE albumId IS NULL ORDER BY createdAt DESC")
    fun getUnorganizedHiddenItems(): Flow<List<HiddenItem>>

    @Query("SELECT * FROM hidden_items WHERE id = :id")
    suspend fun getHiddenItemById(id: Long): HiddenItem?

    @Query("SELECT * FROM hidden_items WHERE LOWER(name) LIKE '%' || LOWER(:query) || '%' OR LOWER(tags) LIKE '%' || LOWER(:query) || '%' OR LOWER(category) LIKE '%' || LOWER(:query) || '%' OR LOWER(ocrText) LIKE '%' || LOWER(:query) || '%' ORDER BY createdAt DESC")
    fun searchHiddenItems(query: String): Flow<List<HiddenItem>>

    @Query("SELECT * FROM hidden_items WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findItemByName(name: String): HiddenItem?

    @Query("SELECT * FROM hidden_items WHERE fileHash = :hash LIMIT 1")
    suspend fun getHiddenItemByHash(hash: String): HiddenItem?

    @Query("SELECT COUNT(*) FROM hidden_items")
    fun getHiddenTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM hidden_items WHERE (itemType = 'PHOTO' OR itemType IS NULL) AND encryptedPath NOT LIKE '%.enc_vid' AND LOWER(name) NOT LIKE '%.mp4' AND LOWER(name) NOT LIKE '%.mov' AND LOWER(name) NOT LIKE '%.mkv' AND LOWER(name) NOT LIKE '%.webm'")
    fun getHiddenPhotosCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM hidden_items WHERE itemType = 'VIDEO' OR encryptedPath LIKE '%.enc_vid' OR LOWER(name) LIKE '%.mp4' OR LOWER(name) LIKE '%.mov' OR LOWER(name) LIKE '%.mkv' OR LOWER(name) LIKE '%.webm'")
    fun getHiddenVideosCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM hidden_items WHERE itemType IN ('DOCUMENT', 'PDF', 'SCAN')")
    fun getHiddenDocsCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(fileSizeBytes), 0) FROM hidden_items WHERE (itemType = 'PHOTO' OR itemType IS NULL) AND encryptedPath NOT LIKE '%.enc_vid' AND LOWER(name) NOT LIKE '%.mp4' AND LOWER(name) NOT LIKE '%.mov' AND LOWER(name) NOT LIKE '%.mkv' AND LOWER(name) NOT LIKE '%.webm'")
    fun getHiddenPhotosSizeBytes(): Flow<Long>

    @Query("SELECT COALESCE(SUM(fileSizeBytes), 0) FROM hidden_items WHERE itemType = 'VIDEO' OR encryptedPath LIKE '%.enc_vid' OR LOWER(name) LIKE '%.mp4' OR LOWER(name) LIKE '%.mov' OR LOWER(name) LIKE '%.mkv' OR LOWER(name) LIKE '%.webm'")
    fun getHiddenVideosSizeBytes(): Flow<Long>

    @Query("SELECT COALESCE(SUM(fileSizeBytes), 0) FROM hidden_items WHERE itemType IN ('DOCUMENT', 'PDF', 'SCAN')")
    fun getHiddenDocsSizeBytes(): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHiddenItem(item: HiddenItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHiddenItems(items: List<HiddenItem>): List<Long>

    @Update
    suspend fun updateHiddenItem(item: HiddenItem)

    @Delete
    suspend fun deleteHiddenItem(item: HiddenItem)

    @Query("DELETE FROM hidden_items WHERE id = :id")
    suspend fun deleteHiddenItemById(id: Long)

    @Query("DELETE FROM hidden_items WHERE id IN (:ids)")
    suspend fun deleteHiddenItemsByIds(ids: List<Long>)

    @Query("DELETE FROM hidden_items")
    suspend fun clearAllHiddenItems()

    // --- Hidden Albums ---
    @Query("SELECT * FROM hidden_albums ORDER BY createdAt DESC")
    fun getAllHiddenAlbums(): Flow<List<HiddenAlbum>>

    @Query("SELECT * FROM hidden_albums WHERE id = :id")
    suspend fun getHiddenAlbumById(id: Long): HiddenAlbum?

    @Query("SELECT COUNT(*) FROM hidden_albums")
    fun getHiddenAlbumsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHiddenAlbum(album: HiddenAlbum): Long

    @Update
    suspend fun updateHiddenAlbum(album: HiddenAlbum)

    @Delete
    suspend fun deleteHiddenAlbum(album: HiddenAlbum)

    // --- Hidden Trash ---
    @Query("SELECT * FROM hidden_trash_items ORDER BY deletedAt DESC")
    fun getAllHiddenTrashItems(): Flow<List<HiddenTrashItem>>

    @Query("SELECT COUNT(*) FROM hidden_trash_items")
    fun getHiddenTrashCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHiddenTrashItem(item: HiddenTrashItem): Long

    @Delete
    suspend fun deleteHiddenTrashItem(item: HiddenTrashItem)

    @Query("DELETE FROM hidden_trash_items WHERE id = :id")
    suspend fun deleteHiddenTrashItemById(id: Long)

    @Query("DELETE FROM hidden_trash_items")
    suspend fun clearAllHiddenTrash()
}
