package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PrivateMedia
import kotlinx.coroutines.flow.Flow

@Dao
interface PrivateMediaDao {
    @Query("SELECT * FROM private_media ORDER BY createdAt DESC")
    fun getAllMedia(): Flow<List<PrivateMedia>>

    @Query("SELECT * FROM private_media WHERE albumId = :albumId ORDER BY createdAt DESC")
    fun getMediaByAlbum(albumId: Long): Flow<List<PrivateMedia>>

    @Query("SELECT * FROM private_media WHERE albumId IS NULL ORDER BY createdAt DESC")
    fun getMediaUnorganized(): Flow<List<PrivateMedia>>

    @Query("SELECT * FROM private_media WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteMedia(): Flow<List<PrivateMedia>>

    @Query("SELECT * FROM private_media WHERE id = :id LIMIT 1")
    suspend fun getMediaById(id: Long): PrivateMedia?

    @Query("SELECT COUNT(*) FROM private_media WHERE (mediaType = 'PHOTO' OR mediaType IS NULL) AND encryptedPath NOT LIKE '%.enc_vid' AND LOWER(name) NOT LIKE '%.mp4' AND LOWER(name) NOT LIKE '%.mov' AND LOWER(name) NOT LIKE '%.mkv'")
    fun getPhotosCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM private_media WHERE mediaType = 'VIDEO' OR encryptedPath LIKE '%.enc_vid' OR LOWER(name) LIKE '%.mp4' OR LOWER(name) LIKE '%.mov' OR LOWER(name) LIKE '%.mkv'")
    fun getVideosCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: PrivateMedia): Long

    @Update
    suspend fun updateMedia(media: PrivateMedia)

    @Delete
    suspend fun deleteMedia(media: PrivateMedia)

    @Query("UPDATE private_media SET albumId = :albumId WHERE id IN (:mediaIds)")
    suspend fun moveMediaToAlbum(mediaIds: List<Long>, albumId: Long?)

    @Query("SELECT * FROM private_media WHERE fileHash = :hash LIMIT 1")
    suspend fun getMediaByHash(hash: String): PrivateMedia?

    @Query("SELECT * FROM private_media WHERE id IN (:ids)")
    suspend fun getMediaByIds(ids: List<Long>): List<PrivateMedia>

    @Query("UPDATE private_media SET isFavorite = :favorite WHERE id IN (:ids)")
    suspend fun updateFavoriteBulk(ids: List<Long>, favorite: Boolean)

    @Query("DELETE FROM private_media WHERE id IN (:ids)")
    suspend fun deleteMediaByIds(ids: List<Long>)

    @Query("DELETE FROM private_media WHERE id = :id")
    suspend fun deleteMediaById(id: Long)

    @Query("DELETE FROM private_media")
    suspend fun clearAll()
}
