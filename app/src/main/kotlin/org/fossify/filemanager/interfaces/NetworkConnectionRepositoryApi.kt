package org.fossify.filemanager.interfaces

import jcifs.smb.SmbFile
import org.fossify.filemanager.models.NetworkConnection

interface NetworkConnectionRepositoryApi {
    suspend fun verifyConnection(connection: NetworkConnection): Boolean

    fun getFilesFromNetworkPath(): Array<SmbFile>
}
