package org.fossify.filemanager.documentProvider

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import androidx.lifecycle.ViewModelProvider
import org.fossify.filemanager.App

import org.fossify.filemanager.viewmodels.NetworkBrowserViewModel

class ExternalStorageProvider: DocumentsProvider() {
    override fun openDocument(documentId: String?, mode: String?, signal: CancellationSignal?): ParcelFileDescriptor? {
        TODO("Not yet implemented")
    }

    override fun queryChildDocuments(
        parentDocumentId: String?,
        projection: Array<out String?>?,
        sortOrder: String?
    ): Cursor? {
        TODO("Not yet implemented")
    }

    override fun queryDocument(documentId: String?, projection: Array<out String?>?): Cursor? {
        TODO("Not yet implemented")
    }

    override fun queryRoots(projection: Array<out String?>?): Cursor? {
        val cursor = MatrixCursor(
            arrayOf(
                DocumentsContract.Root.COLUMN_ROOT_ID,
                DocumentsContract.Root.COLUMN_TITLE,
                DocumentsContract.Root.COLUMN_FLAGS
            )
        )

        val row = cursor.newRow()
        row.add(DocumentsContract.Root.COLUMN_ROOT_ID, "root")
        row.add(DocumentsContract.Root.COLUMN_TITLE, "My Remote Storage")
        row.add(DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.FLAG_SUPPORTS_CREATE)

        return cursor
    }

    override fun onCreate(): Boolean {
        val app = context?.applicationContext as App
        val composition = app.appComposition
        val factory = composition.provideNetworkBrowserViewModelFactory()
        return true
    }

}
