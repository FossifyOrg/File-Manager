package org.fossify.filemanager.interfaces

import kotlinx.coroutines.flow.Flow
import org.fossify.filemanager.models.NetworkConnection

interface NetworkConnectionRepositoryDb {
    suspend fun insertUpdateConnection(connection: NetworkConnection): Long;
    suspend fun getAllSavedConnections(): Flow<List<NetworkConnection>>
    suspend fun deleteConnection(connection: NetworkConnection)
}
