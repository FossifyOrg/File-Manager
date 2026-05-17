package org.fossify.filemanager.mapper

import android.util.Log
import com.thegrizzlylabs.sardineandroid.DavResource
import jcifs.smb.SmbFile
import net.schmizz.sshj.sftp.RemoteResourceInfo
import org.apache.commons.net.ftp.FTPFile
import org.fossify.commons.enums.ConnectionTypes
import org.fossify.commons.models.FileDirItem
import org.fossify.filemanager.entity.NetworkConnectionEntity
import org.fossify.filemanager.enums.Authentication
import org.fossify.filemanager.models.NetworkConnection

fun NetworkConnectionEntity.toDomain(): NetworkConnection {
    return NetworkConnection(
        host = host,
        port = port,
        username = username,
        password = password,
        displayName = displayName,
        connectionType = ConnectionTypes.valueOf(connectionType),
        sharedPath = sharedPath,
        url = url,
        authentication = Authentication.valueOf(authentication),
        privateKeyText = privateKey,
        privateKeyPass = privateKeyPass
    )
}

fun NetworkConnection.toEntity(): NetworkConnectionEntity {
    return NetworkConnectionEntity(
        host = host,
        port = port,
        username = username,
        password = password,
        displayName = displayName,
        connectionType = connectionType.toString(),
        sharedPath = sharedPath,
        url = url,
        authentication = authentication.toString(),
        privateKey = privateKeyText,
        privateKeyPass = privateKeyPass
    )
}

fun SmbFile.toFileItem(): FileDirItem {
    return FileDirItem(
        path = this.path,
        name = this.name.trimEnd('/'),
        isDirectory = this.isDirectory,
        size = if (!this.isDirectory) this.length() else 0L,
        modified = this.lastModified(),
        children = 0,
        mediaStoreId = 0L
    )
}

fun DavResource.toFileItem(): FileDirItem {
    return FileDirItem(
        path = this.path,
        name = this.name.trimEnd('/'),
        isDirectory = this.isDirectory,
        size = if (!this.isDirectory) (this.contentLength ?: 0L) else 0L,
        modified = this.modified?.time ?: 0L,
        children = 0,
        mediaStoreId = 0L
    )
}


fun RemoteResourceInfo.toFileItem(parentPath: String): FileDirItem {
    val attrs = this.attributes
    val cleanParent = parentPath.trimEnd('/')

    return FileDirItem(
        path = "$cleanParent/${this.name}",
        name = this.name,
        isDirectory = this.isDirectory,
        size = if (this.isRegularFile) attrs.size else 0L,
        modified = attrs.mtime * 1000L,
        children = 0,
        mediaStoreId = 0L
    )
}

fun FTPFile.toFileItem(parentPath: String): FileDirItem {
    val cleanParent = parentPath.trimEnd('/')
    return FileDirItem(
        path = "$cleanParent/${this.name}",
        name = this.name,
        isDirectory = this.isDirectory,
        size = if (this.isFile) this.size else 0L,
        modified = this.timestamp?.timeInMillis ?: 0L,
        children = 0,
        mediaStoreId = 0L
    )
}
