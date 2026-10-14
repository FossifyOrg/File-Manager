package org.fossify.filemanager.interfaces

import android.content.Context
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.fossify.filemanager.models.ApiResponse
import org.fossify.filemanager.models.NetworkConnection
import java.io.File
import java.io.InputStream

interface FTPApi {
    suspend fun connectToFTP(connection: NetworkConnection): Pair<Boolean, Exception?>

    suspend fun listAllFTPFiles(path: String): ApiResponse<List<FTPFile>>

    fun getFTPFileDetail(path: String): ApiResponse<FTPFile?>

    fun getFTPFileInputStream(path: String,start: Long): ApiResponse<InputStream>
    fun deleteItem(path: String,isFolder: Boolean): ApiResponse<Boolean>
    fun createItem(path: String, isFolder: Boolean, name: String): ApiResponse<Boolean>
    fun writeFileToCache(path: String,context: Context): ApiResponse<File>
    fun getFTPConn(): FTPClient
}
