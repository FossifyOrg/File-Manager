package org.fossify.filemanager.extensions

import net.lingala.zip4j.model.LocalFileHeader
import java.io.File

fun File.setLastModified(localFileHeader: LocalFileHeader) {
    setLastModified(localFileHeader.lastModifiedOrCurrentTimeMillis)
}

private val LocalFileHeader.lastModifiedOrCurrentTimeMillis
    get() = if (lastModifiedTimeEpoch == 0L) {
        System.currentTimeMillis()
    } else {
        lastModifiedTimeEpoch
    }
