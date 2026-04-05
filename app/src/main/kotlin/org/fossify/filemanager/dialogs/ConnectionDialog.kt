package org.fossify.filemanager.dialogs

import android.content.Intent
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.value
import org.fossify.filemanager.R
import org.fossify.filemanager.activities.CloudActivity
import org.fossify.filemanager.databinding.DialogAddConnectionBinding
import org.fossify.filemanager.databinding.DialogChangeViewTypeBinding
import org.fossify.filemanager.enums.ConnectionTypes

class ConnectionDialog(val activity: BaseSimpleActivity, dispatch: (String, String, String, String, String, Int, ConnectionTypes) -> Unit) {
    private var binding: DialogAddConnectionBinding
    val items = listOf(ConnectionTypes.ExternalStorage.type, ConnectionTypes.SMB.type, ConnectionTypes.WebDav.type, ConnectionTypes.SFTP.type)

    init {
        binding = DialogAddConnectionBinding.inflate(activity.layoutInflater)
        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { _, _ ->
                dispatch(
                    binding.hostEt.value, binding.userEt.value, binding.passwordEt.value, binding.sharedPathEt.value, binding.displayEt.value,binding.portEt.value.toIntOrNull() ?: 0,
                    ConnectionTypes.fromType(binding.dropdownMenu.value)
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this)
            }
        initializeDropDownList()
        dropDownItemSelected()
    }


    private fun initializeDropDownList() {
        val adapter = ArrayAdapter(activity, android.R.layout.simple_list_item_1, items)
        binding.dropdownMenu.setAdapter(adapter)
    }


    private fun promptUserToSelectStorage() {
        // This launches the system file picker
        (activity as CloudActivity).promptUserToSelectStorage()
    }

    private fun dropDownItemSelected() {
        binding.dropdownMenu.setOnItemClickListener { parent, view, position, id ->
            val selectedItem = parent.getItemAtPosition(position).toString()
            if (selectedItem == ConnectionTypes.ExternalStorage.type) {
                promptUserToSelectStorage()
            }
            Toast.makeText(activity, "Selected: $selectedItem", Toast.LENGTH_SHORT).show()
        }
    }
}
