package org.fossify.filemanager.dialogs

import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.enums.ConnectionTypes
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.value
import org.fossify.filemanager.R
import org.fossify.filemanager.activities.CloudActivity
import org.fossify.filemanager.databinding.DialogAddConnectionBinding
import org.fossify.filemanager.enums.Authentication
import org.fossify.filemanager.enums.Protocols
import org.fossify.filemanager.helpers.DEFAULT_FTP_PORT
import org.fossify.filemanager.helpers.DEFAULT_SFTP_PORT
import org.fossify.filemanager.helpers.DEFAULT_SMB_PORT
import org.fossify.filemanager.helpers.DEFAULT_WEBDAV_HTTPS_PORT
import org.fossify.filemanager.helpers.DEFAULT_WEBDAV_HTTP_PORT
import java.io.File

class ConnectionDialog(
    val activity: BaseSimpleActivity,
    dispatch: (String, String, String, String, String, Uri?, String,String, Int, ConnectionTypes, Protocols?, Authentication) -> Unit
) {
    private var binding: DialogAddConnectionBinding
    val items = listOf(ConnectionTypes.DAVx5.type, ConnectionTypes.SMB.type, ConnectionTypes.WebDav.type, ConnectionTypes.SFTP.type, ConnectionTypes.FTP.type)
    private var certUri: Uri? = null
    private var privateKeyUri: Uri? = null

    val protocols = listOf(Protocols.HTTP, Protocols.HTTPS)

    val authentications = listOf(Authentication.Password, Authentication.Anonymous)
    val sftpAuthentications = listOf(Authentication.Password, Authentication.PrivateKey)


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
                    binding.privateKeyEt.value.trimIndent(),
                    binding.privateKeyPassEt.value,
                    binding.portEt.value.toIntOrNull() ?: 0,
                    ConnectionTypes.fromType(binding.dropdownMenu.value),
                    binding.dropdownMenuProtocol.text
                        ?.toString()
                        ?.takeIf { it.isNotBlank() }
                        ?.let { Protocols.valueOf(it) },
                    Authentication.valueOf(binding.authDropDownMenu.text.toString())
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this)
            }
        dropDownItemSelected()
        initializeDropDownList()
        registerAuthClickListener()
        attachCertBtnClickListener()
        attachPrivateKeyBtnClickListener()
        dropDownMenuProtocolItemClickListener()
    }


    private fun initializeDropDownList() {
        initializeConnectionsDropDown()
        initializeAuthDropdown()
        initializeProtocolDropDown()
    }

    private fun initializeConnectionsDropDown() {
        val adapter = ArrayAdapter(activity, android.R.layout.simple_list_item_1, items)
        binding.dropdownMenu.setAdapter(adapter)
        binding.dropdownMenu.setText(items[1], false)
    }

    private fun initializeProtocolDropDown() {
        val protocolsAdapter = ArrayAdapter(activity, android.R.layout.simple_list_item_1, protocols)
        binding.dropdownMenuProtocol.setAdapter(protocolsAdapter)
        binding.dropdownMenuProtocol.setText(protocols[0].toString(), false)
    }

    private fun initializeAuthDropdown() {
        val authAdapter = ArrayAdapter(activity, android.R.layout.simple_list_item_1, authentications)
        binding.authDropDownMenu.setAdapter(authAdapter)
        binding.authDropDownMenu.setText(authentications[0].toString(), false)
    }

    private fun registerAuthClickListener() {
        binding.authDropDownMenu.setOnItemClickListener { parent, view, position, id ->
            val selectedItem = parent.getItemAtPosition(position).toString()
            if (Authentication.valueOf(selectedItem) == Authentication.Anonymous) {
                toggleCredentialsVisibility(View.GONE)
                toggleSFTPAuthVisibility(View.GONE)
            }
            else if(Authentication.valueOf(selectedItem) == Authentication.PrivateKey && binding.dropdownMenu.value == ConnectionTypes.SFTP.type){
                binding.userTf.visibility = View.VISIBLE
                binding.passwordTf.visibility = View.GONE
                toggleSFTPAuthVisibility(View.VISIBLE)
            }
            else if(Authentication.valueOf(selectedItem) == Authentication.Password && binding.dropdownMenu.value == ConnectionTypes.SFTP.type){
                binding.userTf.visibility = View.VISIBLE
                binding.passwordTf.visibility = View.VISIBLE
                toggleSFTPAuthVisibility(View.GONE)
            }
            else {
                toggleCredentialsVisibility(View.VISIBLE)
                toggleSFTPAuthVisibility(View.GONE)
            }
        }
    }

    private fun toggleSFTPAuthVisibility(visibility: Int) {
        binding.privateKeyTf.visibility = visibility
        binding.privateKeyPassTf.visibility = visibility
    }

    private fun toggleCredentialsVisibility(visibility: Int) {
        binding.userTf.visibility = visibility
        binding.passwordTf.visibility = visibility
    }

    private fun promptUserToSelectStorage() {
        (activity as CloudActivity).promptUserToSelectStorage()
    }


    private fun dropDownItemSelected() {
        binding.dropdownMenu.setOnItemClickListener { parent, view, position, id ->
            val selectedItem = parent.getItemAtPosition(position).toString()
            togglePortValue(ConnectionTypes.valueOf(selectedItem), if(binding.dropdownMenuProtocol.value != "") Protocols.valueOf(binding.dropdownMenuProtocol.value) else Protocols.HTTP)
            if (selectedItem == ConnectionTypes.DAVx5.type) {
                binding.allFieldsExceptConnection.visibility = View.GONE
                promptUserToSelectStorage()
            } else if (selectedItem == ConnectionTypes.WebDav.type) {
                binding.allFieldsExceptConnection.visibility = View.VISIBLE
                binding.dropdownProtocol.visibility = View.VISIBLE
                binding.authDropDownLayout.visibility = View.GONE
                toggleSFTPAuthVisibility(View.GONE)
                toggleCredentialsVisibility(View.VISIBLE)
            }
            else if(selectedItem == ConnectionTypes.SMB.type){
                binding.dropdownProtocol.visibility = View.GONE
                binding.certRow.visibility = View.GONE
                binding.allFieldsExceptConnection.visibility = View.VISIBLE
                binding.authDropDownLayout.visibility = View.VISIBLE
                toggleSFTPAuthVisibility(View.GONE)
                binding.authDropDownMenu.setAdapter(ArrayAdapter(activity, android.R.layout.simple_list_item_1, authentications))
            }
            else if(selectedItem == ConnectionTypes.FTP.type || selectedItem == ConnectionTypes.SFTP.type){
                binding.allFieldsExceptConnection.visibility = View.VISIBLE
                binding.authDropDownLayout.visibility = View.VISIBLE
                binding.dropdownProtocol.visibility = View.GONE
                binding.certRow.visibility = View.GONE
                if(Authentication.valueOf(binding.authDropDownMenu.value) == Authentication.Password && selectedItem == ConnectionTypes.SFTP.type){
                    toggleSFTPAuthVisibility(View.GONE)
                }
                else{
                    toggleSFTPAuthVisibility(View.VISIBLE)
                }
                binding.authDropDownMenu.setAdapter(ArrayAdapter(activity, android.R.layout.simple_list_item_1, sftpAuthentications))
            }
        }
    }

    private fun dropDownMenuProtocolItemClickListener() {
        binding.dropdownMenuProtocol.setOnItemClickListener { parent, view, position, id ->
            val selectedItem = parent.getItemAtPosition(position).toString()
            val item = Protocols.valueOf(selectedItem)
            if (item == Protocols.HTTP) {
                binding.certRow.visibility = View.GONE
            } else {
                binding.certRow.visibility = View.VISIBLE
            }
            togglePortValue(ConnectionTypes.WebDav, item)
        }
    }

    private fun togglePortValue(connectionTypes: ConnectionTypes,protocols: Protocols){
        when(connectionTypes){
            ConnectionTypes.SMB -> binding.portEt.setText(DEFAULT_SMB_PORT.toString())
            ConnectionTypes.FTP -> binding.portEt.setText(DEFAULT_FTP_PORT.toString())
            ConnectionTypes.SFTP -> binding.portEt.setText(DEFAULT_SFTP_PORT.toString())
            ConnectionTypes.WebDav -> {
                if(protocols == Protocols.HTTP){
                    binding.portEt.setText(DEFAULT_WEBDAV_HTTP_PORT.toString())
                }
                else{
                    binding.portEt.setText(DEFAULT_WEBDAV_HTTPS_PORT.toString())
                }
            }
            else -> Unit
        }
    }

    private fun attachCertBtnClickListener() {
        binding.certAttachBtn.setOnClickListener {
            (activity as CloudActivity).openFileLinkForCert {
                certUri = it
                binding.certStatusTv.text = it.path
            }

        }
    }

    private fun attachPrivateKeyBtnClickListener() {
        binding.privateKeyTf.setEndIconOnClickListener  {
            (activity as CloudActivity).openFileLinkForPrivateKey{
                privateKeyUri = it
                privateKeyUri?.let {path->
                    val inputStream = activity.contentResolver.openInputStream(path)
                    val keyText = inputStream?.bufferedReader().use { it?.readText() } ?: ""
                    binding.privateKeyEt.setText(keyText)
                }

            }
        }
    }

}
