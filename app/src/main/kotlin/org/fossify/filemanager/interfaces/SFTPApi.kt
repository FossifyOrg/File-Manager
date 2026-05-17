package org.fossify.filemanager.interfaces

import net.schmizz.sshj.sftp.FileAttributes
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.sftp.SFTPClient
import org.fossify.filemanager.models.NetworkConnection
import java.io.InputStream

interface SFTPApi {
    suspend fun connectToSftp(connection: NetworkConnection): Boolean

    suspend fun listAllFilesSFTPRoot(path: String): List<RemoteResourceInfo>

    suspend fun listAllFilesSFTPPath(path: String):List<RemoteResourceInfo>

    fun listSFTPFileDetails(path: String): FileAttributes?

    fun getSFTPFileInputStream(url: String, startByte: Long): InputStream

    fun getSFTPConn(): SFTPClient
}
