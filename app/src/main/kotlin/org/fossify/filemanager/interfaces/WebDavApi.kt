package org.fossify.filemanager.interfaces

import android.content.Context
import com.thegrizzlylabs.sardineandroid.DavResource
import org.fossify.filemanager.enums.Protocols
import org.fossify.filemanager.models.NetworkConnection
import java.io.InputStream

interface WebDavApi {
    suspend fun connectAndVerifyWebDav(connection: NetworkConnection,protocols: Protocols, context: Context): Boolean

    suspend fun listAllFilesOnWebDav(url: String): List<DavResource>

    fun getWebDavFileInputStream(url: String, start: Long, end: Long): InputStream

    fun listWebDavFileDetail(url: String): DavResource?
}
