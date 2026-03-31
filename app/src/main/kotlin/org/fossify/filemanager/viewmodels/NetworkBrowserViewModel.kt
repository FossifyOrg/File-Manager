package org.fossify.filemanager.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.fossify.filemanager.interfaces.NetworkConnectionRepositoryApi
import org.fossify.filemanager.interfaces.NetworkConnectionRepositoryDb
import org.fossify.filemanager.models.NetworkConnection

class NetworkBrowserViewModel(private val networkConnectionRepository: NetworkConnectionRepositoryDb, private val networkConnectionRepositoryApi: NetworkConnectionRepositoryApi): ViewModel() {

    val savedNetworks = MutableStateFlow<List<NetworkConnection>>(emptyList())
    val verifyNetwork = MutableSharedFlow<Boolean>()

    fun saveNetwork(networkConnection: NetworkConnection){
        viewModelScope.launch(Dispatchers.IO) {
            networkConnectionRepository.saveConnection(networkConnection)
        }
    }

    fun getAllSavedNetworks(){
        viewModelScope.launch(Dispatchers.IO){
            val connections = networkConnectionRepository.getAllSavedConnections().collectLatest { value ->
                savedNetworks.emit(value)
            }
        }
    }

    fun verifyNetwork(connection: NetworkConnection){
        viewModelScope.launch(Dispatchers.IO) {
            val value = networkConnectionRepositoryApi.verifyConnection(connection)
            verifyNetwork.emit(value)
        }
    }

    fun getFilesFromNetworkPath():Array<SmbFile>{
        return  networkConnectionRepositoryApi.getFilesFromNetworkPath()
    }

    fun getMainSmb(): SmbFile = networkConnectionRepositoryApi.getMainSmbFile()
}
