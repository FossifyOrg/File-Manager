package org.fossify.filemanager.repository

import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPCmd
import org.apache.commons.net.ftp.FTPFile
import org.fossify.filemanager.interfaces.FTPApi
import org.fossify.filemanager.models.ApiResponse
import org.fossify.filemanager.models.NetworkConnection
import java.io.File
import java.io.InputStream

class FTPApiImpl: FTPApi {
    private lateinit var currentStream: InputStream
    private lateinit var ftp: FTPClient
    private lateinit var ftpStream: FTPClient
    override suspend fun connectToFTP(connection: NetworkConnection): Pair<Boolean, Exception?> {
       return try {
            ftp = FTPClient()
            ftpStream = FTPClient()
            ftp.connect(connection.host, connection.port)
            ftpStream.connect(connection.host, connection.port)

            val loginSuccess = ftp.login(connection.username, connection.password)
            ftpStream.login(connection.username,connection.password)

            if (!loginSuccess) {
                Pair(false, Exception("Login failed"))
            }
            ftp.enterLocalPassiveMode()
            ftpStream.enterLocalPassiveMode()
            Pair(true, null)
        } catch (exp: Exception) {
            Pair(false, exp)
        }
    }

    override suspend fun listAllFTPFiles(path: String): ApiResponse<List<FTPFile>> {
        return try {
            ftp.changeWorkingDirectory(path)
            val files: Array<FTPFile> = ftp.listFiles()
            val theFiles = files.toList()
            ApiResponse(theFiles,null)
        }
        catch (exp: Exception){
            ApiResponse(null,exp)
        }
    }

    override fun getFTPFileDetail(path: String): ApiResponse<FTPFile?> {
        return try {
            val myPath = path.replace("//", "/")
            if (ftp.hasFeature(FTPCmd.MLST)) {
                val file = ftp.mlistFile(myPath)
                ApiResponse(file,null)
            }
            val mP = File(myPath)
            val files = ftp.listFiles(mP.parent).firstOrNull { it != null && it.name == mP.name }
            ApiResponse(files,null)
        }
        catch (exp: Exception){
            ApiResponse(null,exp)
        }
    }

    override fun getFTPFileInputStream(path: String, start: Long): ApiResponse<InputStream> {
        return try {
            if (::currentStream.isInitialized)
                currentStream.close()
            ftpStream.completePendingCommand()
            ftpStream.setFileType(FTP.BINARY_FILE_TYPE)
            ftpStream.restartOffset = start
            currentStream = ftpStream.retrieveFileStream(path)
            ApiResponse(currentStream,null)
        }
        catch (exp: Exception){
            ApiResponse(null,exp)
        }
    }

    override fun getFTPConn(): FTPClient = ftp

}
