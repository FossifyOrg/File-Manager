package org.fossify.filemanager.interfaces

import org.fossify.filemanager.entity.DocumentProviderEntity

interface ExternalStorageRepositoryDb {
    fun saveDocumentInfo(doc: DocumentProviderEntity)
    fun getAllDocuments()
}
