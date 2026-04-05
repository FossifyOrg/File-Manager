package org.fossify.filemanager.helpers

import org.fossify.filemanager.enums.ConnectionTypes
import org.fossify.filemanager.enums.Protocols
import java.nio.file.Path

object Helpers {
    val host: String = "127.0.0.1"
    fun createUrl(connectionTypes: ConnectionTypes, path: String, server: String = "", port: Int): String{
        var protocol = Protocols.HTTP.toString().lowercase()
        if(connectionTypes.equals(ConnectionTypes.WebDav)){
            protocol = Protocols.HTTP.toString().lowercase()
        }
        if(connectionTypes.equals(ConnectionTypes.SMB)){
            protocol = Protocols.SMB.toString().lowercase()
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
        return PORT_SMB
    }
}
