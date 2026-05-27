package org.fossify.filemanager.repository

import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import org.fossify.commons.enums.ConnectionTypes
import org.fossify.filemanager.enums.Authentication
import org.fossify.filemanager.helpers.Helpers
import org.fossify.filemanager.interfaces.SMBApi
import org.fossify.filemanager.models.ApiResponse
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
    override suspend fun verifyConnection(connection: NetworkConnection): Pair<Boolean,Exception?> {
       return try {
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
            Pair(smbClient.exists(),null)
        } catch (exp: Exception) {
            Pair(false,exp)
        }
    }

    override fun getFilesFromNetworkPath(path: String): ApiResponse<Array<SmbFile>> {
        return try {
            if ("$path/" == smbClient.canonicalPath){
                val files = smbClient.listFiles()
                return ApiResponse(files,null)
            }
            val subDir = SmbFile("$path/", smbClient.context)
            ApiResponse(subDir.listFiles(),null)
        }
        catch (exp: Exception){
            ApiResponse(null,exp)
        }
    }

    override fun createFolderOrFile(path: String, isFolder: Boolean, name: String): ApiResponse<Boolean> {
        return try {
            val file = SmbFile("$path/$name", smbClient.context)
            if (isFolder) file.mkdir() else file.createNewFile()
            ApiResponse(true,null)
        }
        catch (exp: Exception){
            ApiResponse(null,exp)
        }
    }

    override fun getMainSmbFile(): SmbFile = smbClient
}
