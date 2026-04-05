package org.fossify.filemanager.models

import org.fossify.filemanager.entity.NetworkConnectionEntity

data class NetworkConnection(
    val host: String = "", val port: Int = 445, val username: String? = "",
    val password: String? = "", val displayName: String = "",val connectionType: String, val sharedPath: String, val url: String = ""
)


