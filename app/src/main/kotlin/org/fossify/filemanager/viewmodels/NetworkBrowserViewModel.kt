package org.fossify.filemanager.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import net.schmizz.sshj.sftp.FileAttributes
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.sftp.SFTPClient
import org.apache.commons.net.ftp.FTPFile
import org.fossify.filemanager.enums.Protocols
import org.fossify.filemanager.interfaces.NetworkConnectionRepositoryApi
import org.fossify.filemanager.interfaces.NetworkConnectionRepositoryDb
import org.fossify.filemanager.models.ConnectionResult
import org.fossify.filemanager.models.NetworkConnection
import java.io.InputStream

class NetworkBrowserViewModel(
    private val networkConnectionRepository: NetworkConnectionRepositoryDb,
    private val networkConnectionRepositoryApi: NetworkConnectionRepositoryApi
) : ViewModel() {

    val savedNetworks = MutableStateFlow<List<NetworkConnection>>(emptyList())
    val verifyNetwork = MutableSharedFlow<ConnectionResult>()

    val verifyWebDav = MutableSharedFlow<ConnectionResult>()

    val verifySFTP = MutableSharedFlow<ConnectionResult>()
    val verifyFTP = MutableSharedFlow<ConnectionResult>()


    val sftpFiles = MutableStateFlow<List<RemoteResourceInfo>>(emptyList())

    val webDavFiles = MutableStateFlow<List<DavResource>>(emptyList())

    val ftpFiles = MutableStateFlow<List<FTPFile>>(emptyList())


    fun saveNetwork(networkConnection: NetworkConnection) {
        viewModelScope.launch(Dispatchers.IO) {
            networkConnectionRepository.saveConnection(networkConnection)
        }
    }

    fun getAllSavedNetworks() {
        viewModelScope.launch(Dispatchers.IO) {
            val connections = networkConnectionRepository.getAllSavedConnections().collectLatest { value ->
                savedNetworks.emit(value)
            }
        }
    }

    fun verifyNetwork(connection: NetworkConnection, saveInfo: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val value = networkConnectionRepositoryApi.verifyConnection(connection)
            verifyNetwork.emit(ConnectionResult(connection, value, saveInfo = saveInfo))
        }
    }

    fun getFilesFromNetworkPath(): Array<SmbFile> {
        return networkConnectionRepositoryApi.getFilesFromNetworkPath()
    }

    fun getMainSmb(): SmbFile = networkConnectionRepositoryApi.getMainSmbFile()

    fun getSFTPConn(): SFTPClient = networkConnectionRepositoryApi.getSFTPConn()

    fun connectAndAuthenticateWebDav(connection: NetworkConnection, protocol: Protocols, saveInfo: Boolean, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = networkConnectionRepositoryApi.connectAndVerifyWebDav(connection, protocol, context)
            verifyWebDav.emit(ConnectionResult(connection, result, saveInfo))
        }
    }

    fun listWebDavFiles(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            webDavFiles.emit(networkConnectionRepositoryApi.listAllFilesOnWebDav(url))
        }
    }

    fun listWebDavFileStream(url: String, start: Long, end: Long): InputStream {
        return networkConnectionRepositoryApi.getWebDavFileInputStream(url, start, end)
    }

    fun listWebDavFileDetail(url: String): DavResource? {
        return networkConnectionRepositoryApi.listWebDavFileDetail(url)
    }

    fun connectSFTP(connection: NetworkConnection, saveInfo: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = networkConnectionRepositoryApi.connectToSftp(connection)
            verifySFTP.emit(ConnectionResult(connection, res, saveInfo))
        }
    }

    fun listAllFilesSFTPRoot(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = networkConnectionRepositoryApi.listAllFilesSFTPRoot(path)
            sftpFiles.emit(res)
        }
    }

    fun listAllFilesSFTPPath(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = networkConnectionRepositoryApi.listAllFilesSFTPRoot(path)
            sftpFiles.emit(res)
        }
    }

    fun listSFTPFileDetails(path: String): FileAttributes? {
        return networkConnectionRepositoryApi.listSFTPFileDetails(path)
    }

    fun getSFTPFileStream(path: String, startByte: Long): InputStream {
        return networkConnectionRepositoryApi.getSFTPFileInputStream(url = path, startByte)
    }

    fun connectFTP(connection: NetworkConnection, saveInfo: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = networkConnectionRepositoryApi.connectToFTP(connection)
            verifyFTP.emit(ConnectionResult(connection, res, saveInfo))

        }
    }

    fun getFTP() = networkConnectionRepositoryApi.getFTPConn()

    fun listAllFTPFiles(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = networkConnectionRepositoryApi.listAllFTPFiles(path)
            ftpFiles.emit(res)
        }
    }

    fun getFTPFileDetail(path: String) = networkConnectionRepositoryApi.getFTPFileDetail(path)

    fun getFTPFileStream(path: String, start: Long) = networkConnectionRepositoryApi.getFTPFileInputStream(path, start)
}
