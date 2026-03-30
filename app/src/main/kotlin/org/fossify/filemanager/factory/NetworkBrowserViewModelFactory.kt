package org.fossify.filemanager.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.fossify.filemanager.interfaces.NetworkConnectionRepositoryApi
import org.fossify.filemanager.interfaces.NetworkConnectionRepositoryDb
import org.fossify.filemanager.viewmodels.NetworkBrowserViewModel

class NetworkBrowserViewModelFactory(private val networkConnectionRepositoryDb: NetworkConnectionRepositoryDb, private val networkConnectionRepositoryApi: NetworkConnectionRepositoryApi): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NetworkBrowserViewModel(networkConnectionRepositoryDb,networkConnectionRepositoryApi) as T
    }
}
