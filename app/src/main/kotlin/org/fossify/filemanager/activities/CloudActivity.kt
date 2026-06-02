package org.fossify.filemanager.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import org.fossify.commons.extensions.toast
import org.fossify.filemanager.dependencies.AppComposition
import org.fossify.filemanager.enums.Authentication
import org.fossify.filemanager.enums.Protocols
import org.fossify.filemanager.helpers.DAVX5_PATH_NAME
import org.fossify.filemanager.helpers.Helpers
import org.fossify.filemanager.helpers.PORT_FTP
import org.fossify.filemanager.interfaces.CertificateRepository
import java.security.Provider
import java.util.Locale


class CloudActivity : SimpleActivity() {
    private val binding by viewBinding(CloudActivityBinding::inflate)
    private lateinit var viewModel: NetworkBrowserViewModel
    private var onCertPicked: ((Uri) -> Unit)? = null
    private var onPrivateKeyPicked: ((Uri) -> Unit)? = null

    private lateinit var certificateRepository: CertificateRepository
    private lateinit var composition: AppComposition
    private var https: HttpServer? = null

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
        initializeCompositionAndViewModel()
        startConnectionsCollection()
        setupBouncyCastle()
        getAllSavedNetworks()
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        stopServer()
    }

    private fun initializeCompositionAndViewModel() {
        composition = (application as App).appComposition
        certificateRepository = composition.certificateRepository
        val factory = composition.provideNetworkBrowserViewModelFactory()
        viewModel = ViewModelProvider(this, factory)
            .get(NetworkBrowserViewModel::class.java)
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
                                connectionType = ConnectionTypes.DAVx5
                            )
                        )
                    }
                }
            }
        }

    private val pickCert = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            onCertPicked?.invoke(it)
        }
    }

    private val pickPrivateKey = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            onPrivateKeyPicked?.invoke(it)
        }
    }

    fun promptUserToSelectStorage() {
        openDocumentTreeLauncher.launch(null)
    }


    fun openFileLinkForCert(dispatch: (Uri) -> Unit) {
        pickCert.launch("*/*")
        onCertPicked = dispatch
    }

    fun openFileLinkForPrivateKey(dispatch: (Uri) -> Unit) {
        pickPrivateKey.launch("*/*")
        onPrivateKeyPicked = dispatch
    }

    private fun setupToolBar() {
        setupTopAppBar(binding.cloudAppbar, NavigationIcon.Arrow)
    }

    private fun showConnectionDialog() {
        ConnectionDialog(this@CloudActivity) { host, user, password, shared, displayName, certPath, privateKeyText, privateKeyPass, port, connection, protocol, auth ->
            saveNetwork(host, user, password, shared, displayName, privateKeyText, privateKeyPass, certPath, port, connection, protocol, auth)
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
        privateKeyText: String,
        privateKeyPass: String,
        certUri: Uri?,
        port: Int,
        connectionType: ConnectionTypes,
        protocol: Protocols?,
        authentication: Authentication
    ) {
        if (connectionType == ConnectionTypes.SMB) {
            viewModel.verifyNetwork(
                NetworkConnection(
                    host = host,
                    username = user,
                    password = password,
                    sharedPath = shared,
                    connectionType = connectionType,
                    displayName = displayName,
                    authentication = authentication,
                    port = port
                ), true
            )
        } else if (connectionType == ConnectionTypes.WebDav) {
            if (protocol == Protocols.HTTPS) {
                saveCertificate(certUri, host)
            }
            val url = Helpers.createProtocolPath(protocol, host, port, shared)
            val network = NetworkConnection(
                host = host,
                username = user,
                password = password,
                connectionType = connectionType,
                port = port,
                displayName = displayName,
                authentication = authentication,
                url = url
            )
            viewModel.connectAndAuthenticateWebDav(network, protocol!!, true, this@CloudActivity)
        } else if (connectionType == ConnectionTypes.SFTP) {
            val network = NetworkConnection(
                host = host,
                username = user,
                password = password,
                connectionType = connectionType,
                port = port,
                displayName = displayName,
                authentication = authentication,
                privateKeyText = privateKeyText,
                privateKeyPass = privateKeyPass
            )
            viewModel.connectSFTP(network, true)
        } else if (connectionType == ConnectionTypes.FTP) {
            viewModel.connectFTP(
                NetworkConnection(
                    username = user,
                    password = password,
                    host = host,
                    port = port,
                    connectionType = ConnectionTypes.FTP,
                    authentication = authentication,
                    displayName = displayName,
                ), true
            )
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

    private fun handleConnection(item: NetworkConnection, connectionType: ConnectionTypes) {
        when (connectionType) {
            ConnectionTypes.SMB -> {
                viewModel.verifyNetwork(item, false)
            }

            ConnectionTypes.DAVx5 -> {
                launchMainActivity(ConnectionTypes.DAVx5, item.sharedPath,item.displayName)
            }

            ConnectionTypes.WebDav -> {
                val protocol = item.url.split(':')[0];
                viewModel.connectAndAuthenticateWebDav(
                    item,
                    Protocols.valueOf(protocol.uppercase(Locale.getDefault())),
                    false,
                    this@CloudActivity

                )
            }

            ConnectionTypes.SFTP -> {
                viewModel.connectSFTP(item, false)
            }

            ConnectionTypes.FTP -> {
                viewModel.connectFTP(item, false)
            }

            else -> Unit
        }
    }

    private fun updateAdapter(listItems: MutableList<NetworkConnection>) {
        ConnectionItemsAdapter(this, listItems, binding.connectionsList) { item ->
            lifecycleScope.launch {
                val itm = item as NetworkConnection
                handleConnection(itm, itm.connectionType)
            }
        }.apply {
            binding.connectionsList.adapter = this
        }
    }

    private fun launchMainActivity(connectionType: ConnectionTypes, path: String, name: String = "") {
        val intent = Intent(this@CloudActivity, MainActivity::class.java).apply {
            putExtra(PATH, path)
            putExtra(CONNECTION_TYPE, connectionType)
            if (name != ""){
                putExtra(DAVX5_PATH_NAME, name)
            }
        }
        launcher.launch(intent)
    }

    private fun startServer(
        connection: NetworkConnection,
        port: Int = 7871,
        connectionType: ConnectionTypes,
        machinePort: Int,
        protocol: Protocols = Protocols.HTTP
    ) {
        https = HttpServer(port, connection.host, connectionType, composition, machinePort, protocol) {
            it?.message?.let { exp ->
                toast(exp, Toast.LENGTH_LONG)
            }
        }
        https?.start()
    }

    private fun stopServer() {
        https?.stop()
        https = null
    }

    private fun startConnectionsCollection() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    handleResponse(ConnectionTypes.SMB)
                }
                launch {
                    handleResponse(ConnectionTypes.WebDav)
                }

                launch {
                    handleResponse(ConnectionTypes.SFTP)
                }

                launch {
                    handleResponse(ConnectionTypes.FTP)
                }
            }
        }
    }

    private suspend fun handleResponse(connectionType: ConnectionTypes) {
        when (connectionType) {
            ConnectionTypes.WebDav -> {
                viewModel.verifyWebDav.collectLatest {
                    if (it.success) {
                        if (!it.saveInfo) {
                            val protocol = it.item.url.split(':')[0];
                            startServer(
                                it.item,
                                PORT_WEBDAV,
                                connectionType = ConnectionTypes.WebDav,
                                machinePort = it.item.port,
                                Protocols.valueOf(protocol.uppercase(Locale.getDefault()))
                            )
                            launchMainActivity(ConnectionTypes.WebDav, it.item.url)
                        } else {
                            viewModel.saveNetwork(
                                NetworkConnection(
                                    host = it.item.host,
                                    username = it.item.username,
                                    password = it.item.password,
                                    sharedPath = it.item.sharedPath,
                                    connectionType = connectionType,
                                    displayName = it.item.displayName,
                                    url = it.item.url,
                                    port = it.item.port,
                                    authentication = it.item.authentication
                                )
                            )
                        }
                    } else {
                        it.exception?.let { exception ->
                            toast(exception.message.toString())
                        }
                    }
                }
            }

            ConnectionTypes.SMB -> {
                viewModel.verifyNetwork.collectLatest {
                    if (it.success) {
                        if (!it.saveInfo) {
                            val path = "smb://${it.item.host.trimEnd('/')}:${it.item.port}/${it.item.sharedPath.trimStart('/')}"
                            startServer(it.item, connectionType = ConnectionTypes.SMB, machinePort = it.item.port)
                            launchMainActivity(ConnectionTypes.SMB, path)
                        } else {
                            viewModel.saveNetwork(
                                NetworkConnection(
                                    host = it.item.host,
                                    username = it.item.username,
                                    password = it.item.password,
                                    connectionType = connectionType,
                                    displayName = it.item.displayName,
                                    sharedPath = it.item.sharedPath,
                                    authentication = it.item.authentication
                                )
                            )
                        }
                    } else {
                        it.exception?.let { exception ->
                            toast(exception.message.toString())
                        }
                    }
                }
            }

            ConnectionTypes.SFTP -> {
                viewModel.verifySFTP.collectLatest {
                    if (it.success) {
                        if (!it.saveInfo) {
                            startServer(it.item, PORT_SFTP, connectionType = ConnectionTypes.SFTP, machinePort = it.item.port)
                            launchMainActivity(ConnectionTypes.SFTP, it.item.url)
                        } else {
                            viewModel.saveNetwork(
                                NetworkConnection(
                                    host = it.item.host,
                                    username = it.item.username,
                                    password = it.item.password,
                                    connectionType = connectionType,
                                    port = it.item.port,
                                    displayName = it.item.displayName,
                                    url = "/",
                                    authentication = it.item.authentication,
                                    privateKeyText = it.item.privateKeyText,
                                    privateKeyPass = it.item.privateKeyPass
                                )
                            )
                        }
                    } else {
                        it.exception?.let { exception ->
                            toast(exception.message.toString())
                        }
                    }
                }
            }

            ConnectionTypes.FTP -> {
                viewModel.verifyFTP.collectLatest {
                    if (it.success) {
                        if (!it.saveInfo) {
                            startServer(it.item, PORT_FTP, connectionType = ConnectionTypes.FTP, machinePort = it.item.port)
                            launchMainActivity(ConnectionTypes.FTP, it.item.url)
                        } else {
                            viewModel.saveNetwork(
                                NetworkConnection(
                                    host = it.item.host,
                                    username = it.item.username,
                                    password = it.item.password,
                                    connectionType = connectionType,
                                    port = it.item.port,
                                    displayName = it.item.displayName,
                                    url = "/",
                                    authentication = it.item.authentication
                                )
                            )
                        }
                    } else {
                        it.exception?.let { exception ->
                            toast(exception.message.toString())
                        }
                    }
                }
            }

            ConnectionTypes.DAVx5 -> Unit
            else -> Unit
        }
    }


    private fun saveCertificate(uri: Uri?, name: String) {
        uri?.let {
            val cert = certificateRepository.loadCertificate(it, this)
            certificateRepository.saveCertificate(name, cert, this)
        }
    }
}
