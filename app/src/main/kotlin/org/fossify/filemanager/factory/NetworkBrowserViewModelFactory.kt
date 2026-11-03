package org.fossify.filemanager.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.fossify.filemanager.interfaces.FTPApi
import org.fossify.filemanager.interfaces.NetworkConnectionRepositoryDb
import org.fossify.filemanager.interfaces.SFTPApi
import org.fossify.filemanager.interfaces.SMBApi
import org.fossify.filemanager.interfaces.WebDavApi
import org.fossify.filemanager.viewmodels.NetworkBrowserViewModel

class NetworkBrowserViewModelFactory(private val networkConnectionRepositoryDb: NetworkConnectionRepositoryDb, private val webDavApi: WebDavApi,private val sftpApi: SFTPApi, private val ftpApi: FTPApi,private val smbApi: SMBApi): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NetworkBrowserViewModel(networkConnectionRepositoryDb,webDavApi,ftpApi,sftpApi,smbApi) as T
    }
}
