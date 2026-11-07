package org.fossify.filemanager.fileSystems

import android.webkit.MimeTypeMap
import org.fossify.filemanager.helpers.OPEN_AS_AUDIO
import org.fossify.filemanager.helpers.OPEN_AS_DEFAULT
import org.fossify.filemanager.helpers.OPEN_AS_IMAGE
import org.fossify.filemanager.helpers.OPEN_AS_TEXT
import org.fossify.filemanager.helpers.OPEN_AS_VIDEO
import java.util.Locale.getDefault

object MimeTypes {
    fun getMimeTypes(path: String?): String? {
        return getFileExtension(path)
    }

    private fun getFileExtension(path: String?): String? {
        var extension: String? = path?.substring(path.lastIndexOf(".") + 1)?.lowercase(getDefault())
        val mime = MimeTypeMap.getSingleton()
        extension = mime.getMimeTypeFromExtension(extension)
        return extension
    }

    public fun getMimeType(type: Int) = when (type) {
        OPEN_AS_DEFAULT -> ""
        OPEN_AS_TEXT -> "text/*"
        OPEN_AS_IMAGE -> "image/*"
        OPEN_AS_AUDIO -> "audio/*"
        OPEN_AS_VIDEO -> "video/*"
        else -> "*/*"
    }
}
