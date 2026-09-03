package org.fossify.filemanager.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.fossify.filemanager.enums.Authentication

@Entity(tableName = "network_connections",
    indices = [Index(value = ["displayName"], unique = true)]
)
data class NetworkConnectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val host: String,
    val port: Int = 445,
    val username: String?,
    val password: String?,
    val displayName: String,
    val connectionType: String,
    val sharedPath: String,
    val url: String,
    val authentication: String,
    val privateKey: String = "",
    val privateKeyPass: String = "",
    val protocols: String? = null
)
