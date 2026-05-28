package org.fossify.filemanager.repository

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import okhttp3.OkHttpClient
import org.fossify.filemanager.enums.Protocols
import org.fossify.filemanager.interfaces.WebDavApi
import org.fossify.filemanager.keyStores.CertificateStore
import org.fossify.filemanager.models.ApiResponse
import org.fossify.filemanager.models.NetworkConnection
import java.io.InputStream
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class WebDavApiImpl: WebDavApi {
    lateinit var sardine: Sardine
    val scope = CoroutineScope(Dispatchers.IO)
    override suspend fun connectAndVerifyWebDav(
        connection: NetworkConnection,
        protocols: Protocols,
        context: Context
    ): Pair<Boolean, Exception?> {
        return try {
            sardine = if (protocols == Protocols.HTTP) {
                OkHttpSardine()
            } else {
                createHTTPSSardine(context,connection.host)
            }
            sardine.setCredentials(connection.username, connection.password)
            Pair(sardine.exists(connection.url),null)
        } catch (exp: Exception) {
            Pair(false,exp)
        }
    }

    override suspend fun listAllFilesOnWebDav(url: String): ApiResponse<List<DavResource>> {
        return try {
            val resources = sardine.list(url)
            val b = Uri.decode(url.toUri().encodedPath?.trimEnd('/'))
            val filteredItems = resources.filter { resource ->
                val a = Uri.decode(resource.href.toString().toUri().encodedPath?.trimEnd('/'))
                a != b
            }
            ApiResponse(filteredItems,null)
        }
        catch (exp: Exception){
            ApiResponse(null,exp)
        }
    }

    override fun getWebDavFileInputStream(url: String, start: Long, end: Long): ApiResponse<InputStream> {
        return try {
            val rangeHeader = "bytes=$start-$end"
            val headers = mapOf("Range" to rangeHeader)
            val stream = sardine.get(url, headers)
            ApiResponse(stream,null)
        }
        catch (exp: Exception){
            ApiResponse(null,exp)
        }
    }

    override fun createItem(path: String, isFolder: Boolean, name: String): ApiResponse<Boolean> {
        return try {
            val uri = "$path/$name"
            if (isFolder) sardine.createDirectory(uri) else sardine.put(uri, ByteArray(0))
            ApiResponse(true,null)
        }
        catch (exp: Exception){
            ApiResponse(false,exp)
        }
    }

    override fun deleteItem(path: String): ApiResponse<Boolean> {
        return try {
            sardine.delete(path)
            ApiResponse(true,null)
        }
        catch (exp: Exception){
            ApiResponse(false,exp)
        }
    }

    override fun writeFileToCache(url: String, context: Context): ApiResponse<File> {
        return try {
            val localFile = File(context.cacheDir, Uri.parse(url).lastPathSegment)
            scope.launch(Dispatchers.IO) {
                try {
                    sardine.get(url).use { input ->
                        localFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            ApiResponse(localFile, null)

        } catch (exp: Exception) {
            ApiResponse(null, exp)
        }
    }

    override fun listWebDavFileDetail(url: String): ApiResponse<DavResource?> {

        return try {
            val resources = sardine.list(url)
            var resource:DavResource? = null
            if (resources.isNotEmpty()) {
                resource = resources[0]
            }
            ApiResponse(resource,null)
        }
        catch (exp: Exception){
            ApiResponse(null,null)
        }
    }

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
