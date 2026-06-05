package org.fossify.filemanager.models

import org.fossify.commons.enums.ConnectionTypes
import org.fossify.filemanager.enums.Authentication

data class NetworkConnection(
    val id: Long = 0,
    val host: String = "",
    val port: Int = 445,
    val username: String? = "",
    val password: String? = "",
    val displayName: String = "",
    val connectionType: ConnectionTypes,
    val sharedPath: String = "",
    val url: String = "",
    val privateKeyText: String = "",
    val privateKeyPass: String = "",
    val authentication: Authentication = Authentication.Password
)


