package org.fossify.filemanager.repository

import android.util.Log
import net.schmizz.sshj.DefaultConfig
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.Factory
import net.schmizz.sshj.sftp.FileAttributes
import net.schmizz.sshj.sftp.OpenMode
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import net.schmizz.sshj.userauth.keyprovider.KeyProviderUtil
import net.schmizz.sshj.userauth.method.AuthPublickey
import net.schmizz.sshj.userauth.password.PasswordUtils
import org.fossify.filemanager.enums.Authentication
import org.fossify.filemanager.interfaces.SFTPApi
import org.fossify.filemanager.models.ApiResponse
import org.fossify.filemanager.models.NetworkConnection
import java.io.IOException
import java.io.InputStream

class SFTPApiImpl : SFTPApi {
    private lateinit var ssh: SSHClient
    private lateinit var sftp: SFTPClient
    private val sftpLock = Any()

    override suspend fun connectToSftp(connection: NetworkConnection): Pair<Boolean, Exception?> {
        return try {
            if (!::ssh.isInitialized || !ssh.isConnected || !ssh.isAuthenticated) {
                ssh = SSHClient()
                ssh.addHostKeyVerifier(PromiscuousVerifier())
                ssh.connect(connection.host, connection.port)
                if (connection.authentication == Authentication.PrivateKey) {
                    ssh.auth(
                        connection.username,
                        AuthPublickey(createKeyProvider(connection.privateKeyText, connection.privateKeyPass.takeIf { it.isNotBlank() }))
                    )
                } else {
                    ssh.authPassword(connection.username, connection.password)
                }
                sftp = ssh.newSFTPClient()
            }
            Pair(true, null)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, e)
        }
    }

    override suspend fun listAllFilesSFTPRoot(path: String): ApiResponse<List<RemoteResourceInfo>> {
        return try {
            val files = sftp.ls(path)
            ApiResponse(files, null)
        } catch (exp: Exception) {
            ApiResponse(null, exp)
        }
    }

    override fun listSFTPFileDetails(path: String): ApiResponse<FileAttributes?> {

        return try {
            val myPath = path.replace("//", "/")
            val attributes = sftp.stat(myPath)
            ApiResponse(attributes, null)
        } catch (exp: Exception) {
            ApiResponse(null, exp)
        }
    }

    override fun getSFTPFileInputStream(url: String, startByte: Long): ApiResponse<InputStream> {
        return try {
            val myPath = url.replace("//", "/")
            val remoteFile = sftp.open(myPath)
            val inputStream = remoteFile.RemoteFileInputStream(startByte)
            ApiResponse(inputStream, null)
        } catch (exp: Exception) {
            ApiResponse(null, exp)
        }

    }

    override fun createItem(path: String, isFolder: Boolean, name: String): ApiResponse<Boolean> {
        return try {
            val uri = "$path/$name"
            if (isFolder) sftp.mkdir(uri) else sftp.open(uri, setOf(OpenMode.CREAT))
            ApiResponse(true, null)
        } catch (exp: Exception) {
            ApiResponse(false, exp)
        }
    }

    override fun getSFTPConn() = sftp

    private val KEY_PROVIDER_FACTORIES = DefaultConfig().fileKeyProviderFactories


    private fun createKeyProvider(
        privateKey: String,
        privateKeyPassword: String?
    ): KeyProvider {
        val format = KeyProviderUtil.detectKeyFileFormat(privateKey, false)
        val keyProvider = Factory.Named.Util.create(KEY_PROVIDER_FACTORIES, format.toString())
            ?: throw IOException("No key provider factory found for $format")
        keyProvider.init(
            privateKey, null,
            privateKeyPassword?.let { PasswordUtils.createOneOff(it.toCharArray()) }
        )
        return keyProvider
    }
}
