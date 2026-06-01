package org.fossify.filemanager.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import org.fossify.commons.dialogs.FilePickerDialog
import org.fossify.commons.extensions.getDocumentFile
import org.fossify.commons.extensions.getDoesFilePathExist
import org.fossify.commons.extensions.getFileOutputStreamSync
import org.fossify.commons.extensions.getFilenameFromContentUri
import org.fossify.commons.extensions.getFilenameFromPath
import org.fossify.commons.extensions.getMimeType
import org.fossify.commons.extensions.needsStupidWritePermissions
import org.fossify.commons.extensions.rescanPaths
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.filemanager.R
import org.fossify.filemanager.databinding.ActivitySaveAsBinding
import org.fossify.filemanager.extensions.config
import java.io.File
import java.io.IOException

class SaveAsMultipleActivity : SimpleActivity() {
    private val binding by viewBinding(ActivitySaveAsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        tryInitFileManager()
    }

    private fun tryInitFileManager() {
        handleStoragePermission { granted ->
            if (granted) {
                saveAsDialog()
            } else {
                toast(R.string.no_storage_permissions)
                finish()
            }
        }
    }

    private fun saveAsDialog() {
        if (intent.action == Intent.ACTION_SEND_MULTIPLE && intent.extras?.containsKey(Intent.EXTRA_STREAM) == true) {
            FilePickerDialog(
                this,
                pickFile = false,
                showHidden = config.shouldShowHidden(),
                showFAB = true,
                showFavoritesButton = true
            ) {
                val destination = it
                handleSAFDialog(destination) {
                    toast(R.string.saving)
                    ensureBackgroundThread {
                        try {
                            if (!getDoesFilePathExist(destination)) {
                                if (needsStupidWritePermissions(destination)) {
                                    val document = getDocumentFile(destination)
                                    document!!.createDirectory(destination.getFilenameFromPath())
                                } else {
                                    File(destination).mkdirs()
                                }
                            }

                            val sources = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)!!

                            sources.forEach { source ->
                                val originalFilename = getFilenameFromContentUri(source)
                                    ?: source.toString().getFilenameFromPath()

                                val filename = sanitizeFilename(originalFilename)

                                val mimeType = contentResolver.getType(source)
                                    ?: filename.getMimeType()

                                val inputStream = contentResolver.openInputStream(source)

                                val destinationPath = getAvailablePath("$destination/$filename")

                                val outputStream = getFileOutputStreamSync(destinationPath, mimeType, null)!!
                                inputStream!!.copyTo(outputStream)

                                val savedPaths = arrayListOf<String>()
                                rescanPaths(savedPaths)
                            }
                            val message = resources.getQuantityString(R.plurals.files_saved,sources.count())
                            toast(message)
                            finish()
                        } catch (e: IOException) {
                            showErrorToast(e)
                            finish()
                        } catch (e: SecurityException) {
                            showErrorToast(e)
                            finish()
                        }
                    }
                }
            }
        } else {
            toast(R.string.unknown_error_occurred)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.activitySaveAsAppbar, NavigationIcon.Arrow)
    }

    private fun sanitizeFilename(filename: String): String {
        return filename.replace("[/\\\\<>:\"|?*\u0000-\u001F]".toRegex(), "_")
            .takeIf { it.isNotBlank() } ?: "unnamed_file"
    }

    private fun getAvailablePath(destinationPath: String): String {
        if (!getDoesFilePathExist(destinationPath)) {
            return destinationPath
        }

        val file = File(destinationPath)
        return findAvailableName(file)
    }

    private fun findAvailableName(file: File): String {
        val parent = file.parent ?: return file.absolutePath
        val name = file.nameWithoutExtension
        val ext = if (file.extension.isNotEmpty()) ".${file.extension}" else ""

        var index = 1
        var newPath: String

        do {
            newPath = "$parent/${name}_$index$ext"
            index++
        } while (getDoesFilePathExist(newPath))

        return newPath
    }

}
