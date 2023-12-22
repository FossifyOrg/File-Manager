package org.fossify.filemanager.dialogs

import android.view.View
import androidx.appcompat.app.AlertDialog
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.dialogs.FilePickerDialog
import org.fossify.commons.extensions.*
import org.fossify.filemanager.R
import org.fossify.filemanager.databinding.DialogCompressAsBinding
import org.fossify.filemanager.extensions.config

class CompressAsDialog(val activity: BaseSimpleActivity, val path: String, val callback: (destination: String, password: String?) -> Unit) {
    private val binding = DialogCompressAsBinding.inflate(activity.layoutInflater)

    init {
        val filename = path.getFilenameFromPath()
        val indexOfDot = if (filename.contains('.') && !activity.getIsPathDirectory(path)) filename.lastIndexOf(".") else filename.length
        val baseFilename = filename.substring(0, indexOfDot)
        var realPath = path.getParentPath()

        binding.apply {
            filenameValue.setText(baseFilename)

            folder.setText(activity.humanizePath(realPath))
            folder.setOnClickListener {
                FilePickerDialog(activity, realPath, false, activity.config.shouldShowHidden(), true, true, showFavoritesButton = true) {
                    folder.setText(activity.humanizePath(it))
                    realPath = it
                }
            }

            passwordProtect.setOnCheckedChangeListener { _, _ ->
                enterPasswordHint.beVisibleIf(passwordProtect.isChecked)
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.compress_as) { alertDialog ->
                    alertDialog.showKeyboard(binding.filenameValue)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(View.OnClickListener {
                        val name = binding.filenameValue.value
                        var password: String? = null
                        if (binding.passwordProtect.isChecked) {
                            password = binding.password.value
                            if (password.isEmpty()) {
                                activity.toast(R.string.empty_password_new)
                                return@OnClickListener
                            }
                        }
                        when {
                            name.isEmpty() -> activity.toast(R.string.empty_name)
                            name.isAValidFilename() -> {
                                val newPath = "$realPath/$name.zip"
                                if (activity.getDoesFilePathExist(newPath)) {
                                    activity.toast(R.string.name_taken)
                                    return@OnClickListener
                                }

                                alertDialog.dismiss()
                                callback(newPath, password)
                            }

                            else -> activity.toast(R.string.invalid_name)
                        }
                    })
                }
            }
    }
}
