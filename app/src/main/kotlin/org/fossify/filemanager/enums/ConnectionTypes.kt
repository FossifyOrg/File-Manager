package org.fossify.filemanager.enums

enum class ConnectionTypes(val type: String) {
    SMB("SMB"),
    WebDav("WebDav"),
    ExternalStorage("External Storage"),
    SFTP("SFTP"),
    Default("Default");

    companion object {
        fun fromType(value: String): ConnectionTypes {
            return entries.find { it.type == value } ?: Default
        }
    }
}
