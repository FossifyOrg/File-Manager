package org.fossify.filemanager.fileSystems

import fi.iki.elonen.NanoHTTPD

import jcifs.smb.SmbFile
import jcifs.smb.SmbFileInputStream
import org.fossify.filemanager.enums.ConnectionTypes
import org.fossify.filemanager.helpers.Helpers
import org.fossify.filemanager.viewmodels.NetworkBrowserViewModel

class HttpServer(
    private val port: Int,
    private val serverIp: String,
    private val connectionTypes: ConnectionTypes,
    private val viewModel: NetworkBrowserViewModel,
    private val machinePort: Int
) :
    NanoHTTPD(port) {
    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val rangeHeader = session.headers["range"]
        if (connectionTypes.equals(ConnectionTypes.SMB)) {
            val smbUrl = "smb://${serverIp}/${uri}"
            val file = SmbFile(smbUrl)
            if (!file.exists()) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File not found")
            }
            return handleRangeRequest(file, rangeHeader, file.length())
        }
        else if(connectionTypes.equals(ConnectionTypes.WebDav)) {
            val url = Helpers.createUrl(connectionTypes, server = serverIp, path = uri.toString(), port = machinePort)
            val webDavFile = viewModel.listWebDavFileDetail(url)
            return handleRangeRequestWebDav(rangeHeader, webDavFile?.contentLength!!, uri = uri, webDavFile.contentType)
        }
        val url = Helpers.createUrl(connectionTypes, server = serverIp, path = "", port = machinePort)
        val sftFile = viewModel.listSFTPFileDetails(url)
        return handleRangeRequestSFTPServer(rangeHeader, sftFile?.size!!, uri = uri, url)
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
            } catch (e: NumberFormatException) {
            }
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


    private fun handleRangeRequestWebDav(rangeHeader: String?, fileLength: Long, uri: String = "", contentType: String): Response {
        var start: Long = 0
        var end = fileLength - 1
        val url = Helpers.createUrl(connectionTypes, server = serverIp, path = uri, port = machinePort)
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            val ranges = rangeHeader.substring(6).split("-")
            try {
                if (ranges[0].isNotEmpty()) start = ranges[0].toLong()
                if (ranges.size > 1 && ranges[1].isNotEmpty()) end = ranges[1].toLong()
            } catch (e: NumberFormatException) {
            }
        }
        if (start >= fileLength) {
            return newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE, "text/plain", "")
        }

        val contentLength = end - start + 1

        val inputStream = viewModel.listWebDavFileStream(url = url,start,end)

        return newFixedLengthResponse(
            Response.Status.PARTIAL_CONTENT,
            MimeTypes.getMimeTypes(contentType),
            inputStream,
            contentLength
        ).apply {
            addHeader("Accept-Ranges", "bytes")
            addHeader("Content-Length", contentLength.toString())
            addHeader("Content-Range", "bytes $start-$end/$fileLength")
            addHeader("Connection", "keep-alive")
        }
    }

    private fun handleRangeRequestSFTPServer(rangeHeader: String?, fileLength: Long, uri: String = "", contentType: String): Response {
        var start: Long = 0
        var end = fileLength - 1
        val url = Helpers.createUrl(connectionTypes, server = serverIp, path = uri, port = machinePort)
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            val ranges = rangeHeader.substring(6).split("-")
            try {
                if (ranges[0].isNotEmpty()) start = ranges[0].toLong()
                if (ranges.size > 1 && ranges[1].isNotEmpty()) end = ranges[1].toLong()
            } catch (e: NumberFormatException) {
            }
        }
        if (start >= fileLength) {
            return newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE, "text/plain", "")
        }

        val contentLength = end - start + 1

        val inputStream = viewModel.getSFTPFileStream(url)
        var remaining = start
        while (remaining > 0) {
            val skipped = inputStream.skip(remaining)
            if (skipped <= 0) break
            remaining -= skipped
        }
        return newFixedLengthResponse(
            Response.Status.PARTIAL_CONTENT,
            MimeTypes.getMimeTypes(contentType),
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
