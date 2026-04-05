package org.fossify.filemanager.interfaces

import com.thegrizzlylabs.sardineandroid.DavResource
import jcifs.smb.SmbFile
import org.fossify.filemanager.models.NetworkConnection
import java.io.InputStream

interface NetworkConnectionRepositoryApi {
    suspend fun verifyConnection(connection: NetworkConnection): Boolean

    fun getFilesFromNetworkPath(): Array<SmbFile>

    fun getMainSmbFile(): SmbFile

   suspend fun connectAndVerifyWebDav(userName: String = "", password: String = "", url: String): Boolean

    suspend fun listAllFilesOnWebDav(url: String): List<DavResource>

    fun listWebDavFileInputStream(url: String): InputStream

    fun listWebDavFileDetail(url: String): DavResource?
}
