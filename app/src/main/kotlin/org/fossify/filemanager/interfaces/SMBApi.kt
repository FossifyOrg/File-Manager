package org.fossify.filemanager.interfaces

import jcifs.smb.SmbFile
import org.fossify.filemanager.models.ApiResponse
import org.fossify.filemanager.models.NetworkConnection

interface SMBApi {
    suspend fun verifyConnection(connection: NetworkConnection): Pair<Boolean, Exception?>

    fun getFilesFromNetworkPath(path: String): ApiResponse<Array<SmbFile>>

    fun createFolderOrFile(path: String, isFolder: Boolean, name: String): ApiResponse<Boolean>

    fun getMainSmbFile(): SmbFile
}
