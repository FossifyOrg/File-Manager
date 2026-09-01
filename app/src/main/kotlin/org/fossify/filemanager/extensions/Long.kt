package org.fossify.filemanager.extensions

import java.util.Locale

/**
 * Format bytes to human-readable size using binary (base 2) calculation
 * This overrides the commons library's formatSize which uses decimal (base 10)
 * 
 * Uses standard binary units: B, KiB, MiB, GiB, TiB, PiB
 * where 1 KiB = 1024 bytes (instead of 1000 in decimal)
 */
@Suppress("MagicNumber")
fun Long.formatSize(): String {
    if (this <= 0) return "0 B"
    
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
    val base = 1024.0
    val digitGroups = (Math.log10(this.toDouble()) / Math.log10(base)).toInt()
    
    return if (digitGroups >= units.size) {
        val maxIndex = units.size - 1
        String.format(Locale.US, "%.1f %s", this / Math.pow(base, maxIndex.toDouble()), units[maxIndex])
    } else {
        val size = this / Math.pow(base, digitGroups.toDouble())
        if (size % 1.0 == 0.0) {
            String.format(Locale.US, "%d %s", size.toLong(), units[digitGroups])
        } else {
            String.format(Locale.US, "%.1f %s", size, units[digitGroups])
        }
    }
}
