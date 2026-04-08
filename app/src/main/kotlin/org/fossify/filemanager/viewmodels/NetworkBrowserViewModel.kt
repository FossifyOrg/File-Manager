package org.fossify.filemanager.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.SftpATTRS
import com.thegrizzlylabs.sardineandroid.DavResource
import jcifs.CIFSContext
import jcifs.Configuration
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
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
import java.io.InputStream

class NetworkBrowserViewModel(private val networkConnectionRepository: NetworkConnectionRepositoryDb, private val networkConnectionRepositoryApi: NetworkConnectionRepositoryApi): ViewModel() {

    val savedNetworks = MutableStateFlow<List<NetworkConnection>>(emptyList())
    val verifyNetwork = MutableSharedFlow<Boolean>()

    val verifyWebDav = MutableSharedFlow<Boolean>()

    val verifySFTP = MutableSharedFlow<Boolean>()

    val sftpFiles = MutableStateFlow<List<ChannelSftp.LsEntry>>(emptyList())

    val webDavFiles = MutableStateFlow<List<DavResource>>(emptyList())
    fun saveNetwork(networkConnection: NetworkConnection){
        viewModelScope.launch(Dispatchers.IO) {
            networkConnectionRepository.saveConnection(networkConnection)
        }
    }

    fun authenticateAndSaveSMBNetwork(networkConnection: NetworkConnection){
        viewModelScope.launch(Dispatchers.IO) {
            val config: Configuration = PropertyConfiguration(System.getProperties())
            val context: CIFSContext = BaseContext(config)
            val auth = NtlmPasswordAuthenticator(
                "",
                networkConnection.username,
                networkConnection.password
            )
            val authContext = context.withCredentials(auth)
            val smbUrl = "smb://${networkConnection.host}/${networkConnection.sharedPath}"

            val dir = SmbFile(smbUrl, authContext)
            if (dir.exists()) {
                saveNetwork(networkConnection)
            }
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

    fun getSFTPConn():ChannelSftp = networkConnectionRepositoryApi.getSFTPConn()

    fun connectAndAuthenticateWebDav(userName: String = "", password: String = "", url: String){
        viewModelScope.launch(Dispatchers.IO) {
           val result = networkConnectionRepositoryApi.connectAndVerifyWebDav(userName, password, url)
            verifyWebDav.emit(result)
        }
    }

    fun listWebDavFiles(url: String){
        viewModelScope.launch(Dispatchers.IO) {
          webDavFiles.emit(networkConnectionRepositoryApi.listAllFilesOnWebDav(url))
        }
    }

    fun listWebDavFileStream(url: String,start: Long,end: Long): InputStream{
        return networkConnectionRepositoryApi.listWebDavFileInputStream(url,start,end)
    }

    fun listWebDavFileDetail(url: String): DavResource?{
        return networkConnectionRepositoryApi.listWebDavFileDetail(url)
    }

    fun connectSFTP(userName: String, password: String,server: String,port: Int){
        viewModelScope.launch(Dispatchers.IO) {
           val res = networkConnectionRepositoryApi.connectToSftp(userName,password,server,port)
            verifySFTP.emit(res)
        }
    }

    fun listAllSFTPFile(path: String){
        viewModelScope.launch(Dispatchers.IO) {
            val res = networkConnectionRepositoryApi.listAllSFTPFiles(path)
            sftpFiles.emit(res)
        }
    }

    fun listSFTPFileDetails(path: String):SftpATTRS?{
        return networkConnectionRepositoryApi.listSFTPFileDetails(path)
    }

    fun getSFTPFileStream(path: String): InputStream{
        return  networkConnectionRepositoryApi.listSFTPFileInputStream(url = path)
    }
}
