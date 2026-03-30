package org.fossify.filemanager.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "network_connections")
data class NetworkConnectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val host: String,
    val port: Int = 445,
    val username: String?,
    val password: String?,
    val displayName: String,
    val connectionType: String,
    val sharedPath: String
)
