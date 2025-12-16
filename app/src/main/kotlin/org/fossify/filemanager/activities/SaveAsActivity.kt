package org.fossify.filemanager.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import org.fossify.commons.dialogs.FilePickerDialog
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.filemanager.R
import org.fossify.filemanager.databinding.ActivitySaveAsBinding
import org.fossify.filemanager.extensions.config
import java.io.File
import java.io.IOException

@Suppress("TooManyFunctions")
class SaveAsActivity : SimpleActivity() {
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
        when {
            intent.action == Intent.ACTION_SEND && intent.extras?.containsKey(Intent.EXTRA_STREAM) == true -> {
                handleSingleFile()
            }
            intent.action == Intent.ACTION_SEND_MULTIPLE && intent.extras?.containsKey(Intent.EXTRA_STREAM) == true -> {
                handleMultipleFiles()
            }
            else -> {
                toast(R.string.unknown_error_occurred)
                finish()
            }
        }
    }

    private fun handleSingleFile() {
        FilePickerDialog(this, pickFile = false, showHidden = config.shouldShowHidden(), showFAB = true, showFavoritesButton = true) {
            val destination = it
            handleSAFDialog(destination) {
                toast(R.string.saving)
                ensureBackgroundThread {
                    try {
                        createDestinationIfNeeded(destination)

                        val source = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
                        val originalFilename = getFilenameFromContentUri(source)
                            ?: source.toString().getFilenameFromPath()
                        val filename = sanitizeFilename(originalFilename)
                        val mimeType = contentResolver.getType(source)
                            ?: intent.type?.takeIf { it != "*/*" }
                            ?: filename.getMimeType()
                        val inputStream = contentResolver.openInputStream(source)

                        val destinationPath = getAvailablePath("$destination/$filename")
                        val outputStream = getFileOutputStreamSync(destinationPath, mimeType, null)!!
                        inputStream!!.copyTo(outputStream)
                        rescanPaths(arrayListOf(destinationPath))
                        toast(R.string.file_saved)
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
    }

    private fun handleMultipleFiles() {
        FilePickerDialog(this, pickFile = false, showHidden = config.shouldShowHidden(), showFAB = true, showFavoritesButton = true) { destination ->
            handleSAFDialog(destination) {
                toast(R.string.saving)
                ensureBackgroundThread {
                    processMultipleFiles(destination)
                }
            }
        }
    }

    private fun processMultipleFiles(destination: String) {
        try {
            createDestinationIfNeeded(destination)

            val uriList = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            if (uriList.isNullOrEmpty()) {
                runOnUiThread {
                    toast(R.string.no_items_found)
                    finish()
                }
                return
            }

            val result = saveAllFiles(destination, uriList)
            showFinalResult(result)
        } catch (e: IOException) {
            runOnUiThread {
                showErrorToast(e)
                finish()
            }
        } catch (e: SecurityException) {
            runOnUiThread {
                showErrorToast(e)
                finish()
            }
        }
    }

    private fun saveAllFiles(destination: String, uriList: ArrayList<Uri>): SaveResult {
        val mimeTypes = intent.getStringArrayListExtra(Intent.EXTRA_MIME_TYPES)
        val savedPaths = mutableListOf<String>()
        var successCount = 0
        var errorCount = 0

        for ((index, source) in uriList.withIndex()) {
            if (saveSingleFileItem(destination, source, index, mimeTypes)) {
                successCount++
                savedPaths.add(destination)
            } else {
                errorCount++
            }
        }

        if (savedPaths.isNotEmpty()) {
            rescanPaths(ArrayList(savedPaths))
        }

        return SaveResult(successCount, errorCount)
    }

    private fun saveSingleFileItem(
        destination: String,
        source: Uri,
        index: Int,
        mimeTypes: ArrayList<String>?): Boolean {
        return try {
            val originalFilename = getFilenameFromContentUri(source)
                ?: source.toString().getFilenameFromPath()
            val filename = sanitizeFilename(originalFilename)

            val mimeType = contentResolver.getType(source)
                ?: mimeTypes?.getOrNull(index)?.takeIf { it != "*/*" }
                ?: intent.type?.takeIf { it != "*/*" }
                ?: filename.getMimeType()

            val inputStream = contentResolver.openInputStream(source)
                ?: throw IOException(getString(R.string.error, source))

            val destinationPath = getAvailablePath("$destination/$filename")
            val outputStream = getFileOutputStreamSync(destinationPath, mimeType, null)
                ?: throw IOException(getString(R.string.error, source))

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: IOException) {
            showErrorToast(e)
            false
        } catch (e: SecurityException) {
            showErrorToast(e)
            false
        }
    }

    private fun showFinalResult(result: SaveResult) {
        runOnUiThread {
            when {
                result.successCount > 0 && result.errorCount == 0 -> {
                    toast(getString(R.string.file_saved))
                }
                result.successCount > 0 && result.errorCount > 0 -> {
                    toast(getString(R.string.files_saved_partially))
                }
                else -> {
                    toast(R.string.error)
                }
            }
            finish()
        }
    }

    private data class SaveResult(val successCount: Int, val errorCount: Int)
    private fun createDestinationIfNeeded(destination: String) {
        if (!getDoesFilePathExist(destination)) {
            if (needsStupidWritePermissions(destination)) {
                val document = getDocumentFile(destination)
                document!!.createDirectory(destination.getFilenameFromPath())
            } else {
                File(destination).mkdirs()
            }
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
