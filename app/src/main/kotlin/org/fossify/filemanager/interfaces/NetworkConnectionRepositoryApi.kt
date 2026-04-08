package org.fossify.filemanager.interfaces

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.SftpATTRS
import com.thegrizzlylabs.sardineandroid.DavResource
import jcifs.smb.SmbFile
import org.fossify.filemanager.models.NetworkConnection
import java.io.InputStream

interface NetworkConnectionRepositoryApi {
    suspend fun verifyConnection(connection: NetworkConnection): Boolean

    fun getFilesFromNetworkPath(): Array<SmbFile>

    fun getMainSmbFile(): SmbFile

   suspend fun connectAndVerifyWebDav(userName: String = "", password: String = "", url: String): Boolean

    suspend fun listAllFilesOnWebDav(url: String): List<DavResource>

    fun listWebDavFileInputStream(url: String,start: Long,end: Long): InputStream

    fun listWebDavFileDetail(url: String): DavResource?

    suspend fun connectToSftp(userName: String, password: String,server: String,port: Int): Boolean

    suspend fun listAllSFTPFiles(path: String):MutableList<ChannelSftp.LsEntry>

    fun listSFTPFileDetails(path: String): SftpATTRS?

    fun listSFTPFileInputStream(url: String): InputStream

    fun getSFTPConn(): ChannelSftp
}
