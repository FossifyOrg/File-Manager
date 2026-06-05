package org.fossify.filemanager.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thegrizzlylabs.sardineandroid.DavResource
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.sftp.SFTPClient
import org.apache.commons.net.ftp.FTPFile
import org.fossify.filemanager.enums.Protocols
import org.fossify.filemanager.interfaces.FTPApi
import org.fossify.filemanager.interfaces.NetworkConnectionRepositoryDb
import org.fossify.filemanager.interfaces.SFTPApi
import org.fossify.filemanager.interfaces.SMBApi
import org.fossify.filemanager.interfaces.WebDavApi
import org.fossify.filemanager.models.ApiResponse
import org.fossify.filemanager.models.ConnectionResult
import org.fossify.filemanager.models.NetworkConnection
import java.io.File

class NetworkBrowserViewModel(
    private val networkConnectionRepository: NetworkConnectionRepositoryDb,
    private val webDavApi: WebDavApi,
    private val ftpApi: FTPApi,
    private val sftpApi: SFTPApi,
    private val smbApi: SMBApi
) : ViewModel() {

    val savedNetworks = MutableStateFlow<List<NetworkConnection>>(emptyList())
    val verifyNetwork = MutableSharedFlow<ConnectionResult>()
    val smbFolderOrFile = MutableSharedFlow<ApiResponse<Boolean>>()
    val smbDelete = MutableSharedFlow<ApiResponse<Boolean>>()
    val smbFileShare = MutableSharedFlow<ApiResponse<File>>()

    val verifyWebDav = MutableSharedFlow<ConnectionResult>()

    val verifySFTP = MutableSharedFlow<ConnectionResult>()
    val verifyFTP = MutableSharedFlow<ConnectionResult>()


    val sftpFiles = MutableStateFlow<ApiResponse<List<RemoteResourceInfo>>?>(null)
    val sftpFolderOrFile = MutableSharedFlow<ApiResponse<Boolean>>()
    val sftpDelete = MutableSharedFlow<ApiResponse<Boolean>>()
    val sftpFileShare = MutableSharedFlow<ApiResponse<File>>()

    val webDavFiles = MutableStateFlow< ApiResponse<List<DavResource>>?>(null)
    val webDavFolderOrFile = MutableSharedFlow<ApiResponse<Boolean>>()
    val webDavDelete = MutableSharedFlow<ApiResponse<Boolean>>()
    val webDavFileShare = MutableSharedFlow<ApiResponse<File>>()

    val ftpFiles = MutableStateFlow<ApiResponse<List<FTPFile>>?>(null)
    val ftpFolderOrFile = MutableSharedFlow<ApiResponse<Boolean>>()
    val ftpDelete = MutableSharedFlow<ApiResponse<Boolean>>()
    val ftpFileShare = MutableSharedFlow<ApiResponse<File>>()



    fun insertUpdateConnection(networkConnection: NetworkConnection) {
        viewModelScope.launch(Dispatchers.IO) {
            networkConnectionRepository.insertUpdateConnection(networkConnection)
        }
    }

    fun getAllSavedNetworks() {
        viewModelScope.launch(Dispatchers.IO) {
            val connections = networkConnectionRepository.getAllSavedConnections().collectLatest { value ->
                savedNetworks.emit(value)
            }
        }
    }

    fun deleteConnection(connection: NetworkConnection){
        viewModelScope.launch(Dispatchers.IO) {
            networkConnectionRepository.deleteConnection(connection)
        }
    }

    fun verifyNetwork(connection: NetworkConnection, saveInfo: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val value = smbApi.verifyConnection(connection)
            verifyNetwork.emit(ConnectionResult(connection, value.first, saveInfo = saveInfo,value.second))
        }
    }

    fun getFilesFromNetworkPath(path: String): ApiResponse<Array<SmbFile>> {
        return smbApi.getFilesFromNetworkPath(path)
    }

    fun createFolderOrFileSMB(path: String, isFolder: Boolean, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            smbFolderOrFile.emit(smbApi.createFolderOrFile(path, isFolder,name))
        }
    }

    fun deleteItemSMB(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            smbDelete.emit(smbApi.deleteItem(path))
        }
    }

    fun writeSmbFileToCache(path: String,context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            smbFileShare.emit(smbApi.writeFileToCache(path,context))
        }
    }


    fun getMainSmb(): SmbFile = smbApi.getMainSmbFile()

    fun getSFTPConn(): SFTPClient = sftpApi.getSFTPConn()

    fun connectAndAuthenticateWebDav(connection: NetworkConnection, protocol: Protocols, saveInfo: Boolean, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = webDavApi.connectAndVerifyWebDav(connection, protocol, context)
            verifyWebDav.emit(ConnectionResult(connection, result.first, saveInfo,result.second))
        }
    }

    fun listWebDavFiles(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            webDavFiles.emit(webDavApi.listAllFilesOnWebDav(url))
        }
    }

    fun createItem(path: String, isFolder: Boolean, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            webDavFolderOrFile.emit(webDavApi.createItem(path, isFolder,name))
        }
    }

    fun deleteItemWebDav(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            webDavDelete.emit(webDavApi.deleteItem(path))
        }
    }

    fun writeWebDavFileToCache(url: String,context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            webDavFileShare.emit(webDavApi.writeFileToCache(url,context))
        }
    }

