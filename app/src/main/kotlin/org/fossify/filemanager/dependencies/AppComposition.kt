package org.fossify.filemanager.dependencies

import android.content.Context
import androidx.room.Room
import org.fossify.filemanager.database.Database
import org.fossify.filemanager.factory.NetworkBrowserViewModelFactory
import org.fossify.filemanager.repository.ExternalStorageRepositoryDbImpl
import org.fossify.filemanager.repository.NetworkConnectionRepositoryApiImpl
import org.fossify.filemanager.repository.NetworkConnectionRepositoryDbImpl

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

    val documentRepository by lazy {
        ExternalStorageRepositoryDbImpl(database.docDao())
    }

    val networkApiRepository by lazy {
        NetworkConnectionRepositoryApiImpl()
    }

    fun provideNetworkBrowserViewModelFactory(): NetworkBrowserViewModelFactory{
        return NetworkBrowserViewModelFactory(networkDbRepository,networkApiRepository)
    }
}
