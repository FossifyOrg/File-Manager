package org.fossify.filemanager.dependencies

import android.content.Context
import androidx.room.Room
import org.fossify.filemanager.database.Database
import org.fossify.filemanager.factory.NetworkBrowserViewModelFactory
import org.fossify.filemanager.repository.CertificateRepositoryImpl
import org.fossify.filemanager.repository.FTPApiImpl
import org.fossify.filemanager.repository.NetworkConnectionRepositoryDbImpl
import org.fossify.filemanager.repository.SFTPApiImpl
import org.fossify.filemanager.repository.SMBApiImpl
import org.fossify.filemanager.repository.WebDavApiImpl

class AppComposition (private val context: Context) {

    private fun createDataBase(context: Context): Database {
        return Room.databaseBuilder(context.applicationContext, Database::class.java,"app-db").build()
    }

    private val database by lazy {
        createDataBase(context)
    }

    val networkDbRepository by lazy {
        NetworkConnectionRepositoryDbImpl(database.networkConnectionDao())
    }


    val webDavApiRepository by lazy {
        WebDavApiImpl()
    }

    val smbApiRepository by lazy {
        SMBApiImpl()
    }

    val sftpApiRepository by lazy {
        SFTPApiImpl()
    }

    val ftpApiRepository by lazy {
        FTPApiImpl()
    }


    val certificateRepository by lazy {
        CertificateRepositoryImpl()
    }

    fun provideNetworkBrowserViewModelFactory(): NetworkBrowserViewModelFactory{
        return NetworkBrowserViewModelFactory(networkDbRepository,webDavApiRepository,sftpApiRepository,ftpApiRepository,smbApiRepository)
    }
}
