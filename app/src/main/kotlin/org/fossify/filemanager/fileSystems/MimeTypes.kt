package org.fossify.filemanager.fileSystems

import android.webkit.MimeTypeMap
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
}
