package org.fossify.filemanager.interfaces

import android.content.Context
import net.schmizz.sshj.sftp.FileAttributes
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.sftp.SFTPClient
import org.fossify.filemanager.models.ApiResponse
import org.fossify.filemanager.models.NetworkConnection
import java.io.File
import java.io.InputStream

interface SFTPApi {
    suspend fun connectToSftp(connection: NetworkConnection): Pair<Boolean, Exception?>

    suspend fun listAllFilesSFTPRoot(path: String): ApiResponse<List<RemoteResourceInfo>>

    fun listSFTPFileDetails(path: String):  ApiResponse<FileAttributes?>

    fun getSFTPFileInputStream(url: String, startByte: Long): ApiResponse<InputStream>

    fun createItem(path: String, isFolder: Boolean, name: String): ApiResponse<Boolean>
    fun deleteItem(path: String,isFolder: Boolean): ApiResponse<Boolean>

    fun writeFileToCache(path: String,context: Context): ApiResponse<File>
    fun getSFTPConn(): SFTPClient
}
