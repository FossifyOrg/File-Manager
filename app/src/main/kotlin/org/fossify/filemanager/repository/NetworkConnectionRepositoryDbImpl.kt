package org.fossify.filemanager.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.fossify.filemanager.dao.NetworkConnectionDao
import org.fossify.filemanager.interfaces.NetworkConnectionRepositoryDb
import org.fossify.filemanager.mapper.toDomain
import org.fossify.filemanager.mapper.toEntity
import org.fossify.filemanager.models.ApiResponse
import org.fossify.filemanager.models.NetworkConnection

class NetworkConnectionRepositoryDbImpl(private val dao: NetworkConnectionDao) : NetworkConnectionRepositoryDb {
    override suspend fun updateConnection(connection: NetworkConnection): ApiResponse<Boolean> {
        return try {
            dao.updateConnection(connection.toEntity())
            ApiResponse(true, null)
        } catch (exp: Exception) {
            ApiResponse(false, exp)
        }
    }

    override fun getAllSavedConnections(): Flow<List<NetworkConnection>> {
        return dao.getAll().map { value -> value.map { entity -> entity.toDomain() } }
    }

    override suspend fun deleteConnection(connection: NetworkConnection): ApiResponse<Boolean> {
        return try {
            dao.delete(connection.toEntity())
            ApiResponse(true, null)
        } catch (exp: Exception) {
            ApiResponse(false, exp)
        }

    }

    override suspend fun addConnection(connection: NetworkConnection): ApiResponse<Boolean> {
        return try {
            dao.addConnection(connection.toEntity())
            ApiResponse(true, null)
        } catch (exp: Exception) {
            ApiResponse(false, exp)
        }
    }
}
