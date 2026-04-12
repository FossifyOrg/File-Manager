package org.fossify.filemanager.dialogs

import android.widget.ArrayAdapter
import android.widget.Toast
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.enums.ConnectionTypes
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.value
import org.fossify.filemanager.R
import org.fossify.filemanager.activities.CloudActivity
import org.fossify.filemanager.databinding.DialogAddConnectionBinding

class ConnectionDialog(val activity: BaseSimpleActivity, dispatch: (String, String, String, String, String, Int, ConnectionTypes) -> Unit) {
    private var binding: DialogAddConnectionBinding
    val items = listOf(ConnectionTypes.DAVx5.type, ConnectionTypes.SMB.type, ConnectionTypes.WebDav.type, ConnectionTypes.SFTP.type, ConnectionTypes.FTP.type)

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
            if (selectedItem == ConnectionTypes.DAVx5.type) {
                promptUserToSelectStorage()
            }
            Toast.makeText(activity, "Selected: $selectedItem", Toast.LENGTH_SHORT).show()
        }
    }
}
