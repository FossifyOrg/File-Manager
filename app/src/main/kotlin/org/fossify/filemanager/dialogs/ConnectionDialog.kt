package org.fossify.filemanager.dialogs

import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.value
import org.fossify.filemanager.R
import org.fossify.filemanager.databinding.DialogAddConnectionBinding
import org.fossify.filemanager.databinding.DialogChangeViewTypeBinding

class ConnectionDialog(val activity: BaseSimpleActivity, dispatch:(String, String, String, String, String)-> Unit) {
    private var binding: DialogAddConnectionBinding

    init {
        binding = DialogAddConnectionBinding.inflate(activity.layoutInflater)
        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { _,_ -> dispatch(binding.hostEt.value,binding.userEt.value,binding.passwordEt.value,binding.sharedPathEt.value,binding.displayEt.value) }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this)
            }
    }
}
