package org.fossify.filemanager.repository

import android.util.Log
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import jcifs.CIFSContext
import jcifs.Configuration
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.FileAttributes
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import org.fossify.filemanager.interfaces.NetworkConnectionRepositoryApi
import org.fossify.filemanager.models.NetworkConnection
import java.io.InputStream
import java.util.Properties
import net.schmizz.sshj.sftp.RemoteResourceInfo

class NetworkConnectionRepositoryApiImpl : NetworkConnectionRepositoryApi {
    lateinit var dir: SmbFile
    lateinit var sardine: Sardine
    private val sftpLock = Any()
    private lateinit var ssh: SSHClient
    private lateinit var sftp: SFTPClient
    private val defaultProperties: Properties =
        Properties().apply {
            setProperty("jcifs.resolveOrder", "BCAST")
            setProperty("jcifs.smb.client.responseTimeout", "30000")
            setProperty("jcifs.netbios.retryTimeout", "5000")
            setProperty("jcifs.netbios.cachePolicy", "-1")
        }

    override suspend fun verifyConnection(connection: NetworkConnection): Boolean {
        try {
            val config: Configuration = PropertyConfiguration(System.getProperties())
            val p = Properties(defaultProperties)
            var context: CIFSContext = BaseContext(PropertyConfiguration(p))
            val auth = NtlmPasswordAuthenticator(
                "",
                connection.username,
                connection.password
            )
            val authContext = context.withCredentials(auth)
            val smbUrl = "smb://${connection.host}/${connection.sharedPath}"
            dir = SmbFile(smbUrl, authContext)
            return dir.exists()
        } catch (exp: Exception) {
            Log.e("Exception", exp.toString())
        }
        return false
    }

    override fun getFilesFromNetworkPath(): Array<SmbFile> {
        val files = dir.listFiles()
        return files
    }

    override fun getMainSmbFile(): SmbFile = dir

    override suspend fun connectAndVerifyWebDav(userName: String, password: String, url: String): Boolean {
        try {
            sardine = OkHttpSardine()
            sardine.setCredentials(userName, password)
            return sardine.exists(url)
        } catch (exp: Exception) {
            Log.d("WebDav", exp.toString())
            return false
        }
    }

    override suspend fun listAllFilesOnWebDav(url: String): List<DavResource> {
        val resources = sardine.list(url)
        return resources
    }

    override fun listWebDavFileInputStream(url: String, start: Long, end: Long): InputStream {
        val rangeHeader = "bytes=$start-$end"
        val headers = mapOf("Range" to rangeHeader)
        return sardine.get(url, headers)
    }

    override fun listWebDavFileDetail(url: String): DavResource? {
        val resources = sardine.list(url)

        if (resources.isNotEmpty()) {
            return resources[0]
        }
        return null
    }

    override suspend fun connectToSftp(userName: String, password: String, server: String, port: Int): Boolean {
        try {
            if (!::ssh.isInitialized || !ssh.isConnected || !ssh.isAuthenticated) {
                ssh = SSHClient()
                ssh.addHostKeyVerifier(PromiscuousVerifier())
                ssh.connect(server)
                ssh.authPassword(userName, password)
                sftp = ssh.newSFTPClient()
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    override suspend fun listAllFilesSFTPRoot(path: String): List<RemoteResourceInfo> {
        val files = sftp.ls(path)
        return files
    }

    override suspend fun listAllFilesSFTPPath(path: String): List<RemoteResourceInfo> {
        val files = sftp.ls(path)
        return files
    }

    override fun listSFTPFileDetails(path: String): FileAttributes? {
        synchronized(sftpLock) {
            return try {
                val myPath = path.replace("//", "/")
                sftp.stat(myPath)
            } catch (e: Exception) {
                Log.e("SFTP", "Stat failed: ${e.message}")
                null
            }
        }
    }

    override fun listSFTPFileInputStream(url: String, startByte: Long): InputStream {
        val myPath = url.replace("//", "/")
        val remoteFile = sftp.open(myPath)
        val inputStream = remoteFile.RemoteFileInputStream(startByte)
        return inputStream
    }

    override fun getSFTPConn() = sftp

}
