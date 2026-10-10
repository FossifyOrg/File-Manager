package org.fossify.filemanager.keyStores

import android.content.Context
import java.io.FileNotFoundException
import java.security.KeyStore
import java.security.cert.X509Certificate

object CertificateStore {
    private const val KEYSTORE_FILE = "webdav_server_certs.keystore"
    private const val KEYSTORE_PASSWORD = "webdav_ks_pass"
    fun loadOrCreateKeyStore(context: Context): KeyStore {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        return try {
            val fis = context.openFileInput(KEYSTORE_FILE)
            keyStore.load(fis, KEYSTORE_PASSWORD.toCharArray())
            keyStore
        } catch (e: FileNotFoundException) {
            keyStore.load(null, null)
            keyStore
        }
    }

    fun loadCert(context: Context, host: String): X509Certificate {
        val keyStore = loadOrCreateKeyStore(context)
        return keyStore.getCertificate(host) as X509Certificate
    }

    fun saveCert(context: Context, host: String, cert: X509Certificate) {
        val keyStore = loadOrCreateKeyStore(context)
        keyStore.setCertificateEntry(host, cert)

        val fos = context.openFileOutput(KEYSTORE_FILE, Context.MODE_PRIVATE)
        keyStore.store(fos, KEYSTORE_PASSWORD.toCharArray())
        fos.close()
    }

    fun removeCert(context: Context, host: String) {
        val keyStore = loadOrCreateKeyStore(context)
        if (keyStore.containsAlias(host)) {
            keyStore.deleteEntry(host)
            val fos = context.openFileOutput(KEYSTORE_FILE, Context.MODE_PRIVATE)
            keyStore.store(fos, KEYSTORE_PASSWORD.toCharArray())
            fos.close()
        }
    }
}
