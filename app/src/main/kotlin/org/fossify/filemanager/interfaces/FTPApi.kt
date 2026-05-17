package org.fossify.filemanager.interfaces

import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.fossify.filemanager.models.NetworkConnection
import java.io.InputStream

interface FTPApi {
    suspend fun connectToFTP(connection: NetworkConnection): Boolean

    suspend fun listAllFTPFiles(path: String): List<FTPFile>

    fun getFTPFileDetail(path: String): FTPFile?

    fun getFTPFileInputStream(path: String,start: Long): InputStream

    fun getFTPConn(): FTPClient
}
