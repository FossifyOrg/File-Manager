package org.fossify.filemanager.helpers

import android.net.Uri
import org.fossify.commons.enums.ConnectionTypes
import org.fossify.filemanager.enums.Protocols
import java.util.Locale.getDefault

object Helpers {
    val URL: String = "http://127.0.0.1"
    fun createProtocolUrl(connectionTypes: ConnectionTypes, path: String? = "", server: String = "", port: Int, protocols: Protocols = Protocols.HTTP): String{
        var protocol = Protocols.HTTP.toString().lowercase()
        if(connectionTypes.equals(ConnectionTypes.WebDav)){
            protocol = protocols.name.lowercase()
        }
        else if(connectionTypes.equals(ConnectionTypes.SMB)){
            protocol = ConnectionTypes.SMB.toString().lowercase()
        }
        val url = "${protocol}://${if (server.isEmpty()) URL else server }:${port}${path}"
        return url
    }

    fun createNanoHttpdUrl(connectionTypes: ConnectionTypes,path: String? = ""): String{
        val port = getPortForEachService(connectionTypes)
        return "${URL}:${port}${path}"
    }

    fun createUrl(connectionTypes: ConnectionTypes,path: String,server: String,port: Int = 0): String{
        if(connectionTypes == ConnectionTypes.SMB){
            return "${connectionTypes.toString().lowercase()}://${server}:${port}/${path}/"
        }
        return ""
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
    fun createProtocolPath(protocol: Protocols?, server: String, port:Int, path:String): String{
        return "${protocol?.name?.lowercase(getDefault())}://${server}:${port}/${path}"
    }

    fun retrievePath(url: String): String?{
        val uri = Uri.parse(url)
       return uri.path
    }
}
