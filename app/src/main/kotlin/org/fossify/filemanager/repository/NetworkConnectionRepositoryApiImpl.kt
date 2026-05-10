package org.fossify.filemanager.repository

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import jcifs.CIFSContext
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
import okhttp3.OkHttpClient
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPCmd
import org.apache.commons.net.ftp.FTPFile
import org.fossify.commons.enums.ConnectionTypes
import org.fossify.filemanager.enums.Authentication
import org.fossify.filemanager.enums.Protocols
import org.fossify.filemanager.helpers.Helpers
import org.fossify.filemanager.keyStores.CertificateStore
import java.io.File
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

class NetworkConnectionRepositoryApiImpl : NetworkConnectionRepositoryApi {
    lateinit var dir: SmbFile
    lateinit var sardine: Sardine
    private val sftpLock = Any()
    private lateinit var ssh: SSHClient
    private lateinit var sftp: SFTPClient
    private lateinit var currentStream: InputStream
    private lateinit var ftp: FTPClient
    private lateinit var ftpStream: FTPClient

    private val defaultProperties: Properties =
        Properties().apply {
            setProperty("jcifs.resolveOrder", "BCAST")
            setProperty("jcifs.smb.client.responseTimeout", "30000")
            setProperty("jcifs.netbios.retryTimeout", "5000")
            setProperty("jcifs.netbios.cachePolicy", "-1")
        }

    override suspend fun verifyConnection(connection: NetworkConnection): Boolean {
        try {
            val p = Properties(defaultProperties)
            val context: CIFSContext = BaseContext(PropertyConfiguration(p))
            var authContext: CIFSContext? = null
            if (connection.authentication == Authentication.Password) {
                val auth = NtlmPasswordAuthenticator(
                    "",
                    connection.username,
                    connection.password
                )
                authContext = context.withCredentials(auth)
            } else if (connection.authentication == Authentication.Anonymous) {
                authContext = context.withGuestCrendentials()
            }
            val smbUrl = Helpers.createUrl(ConnectionTypes.SMB, connection.sharedPath, connection.host)
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

    override suspend fun connectAndVerifyWebDav(
        connection: NetworkConnection,
        protocols: Protocols,
        context: Context
    ): Boolean {
        try {
            sardine = if (protocols == Protocols.HTTP) {
                OkHttpSardine()
            } else {
                createHTTPSSardine(context,connection.host)
            }
            if(connection.authentication == Authentication.Anonymous){
                return sardine.exists(connection.url)
            }
            sardine.setCredentials(connection.username, connection.password)
            return sardine.exists(connection.url)
        } catch (exp: Exception) {
            Log.d("WebDav", exp.toString())
            return false
        }
    }

    override suspend fun listAllFilesOnWebDav(url: String): List<DavResource> {
        val resources = sardine.list("http://192.168.18.86:8090/WebDav/")
        return resources
    }

    override fun getWebDavFileInputStream(url: String, start: Long, end: Long): InputStream {
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

    override fun loadCertificate(stream: InputStream): Result<X509Certificate> {
        return try {
            val cf = CertificateFactory.getInstance("X.509")
            val cert = cf.generateCertificate(stream) as X509Certificate
            cert.checkValidity()
            Result.success(cert)
        } catch (exp: Exception) {
            Result.failure(Exception(exp.message))
        }
    }


    override suspend fun connectToSftp(connection: NetworkConnection): Boolean {
        try {
            if (!::ssh.isInitialized || !ssh.isConnected || !ssh.isAuthenticated) {
                ssh = SSHClient()
                ssh.addHostKeyVerifier(PromiscuousVerifier())
                ssh.connect(connection.host)
                ssh.authPassword(connection.username, connection.password)
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

    override fun getSFTPFileInputStream(url: String, startByte: Long): InputStream {
        val myPath = url.replace("//", "/")
        val remoteFile = sftp.open(myPath)
        val inputStream = remoteFile.RemoteFileInputStream(startByte)
        return inputStream
    }

    override fun getSFTPConn() = sftp

    override suspend fun connectToFTP(connection: NetworkConnection): Boolean {
        try {
            ftp = FTPClient()
            ftpStream = FTPClient()
            ftp.connect(connection.host, connection.port)
            ftpStream.connect(connection.host, connection.port)

            val loginSuccess = ftp.login(connection.username, connection.password)
            ftpStream.login(connection.username,connection.password)

            if (!loginSuccess) {
                return false
            }
            ftp.enterLocalPassiveMode()
            ftpStream.enterLocalPassiveMode()
            return true
        } catch (exp: Exception) {
            return false
        }
    }

    override suspend fun listAllFTPFiles(path: String): List<FTPFile> {
        ftp.changeWorkingDirectory(path)
        val files: Array<FTPFile> = ftp.listFiles()
        return files.toList()
    }

    override fun getFTPFileDetail(path: String): FTPFile? {
        val myPath = path.replace("//", "/")
        if (ftp.hasFeature(FTPCmd.MLST)) {
            val file = ftp.mlistFile(myPath)
            return file
        }
        val mP = File(myPath)
        val files = ftp.listFiles(mP.parent).firstOrNull { it != null && it.name == mP.name }
        return files
    }

    override fun getFTPFileInputStream(path: String, start: Long): InputStream {
        if (::currentStream.isInitialized)
            currentStream.close()
        ftpStream.completePendingCommand()
        ftpStream.setFileType(FTP.BINARY_FILE_TYPE)
        ftpStream.restartOffset = start
        currentStream = ftpStream.retrieveFileStream(path)
        return currentStream
    }

    override fun getFTPConn(): FTPClient = ftp

    private fun createHTTPSSardine(context: Context, host: String): Sardine {
        return buildSardineWithUserCert(context, host)
    }

    private fun buildSardineWithUserCert(
        context: Context,
        host: String
    ): Sardine {
        val cert = CertificateStore.loadCert(context, host)

        val trustManager = @SuppressLint("CustomX509TrustManager")
        object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf(cert)

            @SuppressLint("TrustAllX509TrustManager")
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
            }

            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                if (!chain[0].encoded.contentEquals(cert.encoded)) {
                    throw CertificateException("Untrusted certificate")
                }
            }
        }

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustManager), null)
        }

        val okHttpClient = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier { hostname, _ ->
                hostname == host
            }
            .build()

        return OkHttpSardine(okHttpClient)
    }

}
