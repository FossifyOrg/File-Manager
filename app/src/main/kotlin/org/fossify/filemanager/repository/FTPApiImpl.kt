package org.fossify.filemanager.repository

import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPCmd
import org.apache.commons.net.ftp.FTPFile
import org.fossify.filemanager.interfaces.FTPApi
import org.fossify.filemanager.models.NetworkConnection
import java.io.File
import java.io.InputStream

class FTPApiImpl: FTPApi {
    private lateinit var currentStream: InputStream
    private lateinit var ftp: FTPClient
    private lateinit var ftpStream: FTPClient
    override suspend fun connectToFTP(connection: NetworkConnection): Boolean {
        try {
            ftp = FTPClient()
            ftpStream = FTPClient()
            ftp.connect(connection.host, connection.port)
            ftpStream.connect(connection.host, connection.port)

            val loginSuccess = ftp.login(connection.username, connection.password)
            ftpStream.login(connection.username,connection.password)

            if (!loginSuccess) {
                return false
            }
            ftp.enterLocalPassiveMode()
            ftpStream.enterLocalPassiveMode()
            return true
        } catch (exp: Exception) {
            return false
        }
    }

    override suspend fun listAllFTPFiles(path: String): List<FTPFile> {
        ftp.changeWorkingDirectory(path)
        val files: Array<FTPFile> = ftp.listFiles()
        return files.toList()
    }

    override fun getFTPFileDetail(path: String): FTPFile? {
        val myPath = path.replace("//", "/")
        if (ftp.hasFeature(FTPCmd.MLST)) {
            val file = ftp.mlistFile(myPath)
            return file
        }
        val mP = File(myPath)
        val files = ftp.listFiles(mP.parent).firstOrNull { it != null && it.name == mP.name }
        return files
    }

    override fun getFTPFileInputStream(path: String, start: Long): InputStream {
        if (::currentStream.isInitialized)
            currentStream.close()
        ftpStream.completePendingCommand()
        ftpStream.setFileType(FTP.BINARY_FILE_TYPE)
        ftpStream.restartOffset = start
        currentStream = ftpStream.retrieveFileStream(path)
        return currentStream
    }

    override fun getFTPConn(): FTPClient = ftp

}
