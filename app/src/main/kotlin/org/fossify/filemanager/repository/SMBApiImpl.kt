package org.fossify.filemanager.repository

import android.util.Log
import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import org.fossify.commons.enums.ConnectionTypes
import org.fossify.filemanager.enums.Authentication
import org.fossify.filemanager.helpers.Helpers
import org.fossify.filemanager.interfaces.SMBApi
import org.fossify.filemanager.models.NetworkConnection
import java.util.Properties

class SMBApiImpl : SMBApi {
    lateinit var smbClient: SmbFile

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
            val smbUrl = Helpers.createUrl(ConnectionTypes.SMB, connection.sharedPath, connection.host, connection.port)
            smbClient = SmbFile(smbUrl, authContext)
            return smbClient.exists()
        } catch (exp: Exception) {
            Log.e("Exception", exp.toString())
        }
        return false
    }

    override fun getFilesFromNetworkPath(): Array<SmbFile> {
        val files = smbClient.listFiles()
        return files
    }

    override fun getMainSmbFile(): SmbFile = smbClient
}
