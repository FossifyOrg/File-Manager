package org.fossify.filemanager.fileSystems

import fi.iki.elonen.NanoHTTPD

import jcifs.smb.SmbFile
import jcifs.smb.SmbFileInputStream
import org.fossify.commons.enums.ConnectionTypes
import org.fossify.filemanager.dependencies.AppComposition
import org.fossify.filemanager.enums.Protocols
import org.fossify.filemanager.helpers.Helpers
import org.fossify.filemanager.models.ApiResponse
import java.io.BufferedInputStream
import java.io.InputStream

class HttpServer(
    private val port: Int,
    private val serverIp: String,
    private val connectionType: ConnectionTypes,
    private val composition: AppComposition,
    private val machinePort: Int,
    private val protocol: Protocols = Protocols.HTTP,
    private val dispatchException: (Exception) -> Unit
) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val rangeHeader = session.headers["range"]

        return when (connectionType) {
            ConnectionTypes.SMB -> handleSmb(uri, rangeHeader)
            ConnectionTypes.WebDav -> handleWebDav(uri, rangeHeader)
            ConnectionTypes.SFTP -> handleSftp(uri, rangeHeader)
            else -> handleFtp(uri, rangeHeader)
        }
    }

    private fun handleSmb(uri: String, rangeHeader: String?): Response {
        val file = SmbFile("smb://$serverIp/$uri")
        if (!file.exists()) return notFound()

        val fileLength = file.length()
        val (start, end) = parseRange(rangeHeader, fileLength)
            ?: return rangeNotSatisfiable()

        val stream = SmbFileInputStream(file).also { it.skipFully(start) }
        return try {
            buildResponse(stream, start, end, fileLength, MimeTypes.getMimeTypes(file.path))
        }
        catch (exp: Exception){
            dispatchException.invoke(exp)
            notFound()
        }
    }

    private fun handleWebDav(uri: String, rangeHeader: String?): Response {
        val url = buildUrl(uri)
        val apiResponse = composition.webDavApiRepository.listWebDavFileDetail(url)
        return handleResponse(apiResponse) {
            val file = apiResponse.response ?: return@handleResponse notFound()
            val (start, end) = parseRange(rangeHeader, file.contentLength)
                ?: return@handleResponse rangeNotSatisfiable()

        val streamResponse = composition.webDavApiRepository.getWebDavFileInputStream(url, start, end)
            val stream = handleStream<InputStream>(streamResponse) {
                BufferedInputStream(
                    streamResponse.response,
                    BUFFER_SIZE
                )
            }
            buildResponse(stream, start, end, file.contentLength, MimeTypes.getMimeTypes(uri))
        }
    }

    private fun handleSftp(uri: String, rangeHeader: String?): Response {
        val apiResponse = composition.sftpApiRepository.listSFTPFileDetails(uri)

        return handleResponse(apiResponse) {
            val file = apiResponse.response ?: return@handleResponse notFound()
            val (start, end) = parseRange(rangeHeader, file.size)
                ?: return@handleResponse rangeNotSatisfiable()

            val streamResponse = composition.sftpApiRepository.getSFTPFileInputStream(uri, start)
            val stream = handleStream<InputStream>(streamResponse) {
                BufferedInputStream(
                    streamResponse.response,
                    BUFFER_SIZE
                )
            }
            buildResponse(stream, start, end, file.size, MimeTypes.getMimeTypes(uri))
        }
    }

    private fun handleFtp(uri: String, rangeHeader: String?): Response {
        val apiResponse = composition.ftpApiRepository.getFTPFileDetail(uri)

        return handleResponse(apiResponse) {
            val file = apiResponse.response ?: return@handleResponse notFound()

            val (start, end) = parseRange(rangeHeader, file.size)
                ?: return@handleResponse rangeNotSatisfiable()

            val streamResponse = composition.ftpApiRepository.getFTPFileInputStream(uri, start)
            val stream = handleStream<InputStream>(streamResponse) {
                BufferedInputStream(
                    streamResponse.response,
                    BUFFER_SIZE
                )
            }
            buildResponse(stream, start, end, file.size, MimeTypes.getMimeTypes(uri))
        }
    }

    private fun parseRange(header: String?, fileLength: Long): Pair<Long, Long>? {
        var start = 0L
        var end = fileLength - 1

        if (header != null && header.startsWith("bytes=")) {
            val parts = header.removePrefix("bytes=").split("-")
            try {
                if (parts[0].isNotEmpty()) start = parts[0].toLong()
                if (parts.size > 1 && parts[1].isNotEmpty()) end = parts[1].toLong()
            } catch (_: NumberFormatException) {
            }
        }

        return if (start >= fileLength) null else start to end
    }

    private fun buildResponse(
        stream: InputStream?,
        start: Long,
        end: Long,
        fileLength: Long,
        mimeType: String?
    ): Response {
        val contentLength = end - start + 1
        return newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, mimeType, stream, contentLength).apply {
            addHeader("Accept-Ranges", "bytes")
            addHeader("Content-Length", contentLength.toString())
            addHeader("Content-Range", "bytes $start-$end/$fileLength")
            addHeader("Connection", "keep-alive")
        }
    }

    private fun buildUrl(path: String) =
        Helpers.createNanoHttpdUrl(connectionType, server = serverIp, path = path, port = machinePort, protocols = protocol)


    private fun notFound() =
        newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "File not found")

    private fun rangeNotSatisfiable() =
        newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, "")

    private fun serverError(message: String?) =
        newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, message ?: "Server error")


    private fun InputStream.skipFully(n: Long) {
        var remaining = n
        while (remaining > 0) {
            val skipped = skip(remaining)
            if (skipped <= 0) break
            remaining -= skipped
        }
    }

    private fun <T> handleResponse(
        apiResponse: ApiResponse<T>,
        callBack: () -> Response
    ): Response {
        return if (apiResponse.exception != null) {
            dispatchException.invoke(apiResponse.exception)
            serverError(apiResponse.exception.message)
        } else {
            callBack.invoke()
        }
    }

    private fun <T> handleStream(apiResponse: ApiResponse<T>, callBack: () -> InputStream): InputStream? {
        return if (apiResponse.exception != null) {
            dispatchException.invoke(apiResponse.exception)
            return null
        } else {
            callBack.invoke()
        }
    }

    companion object {
        private const val BUFFER_SIZE = 1024 * 1024
    }
}
