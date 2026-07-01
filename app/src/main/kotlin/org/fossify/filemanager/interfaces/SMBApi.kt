package org.fossify.filemanager.interfaces

import android.content.Context
import jcifs.smb.SmbFile
import org.fossify.filemanager.models.ApiResponse
import org.fossify.filemanager.models.NetworkConnection
import java.io.File

interface SMBApi {
    suspend fun verifyConnection(connection: NetworkConnection): Pair<Boolean, Exception?>

    fun getFilesFromNetworkPath(path: String): ApiResponse<Array<SmbFile>>

    fun createFolderOrFile(path: String, isFolder: Boolean, name: String): ApiResponse<Boolean>

    fun deleteItem(path: String): ApiResponse<Boolean>

    fun writeFileToCache(path: String,context: Context): ApiResponse<File>

    fun getSmbFile(path: String):  ApiResponse<SmbFile>

    fun getMainSmbFile(): SmbFile
}
