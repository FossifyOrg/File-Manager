package org.fossify.filemanager.mapper

import android.util.Log
import com.jcraft.jsch.ChannelSftp
import com.thegrizzlylabs.sardineandroid.DavResource
import jcifs.smb.SmbFile
import org.fossify.commons.models.FileDirItem
import org.fossify.filemanager.entity.NetworkConnectionEntity
import org.fossify.filemanager.models.NetworkConnection

fun NetworkConnectionEntity.toDomain(): NetworkConnection {
    return NetworkConnection(
        host = host,
        port = port,
        username = username,
        password = password,
        displayName = displayName,
        connectionType = connectionType,
        sharedPath = sharedPath,
        url = url
    )
}

fun NetworkConnection.toEntity(): NetworkConnectionEntity {
    return NetworkConnectionEntity(
        host = host,
        port = port,
        username = username,
        password = password,
        displayName = displayName,
        connectionType = connectionType,
        sharedPath = sharedPath,
        url = url
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


fun ChannelSftp.LsEntry.toFileItem(parentPath: String): FileDirItem {
    val attrs = this.attrs
    val cleanParent = parentPath.trimEnd('/')

    return FileDirItem(
        path = "$cleanParent/${this.filename}",
        name = this.filename.trimEnd('/'),
        isDirectory = attrs.isDir,
        size = if (!attrs.isDir) attrs.size else 0L,
        modified = attrs.mTime.toLong() * 1000L,
        children = 0,
        mediaStoreId = 0L
    )
}
