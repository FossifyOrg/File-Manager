package org.fossify.filemanager.repository

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import okhttp3.OkHttpClient
import org.fossify.filemanager.enums.Protocols
import org.fossify.filemanager.interfaces.WebDavApi
import org.fossify.filemanager.keyStores.CertificateStore
import org.fossify.filemanager.models.NetworkConnection
import java.io.InputStream
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

class WebDavApiImpl: WebDavApi {
    lateinit var sardine: Sardine
    override suspend fun connectAndVerifyWebDav(
        connection: NetworkConnection,
        protocols: Protocols,
        context: Context
    ): Boolean {
        try {
            sardine = if (protocols == Protocols.HTTP) {
                OkHttpSardine()
            } else {
                createHTTPSSardine(context,connection.host)
            }
            sardine.setCredentials(connection.username, connection.password)
            return sardine.exists(connection.url)
        } catch (exp: Exception) {
            Log.d("WebDav", exp.toString())
            return false
        }
    }

    override suspend fun listAllFilesOnWebDav(url: String): List<DavResource> {
        val resources = sardine.list(url)
        return resources
    }

    override fun getWebDavFileInputStream(url: String, start: Long, end: Long): InputStream {
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
