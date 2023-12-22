package org.fossify.filemanager.extensions

import java.text.DecimalFormat

// use 1000 instead of 1024 at dividing
fun Long.formatSizeThousand(): String {
    if (this <= 0) {
        return "0 B"
    }

    val units = arrayOf("B", "kB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(toDouble()) / Math.log10(1000.0)).toInt()
    return "${DecimalFormat("#,##0.#").format(this / Math.pow(1000.0, digitGroups.toDouble()))} ${units[digitGroups]}"
}
