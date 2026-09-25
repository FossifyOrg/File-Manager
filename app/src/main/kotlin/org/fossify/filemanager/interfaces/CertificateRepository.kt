package org.fossify.filemanager.interfaces

import android.content.Context
import android.net.Uri
import java.security.cert.X509Certificate

interface CertificateRepository {
    fun loadCertificate(uri: Uri, context: Context): X509Certificate
    fun saveCertificate(host: String, cert: X509Certificate,context: Context)
}
