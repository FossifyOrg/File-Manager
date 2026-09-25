package org.fossify.filemanager.interfaces

import kotlinx.coroutines.flow.Flow
import org.fossify.filemanager.models.ApiResponse
import org.fossify.filemanager.models.NetworkConnection

interface NetworkConnectionRepositoryDb {
    suspend fun updateConnection(connection: NetworkConnection): ApiResponse<Boolean>;
    fun getAllSavedConnections(): Flow<List<NetworkConnection>>
    suspend fun deleteConnection(connection: NetworkConnection): ApiResponse<Boolean>

    suspend fun addConnection(connection: NetworkConnection) : ApiResponse<Boolean>
}
