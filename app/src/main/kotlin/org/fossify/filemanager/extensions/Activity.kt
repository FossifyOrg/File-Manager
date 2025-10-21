package org.fossify.filemanager.extensions

import android.app.Activity
import android.content.Intent
import androidx.core.content.FileProvider
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.getFilenameFromPath
import org.fossify.commons.extensions.getMimeTypeFromUri
import org.fossify.commons.extensions.getParentPath
import org.fossify.commons.extensions.launchActivityIntent
import org.fossify.commons.extensions.openPathIntent
import org.fossify.commons.extensions.renameFile
import org.fossify.commons.extensions.setAsIntent
import org.fossify.commons.extensions.sharePathsIntent
import org.fossify.filemanager.BuildConfig
import org.fossify.filemanager.helpers.OPEN_AS_AUDIO
import org.fossify.filemanager.helpers.OPEN_AS_DEFAULT
import org.fossify.filemanager.helpers.OPEN_AS_IMAGE
import org.fossify.filemanager.helpers.OPEN_AS_TEXT
import org.fossify.filemanager.helpers.OPEN_AS_VIDEO
import java.io.File

fun Activity.sharePaths(paths: ArrayList<String>) {
    sharePathsIntent(paths, BuildConfig.APPLICATION_ID)
}

fun Activity.tryOpenPathIntent(path: String, forceChooser: Boolean, openAsType: Int = OPEN_AS_DEFAULT, finishActivity: Boolean = false) {
    if (!forceChooser && path.endsWith(".apk", true)) {
        val uri = FileProvider.getUriForFile(
            this, "${BuildConfig.APPLICATION_ID}.provider", File(path)
        )

        Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(uri, getMimeTypeFromUri(uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            launchActivityIntent(this)
        }
    } else {
        openPath(path, forceChooser, openAsType)

        if (finishActivity) {
            finish()
        }
    }
}

fun Activity.openPath(path: String, forceChooser: Boolean, openAsType: Int = OPEN_AS_DEFAULT) {
    openPathIntent(path, forceChooser, BuildConfig.APPLICATION_ID, getMimeType(openAsType))
}

private fun getMimeType(type: Int) = when (type) {
    OPEN_AS_DEFAULT -> ""
    OPEN_AS_TEXT -> "text/*"
    OPEN_AS_IMAGE -> "image/*"
    OPEN_AS_AUDIO -> "audio/*"
    OPEN_AS_VIDEO -> "video/*"
    else -> "*/*"
}

fun Activity.setAs(path: String) {
    setAsIntent(path, BuildConfig.APPLICATION_ID)
}

fun BaseSimpleActivity.toggleItemVisibility(oldPath: String, hide: Boolean, callback: ((newPath: String) -> Unit)? = null) {
    val path = oldPath.getParentPath()
    var filename = oldPath.getFilenameFromPath()
    if ((hide && filename.startsWith('.')) || (!hide && !filename.startsWith('.'))) {
        callback?.invoke(oldPath)
        return
    }

    filename = if (hide) {
        ".${filename.trimStart('.')}"
    } else {
        filename.substring(1, filename.length)
    }

    val newPath = "$path/$filename"
    if (oldPath != newPath) {
        renameFile(oldPath, newPath, false) { success, useAndroid30Way ->
            callback?.invoke(newPath)
        }
    }
}
