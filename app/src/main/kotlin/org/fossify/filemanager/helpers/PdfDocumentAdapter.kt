package org.fossify.filemanager.helpers

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import org.fossify.commons.extensions.getFilenameFromPath
import java.io.*

// taken from https://github.com/HarshitaBambure/AndroidPDFPrint/blob/master/app/src/main/java/com/example/androidpdfprint/PdfDocumentAdapter.java
class PdfDocumentAdapter(var context: Context, var path: String) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes,
        printAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        layoutResultCallback: LayoutResultCallback,
        extras: Bundle
    ) {
        if (cancellationSignal.isCanceled) layoutResultCallback.onLayoutCancelled() else {
            val builder = PrintDocumentInfo.Builder(path.getFilenameFromPath())
            builder.setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                .build()
            layoutResultCallback.onLayoutFinished(builder.build(), printAttributes != printAttributes)
        }
    }

    override fun onWrite(
        pages: Array<PageRange>,
        parcelFileDescriptor: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        writeResultCallback: WriteResultCallback
    ) {
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        try {
            val file = File(path)
            inputStream = FileInputStream(file)
            outputStream = FileOutputStream(parcelFileDescriptor.fileDescriptor)
            val buff = ByteArray(16384)
            var size: Int
            while (inputStream.read(buff).also { size = it } >= 0 && !cancellationSignal.isCanceled) {
                outputStream.write(buff, 0, size)
            }

            if (cancellationSignal.isCanceled) writeResultCallback.onWriteCancelled() else {
                writeResultCallback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            }
        } catch (e: Exception) {
            writeResultCallback.onWriteFailed(e.message)
        } finally {
            try {
                inputStream!!.close()
                outputStream!!.close()
            } catch (ex: IOException) {
            }
        }
    }
}
