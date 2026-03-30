package org.fossify.filemanager.repository

import android.util.Log
import jcifs.CIFSContext
import jcifs.Configuration
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import org.fossify.filemanager.interfaces.NetworkConnectionRepositoryApi
import org.fossify.filemanager.models.NetworkConnection
import java.util.Properties

class NetworkConnectionRepositoryApiImpl: NetworkConnectionRepositoryApi {
    private lateinit var dir: SmbFile
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
        }
        catch (exp: Exception){
            Log.e("Exception",exp.toString())
        }
        return false
    }

    override fun getFilesFromNetworkPath(): Array<SmbFile> {
        val files = dir.listFiles()
        return files
    }
}