//    fun listWebDavFileStream(url: String, start: Long, end: Long): InputStream {
//        return webDavApi.getWebDavFileInputStream(url, start, end)
//    }

//    fun listWebDavFileDetail(url: String): DavResource? {
//        return webDavApi.listWebDavFileDetail(url)
//    }

    fun connectSFTP(connection: NetworkConnection, saveInfo: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = sftpApi.connectToSftp(connection)
            verifySFTP.emit(ConnectionResult(connection, res.first, saveInfo,res.second))
        }
    }

    fun listAllFilesSFTPRoot(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = sftpApi.listAllFilesSFTPRoot(path)
            sftpFiles.emit(res)
        }
    }

    fun createItemSFTP(path: String, isFolder: Boolean, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            sftpFolderOrFile.emit(sftpApi.createItem(path, isFolder,name))
        }
    }

    fun deleteItemSFTP(path: String,isFolder: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            sftpDelete.emit(sftpApi.deleteItem(path,isFolder))
        }
    }

    fun writeSftpFileToCache(path: String,context: Context){
        viewModelScope.launch(Dispatchers.IO) {
            sftpFileShare.emit(sftpApi.writeFileToCache(path,context))
        }
    }

//    fun listAllFilesSFTPPath(path: String) {
//        viewModelScope.launch(Dispatchers.IO) {
//            val res = sftpApi.listAllFilesSFTPRoot(path)
//            sftpFiles.emit(res)
//        }
//    }
//
//    fun listSFTPFileDetails(path: String): FileAttributes? {
//        return sftpApi.listSFTPFileDetails(path)
//    }
//
//    fun getSFTPFileStream(path: String, startByte: Long): InputStream {
//        return sftpApi.getSFTPFileInputStream(url = path, startByte)
//    }

    fun connectFTP(connection: NetworkConnection, saveInfo: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = ftpApi.connectToFTP(connection)
            verifyFTP.emit(ConnectionResult(connection, res.first, saveInfo,res.second))
        }
    }

    fun getFTP() = ftpApi.getFTPConn()

    fun listAllFTPFiles(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = ftpApi.listAllFTPFiles(path)
            ftpFiles.emit(res)
        }
    }

    fun createItemFTP(path: String, isFolder: Boolean, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            ftpFolderOrFile.emit(sftpApi.createItem(path, isFolder,name))
        }
    }

    fun deleteItemFTP(path: String,isFolder: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            ftpDelete.emit(sftpApi.deleteItem(path,isFolder))
        }
    }

    fun writeFtpFileToCache(path: String,context: Context){
        viewModelScope.launch(Dispatchers.IO) {
            ftpFileShare.emit(sftpApi.writeFileToCache(path,context))
        }
    }



    fun getFTPFileDetail(path: String) = ftpApi.getFTPFileDetail(path)

    fun getFTPFileStream(path: String, start: Long) = ftpApi.getFTPFileInputStream(path, start)
}
