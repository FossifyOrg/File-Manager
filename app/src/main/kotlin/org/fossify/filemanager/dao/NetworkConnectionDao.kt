package org.fossify.filemanager.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.fossify.filemanager.entity.NetworkConnectionEntity

@Dao
interface NetworkConnectionDao {
    @Query("SELECT * FROM network_connections")
    fun getAll(): Flow<List<NetworkConnectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(connection: NetworkConnectionEntity): Long
}
