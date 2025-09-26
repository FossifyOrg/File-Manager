package org.fossify.filemanager.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import org.fossify.commons.dialogs.FilePickerDialog
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.helpers.isRPlus
import org.fossify.filemanager.R
import org.fossify.filemanager.databinding.ActivitySaveAsBinding
import org.fossify.filemanager.extensions.config
import java.io.File

class SaveAsActivity : SimpleActivity() {
    private val binding by viewBinding(ActivitySaveAsBinding::inflate)

    companion object {
        private const val MANAGE_STORAGE_RC = 201
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isExternalStorageManager()) {
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = "package:$packageName".toUri()
            startActivityForResult(intent, MANAGE_STORAGE_RC)
            return
        }
        setContentView(binding.root)

        if (intent.action == Intent.ACTION_SEND && intent.extras?.containsKey(Intent.EXTRA_STREAM) == true) {
            FilePickerDialog(this, pickFile = false, showHidden = config.shouldShowHidden(), showFAB = true, showFavoritesButton = true) {
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

                            val source = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
                            val originalFilename = getFilenameFromContentUri(source)
                                ?: source.toString().getFilenameFromPath()
                            val filename = sanitizeFilename(originalFilename)
                            val mimeType = contentResolver.getType(source)
                                ?: intent.type?.takeIf { it != "*/*" }
                                ?: filename.getMimeType()
                            val inputStream = contentResolver.openInputStream(source)

                            val destinationPath = "$destination/$filename"
                            val outputStream = getFileOutputStreamSync(destinationPath, mimeType, null)!!
                            inputStream!!.copyTo(outputStream)
                            rescanPaths(arrayListOf(destinationPath))
                            toast(R.string.file_saved)
                            finish()
                        } catch (e: Exception) {
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
        setupToolbar(binding.activitySaveAsToolbar, NavigationIcon.Arrow)
    }

    private fun sanitizeFilename(filename: String): String {
        return filename.replace("[/\\\\<>:\"|?*\u0000-\u001F]".toRegex(), "_")
            .takeIf { it.isNotBlank() } ?: "unnamed_file"
    }

    @SuppressLint("NewApi")
    override fun onActivityResult(requestCode: Int, resultCode: Int, dataIntent: Intent?) {
        super.onActivityResult(requestCode, resultCode, dataIntent)

        if (requestCode == MANAGE_STORAGE_RC && isRPlus()) {
            if (Environment.isExternalStorageManager()) {
                recreate()
            } else {
                toast(R.string.no_storage_permissions)
                finish()
            }
        }
    }
}
