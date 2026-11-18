package org.fossify.filemanager.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.fossify.filemanager.entity.NetworkConnectionEntity

@Dao
interface NetworkConnectionDao {
    @Query("SELECT * FROM network_connections")
    fun getAll(): Flow<List<NetworkConnectionEntity>>

    @Update
    suspend fun updateConnection(connection: NetworkConnectionEntity): Int

    @Delete
    suspend fun delete(connection: NetworkConnectionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun addConnection(connection: NetworkConnectionEntity): Long
}
