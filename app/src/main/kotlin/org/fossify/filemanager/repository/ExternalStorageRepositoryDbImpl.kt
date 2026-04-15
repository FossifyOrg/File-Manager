package org.fossify.filemanager.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.fossify.filemanager.dao.DocumentProviderDao
import org.fossify.filemanager.dao.NetworkConnectionDao
import org.fossify.filemanager.entity.DocumentProviderEntity
import org.fossify.filemanager.interfaces.ExternalStorageRepositoryDb

class ExternalStorageRepositoryDbImpl(private val dao: DocumentProviderDao): ExternalStorageRepositoryDb {
    val allDocs = MutableStateFlow<List<DocumentProviderEntity>>(emptyList())
    override fun saveDocumentInfo(doc: DocumentProviderEntity) {
        CoroutineScope(Dispatchers.IO).launch{
            dao.insert(doc)
        }
    }

    override fun getAllDocuments() {
        CoroutineScope(Dispatchers.IO).launch{
            dao.getAll()
        }
    }
}
