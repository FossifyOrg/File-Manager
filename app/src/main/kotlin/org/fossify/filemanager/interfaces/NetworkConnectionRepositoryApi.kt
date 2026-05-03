package org.fossify.filemanager.interfaces


import android.content.Context
import com.thegrizzlylabs.sardineandroid.DavResource
import jcifs.smb.SmbFile
import net.schmizz.sshj.sftp.FileAttributes
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.sftp.SFTPClient
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.fossify.filemanager.enums.Protocols
import org.fossify.filemanager.models.NetworkConnection
import java.io.InputStream
import java.security.cert.X509Certificate

interface NetworkConnectionRepositoryApi {
    suspend fun verifyConnection(connection: NetworkConnection): Boolean

    fun getFilesFromNetworkPath(): Array<SmbFile>

    fun getMainSmbFile(): SmbFile

    ///WebDav

   suspend fun connectAndVerifyWebDav(userName: String = "", password: String = "", url: String,host:String,protocols: Protocols, context: Context): Boolean

    suspend fun listAllFilesOnWebDav(url: String): List<DavResource>

    fun getWebDavFileInputStream(url: String, start: Long, end: Long): InputStream

    fun listWebDavFileDetail(url: String): DavResource?

    fun loadCertificate(stream: InputStream):Result<X509Certificate>
    //SFTP

    suspend fun connectToSftp(userName: String, password: String,server: String,port: Int): Boolean

    suspend fun listAllFilesSFTPRoot(path: String): List<RemoteResourceInfo>

    suspend fun listAllFilesSFTPPath(path: String):List<RemoteResourceInfo>

    fun listSFTPFileDetails(path: String): FileAttributes?

    fun getSFTPFileInputStream(url: String, startByte: Long): InputStream

    fun getSFTPConn(): SFTPClient

    suspend fun connectToFTP(userName: String, password: String,server: String,port: Int): Boolean

    suspend fun listAllFTPFiles(path: String): List<FTPFile>

    fun getFTPFileDetail(path: String): FTPFile?

    fun getFTPFileInputStream(path: String,start: Long): InputStream

    fun getFTPConn(): FTPClient
}
