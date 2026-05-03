package org.fossify.filemanager.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.toUpperCase
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.fossify.commons.enums.ConnectionTypes
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.filemanager.App
import org.fossify.filemanager.adapters.ConnectionItemsAdapter
import org.fossify.filemanager.databinding.CloudActivityBinding
import org.fossify.filemanager.dialogs.ConnectionDialog
import org.fossify.filemanager.fileSystems.HttpServer
import org.fossify.filemanager.helpers.CONNECTION_TYPE
import org.fossify.filemanager.helpers.PATH
import org.fossify.filemanager.helpers.PORT_SFTP
import org.fossify.filemanager.helpers.PORT_WEBDAV
import org.fossify.filemanager.models.NetworkConnection
import org.fossify.filemanager.viewmodels.NetworkBrowserViewModel
import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.fossify.filemanager.dependencies.AppComposition
import org.fossify.filemanager.enums.Protocols
import org.fossify.filemanager.helpers.Helpers
import org.fossify.filemanager.helpers.PORT_FTP
import org.fossify.filemanager.interfaces.CertificateRepository
import java.security.Provider
import java.util.Locale


class CloudActivity : SimpleActivity() {
    private val binding by viewBinding(CloudActivityBinding::inflate)
    private lateinit var viewModel: NetworkBrowserViewModel
    private var onCertPicked: ((Uri) -> Unit)? = null

    private lateinit var certificateRepository: CertificateRepository
    private lateinit var composition: AppComposition

    private fun setupBouncyCastle() {
        val provider: Provider? = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)
        if (provider == null) {
            return
        }
        if (provider.javaClass.equals(BouncyCastleProvider::class.java)) {
            return
        }
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.apply {
            setupMaterialScrollListener(binding.connectionsList, binding.cloudAppbar)
        }
        setupToolBar()
        registerAddConnectionListener()
        composition = (application as App).appComposition
        certificateRepository = composition.certificateRepository
        val factory = composition.provideNetworkBrowserViewModelFactory()
        setupBouncyCastle()
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
                                connectionType = ConnectionTypes.DAVx5.toString()
                            )
                        )
                    }
                }
            }
        }

    private val pickCert = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            onCertPicked?.invoke(it)
//            val stream = contentResolver.openInputStream(it) ?: return@let
//            val res = viewModel.loadSSLCertificate(inputStream = stream)
//            res.onSuccess { certificate ->
//                viewModel.attachCertificate(certificate, "TestUser", "bilal786")
//            }.onFailure {
//                Log.d("WebDav HTTPS", it.localizedMessage)
//            }
        }
    }

    fun promptUserToSelectStorage() {
        openDocumentTreeLauncher.launch(null)
    }


    fun openFileLink(dispatch: (Uri) -> Unit) {
        pickCert.launch("*/*")
        onCertPicked = dispatch
    }

    private fun setupToolBar() {
        setupTopAppBar(binding.cloudAppbar, NavigationIcon.Arrow)
    }

    private fun showConnectionDialog() {
        ConnectionDialog(this@CloudActivity) { host, user, password, shared, displayName, certPath, port, connection, protocol ->
            saveNetwork(host, user, password, shared, displayName, certPath, port, connection, protocol)
        }
    }


    private fun registerAddConnectionListener() {
        binding.addButton.setOnClickListener {
            showConnectionDialog()
        }
    }

    private fun saveNetwork(
        host: String,
        user: String,
        password: String,
        shared: String,
        displayName: String,
        certUri: Uri?,
        port: Int,
        connectionType: ConnectionTypes,
        protocol: Protocols
    ) {
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
            } else if (connectionType == ConnectionTypes.WebDav) {
                val url = Helpers.createProtocolPath(protocol, host, port, shared)
                if (protocol == Protocols.HTTPS) {
                    saveCertificate(certUri,host)
                }
                viewModel.connectAndAuthenticateWebDav(user, password, url,host,protocol,this@CloudActivity)
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
            } else if (connectionType == ConnectionTypes.SFTP) {
                viewModel.connectSFTP(user, password, host, port)
                viewModel.verifySFTP.collectLatest {
                    if (it) {
                        viewModel.saveNetwork(
                            NetworkConnection(
                                host = host,
                                username = user,
                                password = password,
                                connectionType = connectionType.toString(),
                                port = port,
                                displayName = displayName,
                                url = viewModel.getSFTPConn().canonicalize(".")
                            )
                        )
                    }
                }
            } else if (connectionType == ConnectionTypes.FTP) {
                viewModel.connectFTP(user, password, host, port)
                viewModel.verifyFTP.collectLatest {
                    if (it) {
                        viewModel.saveNetwork(
                            NetworkConnection(
                                host = host,
                                username = user,
                                password = password,
                                connectionType = connectionType.toString(),
                                port = port,
                                displayName = displayName,
                                url = viewModel.getFTP().printWorkingDirectory()
                            )
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
            } else if (itm.connectionType == ConnectionTypes.DAVx5.type) {
                launchMainActivity(ConnectionTypes.DAVx5, itm.sharedPath)
            } else if (itm.connectionType == ConnectionTypes.WebDav.type) {
                itm.username?.let { username ->
                    itm.password?.let { password ->
                        val protocol = itm.url.split(':')[0];
                        viewModel.connectAndAuthenticateWebDav(username, password, itm.url,itm.host, Protocols.valueOf(protocol.uppercase(Locale.getDefault())), this)
                        lifecycleScope.launch {
                            viewModel.verifyWebDav.collectLatest {
                                if (it) {
                                    startServer(item, PORT_WEBDAV, connectionType = ConnectionTypes.WebDav, machinePort = itm.port, Protocols.valueOf(protocol.uppercase(Locale.getDefault())))
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
                                if (it) {
                                    startServer(item, PORT_SFTP, connectionType = ConnectionTypes.SFTP, machinePort = itm.port)
                                    launchMainActivity(ConnectionTypes.SFTP, itm.url)
                                }
                            }
                        }
                    }
                }

            } else if (item.connectionType == ConnectionTypes.FTP.type) {
                itm.username?.let { username ->
                    itm.password?.let { password ->
                        viewModel.connectFTP(username, password, itm.host, itm.port)
                        lifecycleScope.launch(Dispatchers.IO) {
                            viewModel.verifyFTP.collectLatest {
                                if (it) {
                                    startServer(item, PORT_FTP, connectionType = ConnectionTypes.FTP, machinePort = itm.port)
                                    launchMainActivity(ConnectionTypes.FTP, itm.url)
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

    private fun startServer(connection: NetworkConnection, port: Int = 7871, connectionType: ConnectionTypes, machinePort: Int,protocol: Protocols = Protocols.HTTP) {
        val https = HttpServer(port, connection.host, connectionType, composition.networkApiRepository, machinePort,protocol)
        https.start()
    }

    private fun saveCertificate(uri: Uri?, name: String) {
        uri?.let {
            val cert = certificateRepository.loadCertificate(it, this)
            certificateRepository.saveCertificate(name, cert, this)
        }
    }
}
