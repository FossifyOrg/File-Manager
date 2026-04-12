package org.fossify.filemanager.interfaces


import com.thegrizzlylabs.sardineandroid.DavResource
import jcifs.smb.SmbFile
import net.schmizz.sshj.sftp.FileAttributes
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.sftp.SFTPClient
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
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

    suspend fun listAllFilesSFTPRoot(path: String): List<RemoteResourceInfo>

    suspend fun listAllFilesSFTPPath(path: String):List<RemoteResourceInfo>

    fun listSFTPFileDetails(path: String): FileAttributes?

    fun listSFTPFileInputStream(url: String,startByte: Long): InputStream

    fun getSFTPConn(): SFTPClient

    suspend fun connectToFTP(userName: String, password: String,server: String,port: Int): Boolean

    suspend fun listAllFTPFiles(path: String): List<FTPFile>

    fun getFTPFileDetail(path: String): FTPFile?

    fun getFTPFileInputStream(path: String,start: Long): InputStream

    fun getFTPConn(): FTPClient
}
