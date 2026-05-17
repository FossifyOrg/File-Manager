package org.fossify.filemanager.repository

import android.util.Log
import net.schmizz.sshj.DefaultConfig
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.Factory
import net.schmizz.sshj.sftp.FileAttributes
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import net.schmizz.sshj.userauth.keyprovider.KeyProviderUtil
import net.schmizz.sshj.userauth.method.AuthPublickey
import net.schmizz.sshj.userauth.password.PasswordUtils
import org.fossify.filemanager.enums.Authentication
import org.fossify.filemanager.interfaces.SFTPApi
import org.fossify.filemanager.models.NetworkConnection
import java.io.IOException
import java.io.InputStream

class SFTPApiImpl: SFTPApi {
    private lateinit var ssh: SSHClient
    private lateinit var sftp: SFTPClient
    private val sftpLock = Any()

    override suspend fun connectToSftp(connection: NetworkConnection): Boolean {
        try {
            if (!::ssh.isInitialized || !ssh.isConnected || !ssh.isAuthenticated) {
                ssh = SSHClient()
                ssh.addHostKeyVerifier(PromiscuousVerifier())
                ssh.connect(connection.host,connection.port)
                if(connection.authentication == Authentication.PrivateKey){
                    ssh.auth(connection.username, AuthPublickey(createKeyProvider(connection.privateKeyText,connection.privateKeyPass.takeIf { it.isNotBlank() })))
                }
                else{
                    ssh.authPassword(connection.username, connection.password)
                }
                sftp = ssh.newSFTPClient()
            }
            return true
        } catch (e: Exception) {
            Log.e("SFTP", "Connect failed: ${e.message}")
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
