package org.fossify.filemanager.extensions

import android.content.Context
import android.os.storage.StorageManager
import org.fossify.commons.extensions.isPathOnOTG
import org.fossify.commons.extensions.isPathOnSD
import org.fossify.commons.helpers.isNougatPlus
import org.fossify.filemanager.helpers.Config
import org.fossify.filemanager.helpers.PRIMARY_VOLUME_NAME
import java.util.Locale

val Context.config: Config get() = Config.newInstance(applicationContext)

fun Context.isPathOnRoot(path: String) = !(path.startsWith(config.internalStoragePath) || isPathOnOTG(path) || (isPathOnSD(path)))

fun Context.getAllVolumeNames(): List<String> {
    val volumeNames = mutableListOf(PRIMARY_VOLUME_NAME)
    if (isNougatPlus()) {
        val storageManager = getSystemService(Context.STORAGE_SERVICE) as StorageManager
        getExternalFilesDirs(null)
            .mapNotNull { storageManager.getStorageVolume(it) }
            .filterNot { it.isPrimary }
            .mapNotNull { it.uuid?.lowercase(Locale.US) }
            .forEach {
                volumeNames.add(it)
            }
    }
    return volumeNames
}
