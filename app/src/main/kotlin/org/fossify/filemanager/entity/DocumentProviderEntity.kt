package org.fossify.filemanager.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "document_provider_entity")
data class DocumentProviderEntity (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String
)
