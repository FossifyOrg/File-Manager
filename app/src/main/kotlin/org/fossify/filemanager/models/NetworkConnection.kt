package org.fossify.filemanager.models

import org.fossify.commons.enums.ConnectionTypes
import org.fossify.filemanager.entity.NetworkConnectionEntity
import org.fossify.filemanager.enums.Authentication

data class NetworkConnection(
    val host: String = "", val port: Int = 445, val username: String? = "",
    val password: String? = "", val displayName: String = "",val connectionType: ConnectionTypes, val sharedPath: String = "", val url: String = "",val authentication: Authentication = Authentication.Password
)


