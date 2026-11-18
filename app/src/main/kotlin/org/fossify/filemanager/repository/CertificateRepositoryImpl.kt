package org.fossify.filemanager.repository

import android.content.Context
import android.net.Uri
import org.fossify.filemanager.interfaces.CertificateRepository
import org.fossify.filemanager.keyStores.CertificateStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class CertificateRepositoryImpl: CertificateRepository {
    override fun loadCertificate(uri: Uri, context: Context): X509Certificate {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open URI")

        val cf = CertificateFactory.getInstance("X.509")
        return cf.generateCertificate(inputStream) as X509Certificate
    }

    override fun saveCertificate(host: String, cert: X509Certificate, context: Context) {
        CertificateStore.saveCert(context, host, cert)
    }
}
