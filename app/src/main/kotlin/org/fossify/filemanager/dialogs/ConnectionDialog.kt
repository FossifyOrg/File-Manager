package org.fossify.filemanager.dialogs

import android.net.Uri
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.enums.ConnectionTypes
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.isVisible
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
import org.fossify.filemanager.models.NetworkConnection

class ConnectionDialog(
    val activity: BaseSimpleActivity,
    val connection: NetworkConnection? = null,
    dispatch: (NetworkConnection, Uri?) -> Unit
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
        val dialog = activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->
                    alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(View.OnClickListener {
                        val isValid = validateFields()
                        if (isValid) {
                            val connection = createConnection()
                            dispatch(connection, certUri)
                            alertDialog.dismiss()
                        }
                    })
                }
            }
            .create()
        dropDownItemSelected()
        initializeDropDownList()
        registerAuthClickListener()
        attachCertBtnClickListener()
        attachPrivateKeyBtnClickListener()
        dropDownMenuProtocolItemClickListener()
        populateDialogValues()
        textFieldsListener()
    }


    private fun initializeDropDownList() {
        initializeConnectionsDropDown()
        initializeAuthDropdown()
        initializeProtocolDropDown()
    }

    private fun createConnection():NetworkConnection {
        val networkConnection = NetworkConnection(
            host = binding.hostEt.value,
            port = binding.portEt.value.toIntOrNull() ?: 445,
            username = binding.userEt.value,
            password = binding.passwordEt.value,
            displayName = binding.displayEt.value,
            connectionType = ConnectionTypes.fromType(binding.dropdownMenu.value),
            sharedPath = binding.sharedPathEt.value,
            url = "",
            privateKeyText = binding.privateKeyEt.value.trimIndent(),
            privateKeyPass = binding.privateKeyPassEt.value,
            authentication = Authentication.valueOf(
                binding.authDropDownMenu.text.toString()
            ),
            protocols = binding.dropdownMenuProtocol.text
                ?.toString()
                ?.takeIf { it.isNotBlank() }
                ?.let { Protocols.valueOf(it) },
        )

        connection?.let {
            networkConnection.id = it.id
        }
        return networkConnection
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

    private fun onAuthSelected(selectedItem: String) {
        if (binding.dropdownMenu.value == ConnectionTypes.SMB.type) {
            if (Authentication.valueOf(selectedItem) == Authentication.Anonymous) {
                toggleCredentialsVisibility(View.GONE)
                toggleSFTPAuthVisibility(View.GONE)
            } else {
                toggleCredentialsVisibility(View.VISIBLE)
                toggleSFTPAuthVisibility(View.GONE)
            }
        } else if (binding.dropdownMenu.value == ConnectionTypes.SFTP.type) {
            if (Authentication.valueOf(selectedItem) == Authentication.PrivateKey) {
                binding.userTf.visibility = View.VISIBLE
                binding.passwordTf.visibility = View.GONE
                toggleSFTPAuthVisibility(View.VISIBLE)
            } else {
                toggleCredentialsVisibility(View.VISIBLE)
                toggleSFTPAuthVisibility(View.GONE)
            }
        } else if (binding.dropdownMenu.value == ConnectionTypes.FTP.type) {
            if (Authentication.valueOf(selectedItem) == Authentication.Password) {
                toggleCredentialsVisibility(View.VISIBLE)
                toggleSFTPAuthVisibility(View.GONE)
            } else {
                toggleCredentialsVisibility(View.GONE)
                toggleSFTPAuthVisibility(View.GONE)
            }
        }
    }

    private fun registerAuthClickListener() {
        binding.authDropDownMenu.setOnItemClickListener { parent, view, position, id ->
            val selectedItem = parent.getItemAtPosition(position).toString()
            onAuthSelected(selectedItem)
        }
    }

    private fun toggleSFTPAuthVisibility(visibility: Int) {
        binding.privateKeyTf.visibility = visibility
        binding.privateKeyPassTf.visibility = visibility
    }

    private fun toggleSharedPathVisibility(visibility: Int) {
        binding.sharedPathTf.visibility = visibility
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
            handleConnectionTypeSelection(selectedItem)
        }
    }

    private fun handleConnectionTypeSelection(selectedItem: String) {
        togglePortValue(
            ConnectionTypes.valueOf(selectedItem),
            if (binding.dropdownMenuProtocol.value != "") Protocols.valueOf(binding.dropdownMenuProtocol.value) else Protocols.HTTP
        )
        if (selectedItem == ConnectionTypes.DAVx5.type) {
            binding.allFieldsExceptConnection.visibility = View.GONE
            promptUserToSelectStorage()
        } else if (selectedItem == ConnectionTypes.WebDav.type) {
            binding.allFieldsExceptConnection.visibility = View.VISIBLE
            binding.dropdownProtocol.visibility = View.VISIBLE
            binding.authDropDownLayout.visibility = View.GONE
            toggleSFTPAuthVisibility(View.GONE)
            toggleCredentialsVisibility(View.VISIBLE)
            toggleSharedPathVisibility(View.VISIBLE)
        } else if (selectedItem == ConnectionTypes.SMB.type) {
            binding.dropdownProtocol.visibility = View.GONE
            binding.certRow.visibility = View.GONE
            binding.allFieldsExceptConnection.visibility = View.VISIBLE
            binding.authDropDownLayout.visibility = View.VISIBLE
            toggleSFTPAuthVisibility(View.GONE)
            binding.authDropDownMenu.setAdapter(ArrayAdapter(activity, android.R.layout.simple_list_item_1, authentications))
            binding.authDropDownMenu.setText(authentications[0].toString(), false)
            toggleSharedPathVisibility(View.VISIBLE)
            onAuthSelected(authentications[0].toString())
        } else if (selectedItem == ConnectionTypes.SFTP.type) {
            binding.allFieldsExceptConnection.visibility = View.VISIBLE
            binding.authDropDownLayout.visibility = View.VISIBLE
            binding.dropdownProtocol.visibility = View.GONE
            binding.certRow.visibility = View.GONE
            if (Authentication.valueOf(binding.authDropDownMenu.value) == Authentication.Password) {
                toggleSFTPAuthVisibility(View.GONE)
            } else {
                toggleSFTPAuthVisibility(View.VISIBLE)
            }
            binding.authDropDownMenu.setAdapter(ArrayAdapter(activity, android.R.layout.simple_list_item_1, sftpAuthentications))
            binding.authDropDownMenu.setText(sftpAuthentications[0].toString(), false)
            onAuthSelected(sftpAuthentications[0].toString())
            toggleSharedPathVisibility(View.GONE)

        } else if (selectedItem == ConnectionTypes.FTP.type) {
            binding.allFieldsExceptConnection.visibility = View.VISIBLE
            binding.authDropDownLayout.visibility = View.VISIBLE
            binding.dropdownProtocol.visibility = View.GONE
            binding.certRow.visibility = View.GONE
            toggleSFTPAuthVisibility(View.GONE)
            if (Authentication.valueOf(binding.authDropDownMenu.value) == Authentication.Password) {
                toggleCredentialsVisibility(View.VISIBLE)
            } else {
                toggleCredentialsVisibility(View.GONE)
            }
            binding.authDropDownMenu.setAdapter(ArrayAdapter(activity, android.R.layout.simple_list_item_1, authentications))
            binding.authDropDownMenu.setText(authentications[0].toString(), false)
            onAuthSelected(authentications[0].toString())
            toggleSharedPathVisibility(View.GONE)
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

    private fun togglePortValue(connectionTypes: ConnectionTypes, protocols: Protocols) {
        when (connectionTypes) {
            ConnectionTypes.SMB -> binding.portEt.setText(DEFAULT_SMB_PORT.toString())
            ConnectionTypes.FTP -> binding.portEt.setText(DEFAULT_FTP_PORT.toString())
            ConnectionTypes.SFTP -> binding.portEt.setText(DEFAULT_SFTP_PORT.toString())
            ConnectionTypes.WebDav -> {
                if (protocols == Protocols.HTTP) {
                    binding.portEt.setText(DEFAULT_WEBDAV_HTTP_PORT.toString())
                } else {
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
        binding.privateKeyTf.setEndIconOnClickListener {
            (activity as CloudActivity).openFileLinkForPrivateKey {
                privateKeyUri = it
                privateKeyUri?.let { path ->
                    val inputStream = activity.contentResolver.openInputStream(path)
                    val keyText = inputStream?.bufferedReader().use { it?.readText() } ?: ""
                    binding.privateKeyEt.setText(keyText)
                }

            }
        }
    }


    private fun validateFields(): Boolean {
        binding.apply {
            if (hostTf.isVisible() && hostEt.value.isEmpty()) {
                hostTf.error = activity.getString(R.string.host_name_error)
                return false
            }
            if (userTf.isVisible() && userEt.value.isNullOrEmpty()) {
                userTf.error = activity.getString(R.string.user_name_error)
                return false
            }
            if (passwordTf.isVisible() && passwordEt.value.isNullOrEmpty()) {
                passwordTf.error = activity.getString(R.string.password_error)
                return false
            }
            if (sharedPathTf.isVisible() && sharedPathEt.value.isNullOrEmpty()) {
                sharedPathTf.error = activity.getString(R.string.shared_path_error)
                return false
            }
            if (displayTf.isVisible() && displayEt.value.isNullOrEmpty()) {
                displayTf.error = activity.getString(R.string.display_name_error)
                return false
            }
            if (portTf.isVisible() && portEt.value.isNullOrEmpty()) {
                portTf.error = activity.getString(R.string.port_error)
                return false
            }
            if (privateKeyTf.isVisible() && privateKeyEt.value.isNullOrEmpty()) {
                privateKeyTf.error = activity.getString(R.string.private_key_error)
                return false
            }
            if (privateKeyPassTf.isVisible() && privateKeyPassEt.value.isNullOrEmpty()) {
                privateKeyPassTf.error = activity.getString(R.string.private_key_pass_error)
                return false
            }
        }
        return true
    }

    private fun textFieldsListener() {
        binding.apply {
            hostEt.doAfterTextChanged { editable ->
                hostTf.error = null
            }

            userEt.doAfterTextChanged { editable ->
                userTf.error = null
            }

            passwordEt.doAfterTextChanged { editable ->
                passwordTf.error = null
            }

            sharedPathEt.doAfterTextChanged { editable ->
                sharedPathTf.error = null
            }

            displayEt.doAfterTextChanged { editable ->
                displayTf.error = null
            }

            portEt.doAfterTextChanged { editable ->
                portTf.error = null
            }

            privateKeyEt.doAfterTextChanged { editable ->
                privateKeyTf.error = null
            }

            privateKeyPassEt.doAfterTextChanged { editable ->
                privateKeyPassTf.error = null
            }
        }
    }

    private fun populateDialogValues() {
        connection?.let {
            binding.apply {
                hostEt.setText(it.host)
                userEt.setText(it.username)
                passwordEt.setText(it.password)
                sharedPathEt.setText(it.sharedPath)
                displayEt.setText(it.displayName)
                dropdownMenu.setText(it.connectionType.type, false)
                privateKeyEt.setText(it.privateKeyText)
                privateKeyPassEt.setText(it.privateKeyPass)
                dropdownMenuProtocol.setText(it.protocols.toString(), false)
                handleConnectionTypeSelection(it.connectionType.type)
                authDropDownMenu.setText(it.authentication.toString(), false)
                onAuthSelected(it.authentication.toString())
                portEt.setText(it.port.toString())
            }
        }
    }

}
