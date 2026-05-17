package org.fossify.filemanager.models

data class ApiResponse<T>(val response:T?,val exception: Exception?)
