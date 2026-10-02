package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SecurityActivityLog
import kotlinx.coroutines.flow.Flow

@Dao
interface SecurityLogDao {

    @Query("SELECT * FROM security_activity_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<SecurityActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SecurityActivityLog): Long

    @Query("DELETE FROM security_activity_logs")
    suspend fun clearAllLogs()

    @Query("SELECT COUNT(*) FROM security_activity_logs")
    fun getLogsCount(): Flow<Int>
}
