package org.fossify.filemanager.dependencies

import android.content.Context
import androidx.room.Room
import org.fossify.filemanager.database.NetworkConnectionDatabase
import org.fossify.filemanager.factory.NetworkBrowserViewModelFactory
import org.fossify.filemanager.interfaces.NetworkConnectionRepositoryDb
import org.fossify.filemanager.repository.NetworkConnectionRepositoryApiImpl
import org.fossify.filemanager.repository.NetworkConnectionRepositoryDbImpl

class AppComposition (private val context: Context) {

    private fun createDataBase(context: Context): NetworkConnectionDatabase {
        return Room.databaseBuilder(context.applicationContext, NetworkConnectionDatabase::class.java,"app-db").build()
    }

    private val database by lazy {
        createDataBase(context)
    }

    val networkDbRepository by lazy {
        NetworkConnectionRepositoryDbImpl(database.networkConnectionDao())
    }

    val networkApiRepository by lazy {
        NetworkConnectionRepositoryApiImpl()
    }

    fun provideNetworkBrowserViewModelFactory(): NetworkBrowserViewModelFactory{
        return NetworkBrowserViewModelFactory(networkDbRepository,networkApiRepository)
    }
}
