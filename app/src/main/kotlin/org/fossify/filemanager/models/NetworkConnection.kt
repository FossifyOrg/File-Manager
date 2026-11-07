package org.fossify.filemanager.models

import org.fossify.commons.enums.ConnectionTypes
import org.fossify.filemanager.enums.Authentication
import org.fossify.filemanager.enums.Protocols

data class NetworkConnection(
    var id: Long = 0,
    var host: String = "",
    var port: Int = 445,
    var username: String? = "",
    var password: String? = "",
    var displayName: String = "",
    var connectionType: ConnectionTypes,
    var sharedPath: String = "",
    var url: String = "",
    var privateKeyText: String = "",
    var privateKeyPass: String = "",
    var authentication: Authentication = Authentication.Password,
    var protocols: Protocols? = Protocols.HTTP
)


