package org.fossify.filemanager.dialogs

import androidx.appcompat.app.AlertDialog
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.dialogs.FilePickerDialog
import org.fossify.commons.extensions.*
import org.fossify.filemanager.R
import org.fossify.filemanager.databinding.DialogSaveAsBinding

class SaveAsDialog(
    val activity: BaseSimpleActivity, var path: String, val hidePath: Boolean,
    val callback: (path: String, filename: String) -> Unit
) {

    init {
        if (path.isEmpty()) {
            path = "${activity.internalStoragePath}/${activity.getCurrentFormattedDateTime()}.txt"
        }

        var realPath = path.getParentPath()
        val binding = DialogSaveAsBinding.inflate(activity.layoutInflater).apply {
            folderValue.setText(activity.humanizePath(realPath))

            val fullName = path.getFilenameFromPath()
            val dotAt = fullName.lastIndexOf(".")
            var name = fullName

            if (dotAt > 0) {
                name = fullName.substring(0, dotAt)
                val extension = fullName.substring(dotAt + 1)
                extensionValue.setText(extension)
            }

            filenameValue.setText(name)

            if (hidePath) {
                folderHint.beGone()
            } else {
                folderValue.setOnClickListener {
                    FilePickerDialog(activity, realPath, false, false, true, true, showFavoritesButton = true) {
                        folderValue.setText(activity.humanizePath(it))
                        realPath = it
                    }
                }
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.save_as) { alertDialog ->
                    alertDialog.showKeyboard(binding.filenameValue)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val filename = binding.filenameValue.value
                        val extension = binding.extensionValue.value

                        if (filename.isEmpty()) {
                            activity.toast(R.string.filename_cannot_be_empty)
                            return@setOnClickListener
                        }

                        var newFilename = filename
                        if (extension.isNotEmpty()) {
                            newFilename += ".$extension"
                        }

                        val newPath = "$realPath/$newFilename"
                        if (!newFilename.isAValidFilename()) {
                            activity.toast(R.string.filename_invalid_characters)
                            return@setOnClickListener
                        }

                        if (!hidePath && activity.getDoesFilePathExist(newPath)) {
                            val title = String.format(activity.getString(R.string.file_already_exists_overwrite), newFilename)
                            ConfirmationDialog(activity, title) {
                                callback(newPath, newFilename)
                                alertDialog.dismiss()
                            }
                        } else {
                            callback(newPath, newFilename)
                            alertDialog.dismiss()
                        }
                    }
                }
            }
    }
}
