package org.fossify.filemanager.interfaces

import org.fossify.commons.models.FileDirItem

interface ItemOperationsListener {
    fun refreshFragment()

    fun deleteFiles(files: ArrayList<FileDirItem>)

    fun selectedPaths(paths: ArrayList<String>)

    fun setupDateTimeFormat()

    fun setupFontSize()

    fun toggleFilenameVisibility()

    fun columnCountChanged()

    fun finishActMode()

    fun shareFile(paths: ArrayList<String>)

    fun openWith(path: String,mimType:String? = null)

    fun setAs(path: String)
}
