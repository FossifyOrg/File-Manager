package org.fossify.filemanager.helpers

import org.fossify.commons.enums.ConnectionTypes
import org.fossify.filemanager.enums.Protocols
import java.nio.file.Path
import java.util.Locale
import java.util.Locale.getDefault

object Helpers {
    val host: String = "127.0.0.1"
    fun createUrl(connectionTypes: ConnectionTypes, path: String = "", server: String = "", port: Int,protocols: Protocols = Protocols.HTTP): String{
        var protocol = Protocols.HTTP.toString().lowercase()
        if(connectionTypes.equals(ConnectionTypes.WebDav)){
            protocol = protocols.name.lowercase()
        }
        else if(connectionTypes.equals(ConnectionTypes.SMB)){
            protocol = ConnectionTypes.SMB.toString().lowercase()
        }
        val url = "${protocol}://${if (server.isEmpty()) host else server }:${port}/${path}"
        return url
    }

    fun getPortForEachService(connectionTypes: ConnectionTypes): Int{
        if (connectionTypes.equals(ConnectionTypes.WebDav)){
            return PORT_WEBDAV
        }
        else if(connectionTypes.equals(ConnectionTypes.SMB)){
            return PORT_SMB
        }
        else if(connectionTypes.equals(ConnectionTypes.SFTP)){
            return PORT_SFTP
        }
        else if(connectionTypes.equals(ConnectionTypes.FTP)){
            return PORT_FTP
        }
        return PORT_SMB
    }

    fun createProtocolPath(protocol: Protocols, server: String, port:Int, path:String): String{
        return "${protocol.name.lowercase(getDefault())}://${server}:${port}/${path}"
    }
}
