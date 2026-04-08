package org.fossify.filemanager.repository

import android.util.Log
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.SftpATTRS
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import jcifs.CIFSContext
import jcifs.Configuration
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import org.fossify.filemanager.interfaces.NetworkConnectionRepositoryApi
import org.fossify.filemanager.models.NetworkConnection
import java.io.InputStream
import java.util.Properties

class NetworkConnectionRepositoryApiImpl : NetworkConnectionRepositoryApi {
    lateinit var dir: SmbFile
    lateinit var sardine: Sardine

    lateinit var sftp: ChannelSftp
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
            val jsch = JSch()
            val session: Session = jsch.getSession(userName, server, port)

            session.setPassword(password)
            val config = Properties()
            config["StrictHostKeyChecking"] = "no"
            session.setConfig(config)

            session.connect()
            sftp = session.openChannel("sftp") as ChannelSftp
            sftp.connect()
            return true

        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    override suspend fun listAllSFTPFiles(path: String): MutableList<ChannelSftp.LsEntry> {
        val currentPath = sftp.pwd()
        val files = sftp.ls(currentPath)
        val allFiles = mutableListOf<ChannelSftp.LsEntry>()
        for (item in files) {
            val entry = item as ChannelSftp.LsEntry
            allFiles.add(entry)
        }
        return allFiles
    }

    override fun listSFTPFileDetails(path: String): SftpATTRS? {
        val file = sftp.stat(path)
        return file
    }

    override fun listSFTPFileInputStream(url: String): InputStream {
        return sftp.get(url)
    }

    override fun getSFTPConn() = sftp
}
