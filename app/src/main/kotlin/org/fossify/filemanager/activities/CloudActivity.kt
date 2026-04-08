package org.fossify.filemanager.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.filemanager.App
import org.fossify.filemanager.adapters.ConnectionItemsAdapter
import org.fossify.filemanager.databinding.CloudActivityBinding
import org.fossify.filemanager.dialogs.ConnectionDialog
import org.fossify.filemanager.enums.ConnectionTypes
import org.fossify.filemanager.fileSystems.HttpServer
import org.fossify.filemanager.helpers.CONNECTION_TYPE
import org.fossify.filemanager.helpers.PATH
import org.fossify.filemanager.helpers.PORT_SFTP
import org.fossify.filemanager.helpers.PORT_WEBDAV
import org.fossify.filemanager.models.NetworkConnection
import org.fossify.filemanager.viewmodels.NetworkBrowserViewModel


class CloudActivity : SimpleActivity() {
    private val binding by viewBinding(CloudActivityBinding::inflate)
    private lateinit var viewModel: NetworkBrowserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.apply {
            setupMaterialScrollListener(binding.connectionsList, binding.cloudAppbar)
        }
        setupToolBar()
        registerAddConnectionListener()
        val composition = (application as App).appComposition
        val factory = composition.provideNetworkBrowserViewModelFactory()

        viewModel = ViewModelProvider(this, factory)
            .get(NetworkBrowserViewModel::class.java)
        getAllSavedNetworks()
    }

    private val openDocumentTreeLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                val storage = DocumentFile.fromTreeUri(this, it)
                storage?.let { s ->
                    if (s.name != null && it.path != null) {
                        viewModel.saveNetwork(
                            NetworkConnection(
                                displayName = s.name!!,
                                sharedPath = s.uri.toString(),
                                connectionType = ConnectionTypes.ExternalStorage.toString()
                            )
                        )
                    }
                }
            }
        }

    fun promptUserToSelectStorage() {
        openDocumentTreeLauncher.launch(null)
    }

    private fun setupToolBar() {
        setupTopAppBar(binding.cloudAppbar, NavigationIcon.Arrow)
    }

    private fun showConnectionDialog() {
        ConnectionDialog(this@CloudActivity) { host, user, password, shared, displayName, port, connection ->
            saveNetwork(host, user, password, shared, displayName, port, connection)
        }
    }


    private fun registerAddConnectionListener() {
        binding.addButton.setOnClickListener {
            showConnectionDialog()
        }
    }

    private fun saveNetwork(host: String, user: String, password: String, shared: String, displayName: String, port: Int, connectionType: ConnectionTypes) {
        lifecycleScope.launch((Dispatchers.IO)) {
            if (connectionType == ConnectionTypes.SMB) {
                viewModel.authenticateAndSaveSMBNetwork(
                    NetworkConnection(
                        host = host,
                        username = user,
                        password = password,
                        sharedPath = shared,
                        connectionType = connectionType.toString(),
                        displayName = displayName
                    )
                )
            }
            if (connectionType == ConnectionTypes.WebDav) {
                val url = "http://${host}:${port}/${shared}"
                viewModel.connectAndAuthenticateWebDav(user, password, url)
                viewModel.verifyWebDav.collectLatest {
                    if (it) {
                        viewModel.saveNetwork(
                            NetworkConnection(
                                host = host,
                                username = user,
                                password = password,
                                sharedPath = shared,
                                connectionType = connectionType.toString(),
                                displayName = displayName,
                                url = url,
                                port = port
                            )
                        )
                    }
                }
            }

            if (connectionType == ConnectionTypes.SFTP) {
                viewModel.connectSFTP(user, password, host, port)
                viewModel.verifySFTP.collectLatest {
                    if (it) {
                        viewModel.saveNetwork(
                            NetworkConnection(host = host, username = user, password = password, connectionType = connectionType.toString(), port = port, displayName = displayName, url = viewModel.getSFTPConn().pwd())
                        )
                    }
                }
            }
        }
    }

    private fun getAllSavedNetworks() {
        viewModel.getAllSavedNetworks()
        collectSavedNetworks()
    }

    private fun collectSavedNetworks() {
        lifecycleScope.launch {
            viewModel.savedNetworks.collectLatest {
                if (it.isNotEmpty())
                    updateAdapter(it.toMutableList())
            }
        }
    }

    private fun updateAdapter(listItems: MutableList<NetworkConnection>) {
        ConnectionItemsAdapter(this, listItems, binding.connectionsList) { item ->
            val itm = item as NetworkConnection
            if (itm.connectionType == ConnectionTypes.SMB.type) {
                viewModel.verifyNetwork(itm)
                lifecycleScope.launch {
                    viewModel.verifyNetwork.collectLatest { value ->
                        if (value) {
                            val path = "${item.host.trimEnd('/')}/${item.sharedPath.trimStart('/')}"
                            startServer(item, connectionType = ConnectionTypes.SMB, machinePort = itm.port)
                            launchMainActivity(ConnectionTypes.SMB, path)
                        } else {
                            Toast.makeText(this@CloudActivity, "Connection failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else if (itm.connectionType == ConnectionTypes.ExternalStorage.type) {
                launchMainActivity(ConnectionTypes.ExternalStorage, itm.sharedPath)
            } else if (itm.connectionType == ConnectionTypes.WebDav.type) {
                itm.username?.let { username ->
                    itm.password?.let { password ->
                        viewModel.connectAndAuthenticateWebDav(username, password, itm.url)
                        lifecycleScope.launch {
                            viewModel.verifyWebDav.collectLatest {
                                if (it) {
                                    startServer(item, PORT_WEBDAV, connectionType = ConnectionTypes.WebDav, machinePort = itm.port)
                                    launchMainActivity(ConnectionTypes.WebDav, itm.url)
                                }
                            }
                        }
                    }
                }
            } else if (item.connectionType == ConnectionTypes.SFTP.type) {
                itm.username?.let { username ->
                    itm.password?.let { password ->
                        viewModel.connectSFTP(username, password, itm.host, itm.port)
                        lifecycleScope.launch(Dispatchers.IO) {
                            viewModel.verifySFTP.collectLatest {
                                if(it){
                                    startServer(item, PORT_SFTP, connectionType = ConnectionTypes.SFTP, machinePort = itm.port)
                                    launchMainActivity(ConnectionTypes.SFTP,itm.url)
                                }
                            }
                        }
                    }
                }

            }

        }.apply {
            binding.connectionsList.adapter = this
        }
    }

    private fun launchMainActivity(connectionType: ConnectionTypes, path: String) {
        startActivity(Intent(this@CloudActivity, MainActivity::class.java).apply {
            putExtra(PATH, path)
            putExtra(CONNECTION_TYPE, connectionType)
        })
    }

    private fun startServer(connection: NetworkConnection, port: Int = 7871, connectionType: ConnectionTypes, machinePort: Int) {
        val https = HttpServer(port, connection.host, connectionType, viewModel, machinePort)
        https.start()
    }
}
