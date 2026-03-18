package org.fossify.filemanager.activities

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import jcifs.CIFSContext
import jcifs.Configuration
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.filemanager.databinding.CloudActivityBinding
import org.fossify.filemanager.dialogs.ConnectionDialog


class CloudActivity: SimpleActivity() {
    private val binding by viewBinding(CloudActivityBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.apply {
            setupMaterialScrollListener(binding.cloudNestedScrollview, binding.cloudAppbar)
        }
        setupToolBar()
        registerAddConnectionListener()
    }



    private fun setupToolBar(){
        setupTopAppBar(binding.cloudAppbar, NavigationIcon.Arrow)
    }

    private fun showConnectionDialog(){
        ConnectionDialog(this@CloudActivity){host,user,password,shared->
            listFiles(host,user,password,shared)
        }
    }

    private fun registerAddConnectionListener(){
        binding.addButton.setOnClickListener {
            showConnectionDialog()
        }
    }

    private fun listFiles(host: String, user: String, password: String,shared: String){
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

                val files = dir.listFiles()

                for (file in files) {
                    Log.d("Loading Files", file.name)
                }

            } catch (e: Exception) {
                Log.e("File Load Failed",e.toString())
                toast(e.message.toString(), Toast.LENGTH_LONG)
            }
        }
    }

}
