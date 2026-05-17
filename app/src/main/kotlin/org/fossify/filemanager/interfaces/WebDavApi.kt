package org.fossify.filemanager.interfaces

import android.content.Context
import com.thegrizzlylabs.sardineandroid.DavResource
import org.fossify.filemanager.enums.Protocols
import org.fossify.filemanager.models.ApiResponse
import org.fossify.filemanager.models.NetworkConnection
import java.io.InputStream

interface WebDavApi {
    suspend fun connectAndVerifyWebDav(connection: NetworkConnection,protocols: Protocols, context: Context): Pair<Boolean, Exception?>

    suspend fun listAllFilesOnWebDav(url: String): ApiResponse<List<DavResource>>

    fun getWebDavFileInputStream(url: String, start: Long, end: Long): ApiResponse<InputStream>

    fun listWebDavFileDetail(url: String): ApiResponse<DavResource?>
}
