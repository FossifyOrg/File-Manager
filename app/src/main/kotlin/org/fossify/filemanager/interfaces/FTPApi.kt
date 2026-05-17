package org.fossify.filemanager.interfaces

import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.fossify.filemanager.models.ApiResponse
import org.fossify.filemanager.models.NetworkConnection
import java.io.InputStream

interface FTPApi {
    suspend fun connectToFTP(connection: NetworkConnection): Pair<Boolean, Exception?>

    suspend fun listAllFTPFiles(path: String): ApiResponse<List<FTPFile>>

    fun getFTPFileDetail(path: String): ApiResponse<FTPFile?>

    fun getFTPFileInputStream(path: String,start: Long): ApiResponse<InputStream>

    fun getFTPConn(): FTPClient
}
