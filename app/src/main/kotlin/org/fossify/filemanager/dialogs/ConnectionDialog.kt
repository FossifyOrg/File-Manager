package org.fossify.filemanager.dialogs

import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.enums.ConnectionTypes
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.value
import org.fossify.filemanager.R
import org.fossify.filemanager.activities.CloudActivity
import org.fossify.filemanager.databinding.DialogAddConnectionBinding
import org.fossify.filemanager.enums.Protocols

class ConnectionDialog(val activity: BaseSimpleActivity, dispatch: (String, String, String, String, String, Uri?, Int, ConnectionTypes, Protocols?) -> Unit) {
    private var binding: DialogAddConnectionBinding
    val items = listOf(ConnectionTypes.DAVx5.type, ConnectionTypes.SMB.type, ConnectionTypes.WebDav.type, ConnectionTypes.SFTP.type, ConnectionTypes.FTP.type)
    private var certUri: Uri? = null
    val protocols = listOf(Protocols.HTTP, Protocols.HTTPS)

    init {
        binding = DialogAddConnectionBinding.inflate(activity.layoutInflater)
        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { _, _ ->
                dispatch(
                    binding.hostEt.value,
                    binding.userEt.value,
                    binding.passwordEt.value,
                    binding.sharedPathEt.value,
                    binding.displayEt.value,
                    certUri,
                    binding.portEt.value.toIntOrNull() ?: 0,
                    ConnectionTypes.fromType(binding.dropdownMenu.value),
                    binding.dropdownMenuProtocol.text
                        ?.toString()
                        ?.takeIf { it.isNotBlank() }
                        ?.let { Protocols.valueOf(it) }
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this)
            }
        initializeDropDownList()
        dropDownItemSelected()
        attachCertBtnClickListener()
    }


    private fun initializeDropDownList() {
        val adapter = ArrayAdapter(activity, android.R.layout.simple_list_item_1, items)
        binding.dropdownMenu.setAdapter(adapter)
        val protocolsAdapter = ArrayAdapter(activity,android.R.layout.simple_list_item_1, protocols)
        binding.dropdownMenuProtocol.setAdapter(protocolsAdapter)
    }


    private fun promptUserToSelectStorage() {
        (activity as CloudActivity).promptUserToSelectStorage()
    }


    private fun dropDownItemSelected() {
        binding.dropdownMenu.setOnItemClickListener { parent, view, position, id ->
            val selectedItem = parent.getItemAtPosition(position).toString()
            if (selectedItem == ConnectionTypes.DAVx5.type) {
                binding.dropdownProtocol.visibility = View.GONE
                promptUserToSelectStorage()
            } else if (selectedItem == ConnectionTypes.WebDav.type) {
                binding.dropdownProtocol.visibility = View.VISIBLE
            } else {
                binding.dropdownProtocol.visibility = View.GONE
            }
            Toast.makeText(activity, "Selected: $selectedItem", Toast.LENGTH_SHORT).show()
        }
    }

    private fun attachCertBtnClickListener() {
        binding.certAttachBtn.setOnClickListener {
            (activity as CloudActivity).openFileLink{
                certUri = it
                binding.certStatusTv.text = it.path
            }

        }
    }

}
