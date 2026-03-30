package org.fossify.filemanager.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.fossify.filemanager.dao.NetworkConnectionDao
import org.fossify.filemanager.database.NetworkConnectionDatabase
import org.fossify.filemanager.entity.NetworkConnectionEntity
import org.fossify.filemanager.interfaces.NetworkConnectionRepositoryDb
import org.fossify.filemanager.mapper.toDomain
import org.fossify.filemanager.mapper.toEntity
import org.fossify.filemanager.models.NetworkConnection

class NetworkConnectionRepositoryDbImpl(private val dao: NetworkConnectionDao): NetworkConnectionRepositoryDb {
    override suspend fun saveConnection(connection: NetworkConnection) {
        dao.insert(connection.toEntity())
    }

    override suspend fun getAllSavedConnections(): Flow<List<NetworkConnection>> {
        return dao.getAll().map { value -> value.map { entity -> entity.toDomain() } }
    }

    override suspend fun deleteConnection() {
        TODO("Not yet implemented")
    }
}
