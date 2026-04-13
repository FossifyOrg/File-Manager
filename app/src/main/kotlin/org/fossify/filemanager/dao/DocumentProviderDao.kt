package org.fossify.filemanager.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.fossify.filemanager.entity.DocumentProviderEntity
import org.fossify.filemanager.entity.NetworkConnectionEntity

@Dao
interface DocumentProviderDao {
    @Query("SELECT * FROM document_provider_entity")
    fun getAll(): Flow<List<DocumentProviderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(doc: DocumentProviderEntity)
}
