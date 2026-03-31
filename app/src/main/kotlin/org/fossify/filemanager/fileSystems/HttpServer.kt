package org.fossify.filemanager.fileSystems

import fi.iki.elonen.NanoHTTPD
import java.io.FileInputStream

import jcifs.smb.SmbFile
import jcifs.smb.SmbFileInputStream

class HttpServer(private val port: Int, private val serverIp: String) : NanoHTTPD(port) {
    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri

        val smbUrl = "smb://${serverIp}${uri}"
        val file = SmbFile(smbUrl)

        if (!file.exists()) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File not found")
        }

        val fileLength = file.length()
        val rangeHeader = session.headers["range"]

        return handleRangeRequest(file, rangeHeader, fileLength)
    }

    private fun handleRangeRequest(
        file: SmbFile,
        rangeHeader: String?,
        fileLength: Long
    ): Response {

        var start: Long = 0
        var end = fileLength - 1

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            val ranges = rangeHeader.substring(6).split("-")
            try {
                if (ranges[0].isNotEmpty()) start = ranges[0].toLong()
                if (ranges.size > 1 && ranges[1].isNotEmpty()) end = ranges[1].toLong()
            } catch (e: NumberFormatException) {}
        }

        if (start >= fileLength) {
            return newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE, "text/plain", "")
        }

        val contentLength = end - start + 1

        val inputStream = SmbFileInputStream(file)
            var remaining = start
            while (remaining > 0) {
                val skipped = inputStream.skip(remaining)
                if (skipped <= 0) break
                remaining -= skipped
            }

        return newFixedLengthResponse(
            Response.Status.PARTIAL_CONTENT,
            MimeTypes.getMimeTypes(file.path),
            inputStream,
            contentLength
        ).apply {
            addHeader("Accept-Ranges", "bytes")
            addHeader("Content-Length", contentLength.toString())
            addHeader("Content-Range", "bytes $start-$end/$fileLength")
            addHeader("Connection", "keep-alive")
        }
    }


}
