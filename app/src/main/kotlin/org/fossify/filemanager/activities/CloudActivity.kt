package org.fossify.filemanager.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import jcifs.CIFSContext
import jcifs.Configuration
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.filemanager.App
import org.fossify.filemanager.adapters.ConnectionItemsAdapter
import org.fossify.filemanager.adapters.DecompressItemsAdapter
import org.fossify.filemanager.databinding.CloudActivityBinding
import org.fossify.filemanager.dialogs.ConnectionDialog
import org.fossify.filemanager.enums.ConnectionTypes
import org.fossify.filemanager.fileSystems.HttpServer
import org.fossify.filemanager.helpers.NETWORK_PATH
import org.fossify.filemanager.helpers.PATH
import org.fossify.filemanager.models.ListItem
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


    private fun setupToolBar() {
        setupTopAppBar(binding.cloudAppbar, NavigationIcon.Arrow)
    }

    private fun showConnectionDialog() {
        ConnectionDialog(this@CloudActivity) { host, user, password, shared, displayName ->
            listFiles(host, user, password, shared, displayName)
        }
    }

    private fun registerAddConnectionListener() {
        binding.addButton.setOnClickListener {
            showConnectionDialog()
        }
    }

    private fun listFiles(host: String, user: String, password: String, shared: String, displayName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val config: Configuration = PropertyConfiguration(System.getProperties())
                val context: CIFSContext = BaseContext(config)
                val auth = NtlmPasswordAuthenticator(
                    "",
                    user,
                    password
                )
                val authContext = context.withCredentials(auth)
                val smbUrl = "smb://$host/$shared"

                val dir = SmbFile(smbUrl, authContext)
                Log.d("Display Name", displayName)
                if (dir.exists())
                    viewModel.saveNetwork(
                        NetworkConnection(
                            host = host,
                            username = user,
                            password = password,
                            sharedPath = shared,
                            connectionType = ConnectionTypes.SMB.type,
                            displayName = displayName
                        )
                    )
                val files = dir.listFiles()

                for (file in files) {
                    Log.d("Loading Files", file.name)
                }

            } catch (e: Exception) {
                Log.e("File Load Failed", e.toString())
                toast(e.message.toString(), Toast.LENGTH_LONG)
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
            viewModel.verifyNetwork(item as NetworkConnection)
            lifecycleScope.launch {
                viewModel.verifyNetwork.collectLatest { value ->
                    if (value) {
                        val path = "${item.host.trimEnd('/')}/${item.sharedPath.trimStart('/')}"
                        startServer(item)
                        startActivity(Intent(this@CloudActivity, MainActivity::class.java).apply {
                            putExtra(PATH, path)
                            putExtra(NETWORK_PATH, true)
                        })
                    } else {
                        Toast.makeText(this@CloudActivity, "Connection failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }.apply {
            binding.connectionsList.adapter = this
        }
    }

    private fun startServer(connection:NetworkConnection){
        val https = HttpServer(7871,connection.host)
        https.start()
    }
}
