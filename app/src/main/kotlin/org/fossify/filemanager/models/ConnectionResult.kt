package org.fossify.filemanager.models

data class ConnectionResult(val item: NetworkConnection, val success: Boolean, val saveInfo: Boolean = true, val isAddCallOperation: Boolean = false, val exception: Exception? = null)
