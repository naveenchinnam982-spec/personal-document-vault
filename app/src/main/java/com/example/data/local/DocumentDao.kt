package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Document
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY updatedAt DESC")
    fun getAllDocuments(): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): Document?

    @Query("SELECT * FROM documents WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteDocuments(): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE category = :category ORDER BY updatedAt DESC")
    fun getDocumentsByCategory(category: String): Flow<List<Document>>

    @Query("""
        SELECT * FROM documents 
        WHERE name LIKE '%' || :query || '%' 
           OR category LIKE '%' || :query || '%' 
           OR tags LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%' 
           OR ocrText LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
    """)
    fun searchDocuments(query: String): Flow<List<Document>>

    @Query("""
        SELECT * FROM documents 
        WHERE expiryDate IS NOT NULL 
          AND expiryDate >= :now 
          AND expiryDate <= :futureLimit
        ORDER BY expiryDate ASC
    """)
    fun getExpiringSoonDocuments(now: Long, futureLimit: Long): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE expiryDate IS NOT NULL AND expiryDate < :now ORDER BY expiryDate DESC")
    fun getExpiredDocuments(now: Long): Flow<List<Document>>

    @Query("SELECT COUNT(*) FROM documents")
    fun getDocumentsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM documents")
    suspend fun getDocumentsCountDirect(): Int

    @Query("SELECT COUNT(*) FROM documents WHERE fileType = 'PDF'")
    fun getPdfCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM documents WHERE fileType != 'PDF'")
    fun getImageCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: Document): Long

    @Update
    suspend fun updateDocument(document: Document)

    @Delete
    suspend fun deleteDocument(document: Document)

    @Query("SELECT * FROM documents WHERE fileHash = :hash LIMIT 1")
    suspend fun getDocumentByHash(hash: String): Document?

    @Query("SELECT * FROM documents WHERE id IN (:ids)")
    suspend fun getDocumentsByIds(ids: List<Long>): List<Document>

    @Query("UPDATE documents SET isFavorite = :favorite WHERE id IN (:ids)")
    suspend fun updateFavoriteBulk(ids: List<Long>, favorite: Boolean)

    @Query("DELETE FROM documents WHERE id IN (:ids)")
    suspend fun deleteDocumentsByIds(ids: List<Long>)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Long)

    @Query("DELETE FROM documents")
    suspend fun clearAll()
}
