package org.fossify.filemanager.activities

import android.net.Uri
import android.os.Bundle
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.exception.ZipException.Type
import net.lingala.zip4j.io.inputstream.ZipInputStream
import net.lingala.zip4j.model.LocalFileHeader
import org.fossify.commons.dialogs.EnterPasswordDialog
import org.fossify.commons.dialogs.FilePickerDialog
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.helpers.isOreoPlus
import org.fossify.filemanager.R
import org.fossify.filemanager.adapters.DecompressItemsAdapter
import org.fossify.filemanager.databinding.ActivityDecompressBinding
import org.fossify.filemanager.extensions.config
import org.fossify.filemanager.models.ListItem
import java.io.BufferedInputStream
import java.io.File

class DecompressActivity : SimpleActivity() {
    companion object {
        private const val PASSWORD = "password"
    }

    private val binding by viewBinding(ActivityDecompressBinding::inflate)
    private val allFiles = ArrayList<ListItem>()
    private var currentPath = ""
    private var uri: Uri? = null
    private var password: String? = null
    private var passwordDialog: EnterPasswordDialog? = null
    private var filename = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupOptionsMenu()
        binding.apply {
            updateMaterialActivityViews(decompressCoordinator, decompressList, useTransparentNavigation = true, useTopSearchMenu = false)
            setupMaterialScrollListener(decompressList, decompressToolbar)
        }

        uri = intent.data
        if (uri == null) {
            toast(R.string.unknown_error_occurred)
            return
        }

        password = savedInstanceState?.getString(PASSWORD, null)

        val realPath = getRealPathFromURI(uri!!)
        filename = realPath?.getFilenameFromPath() ?: Uri.decode(uri.toString().getFilenameFromPath())
        binding.decompressToolbar.title = filename
        setupFilesList()
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.decompressToolbar, NavigationIcon.Arrow)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(PASSWORD, password)
    }

    private fun setupOptionsMenu() {
        binding.decompressToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.decompress -> decompressFiles()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    override fun onBackPressed() {
        if (currentPath.isEmpty()) {
            super.onBackPressed()
        } else {
            val newPath = if (currentPath.contains("/")) currentPath.getParentPath() else ""
            updateCurrentPath(newPath)
        }
    }

    private fun setupFilesList() {
        fillAllListItems(uri!!) {
            updateCurrentPath("")
        }
    }

    private fun updateCurrentPath(path: String) {
        currentPath = path
        try {
            val listItems = getFolderItems(currentPath)
            updateAdapter(listItems = listItems)
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }

    private fun updateAdapter(listItems: MutableList<ListItem>) {
        runOnUiThread {
            DecompressItemsAdapter(this, listItems, binding.decompressList) {
                if ((it as ListItem).isDirectory) {
                    updateCurrentPath(it.path)
                }
            }.apply {
                binding.decompressList.adapter = this
            }
        }
    }

    private fun decompressFiles() {
        val defaultFolder = getRealPathFromURI(uri!!) ?: internalStoragePath
        FilePickerDialog(
            activity = this,
            currPath = defaultFolder,
            pickFile = false,
            showHidden = config.showHidden,
            showFAB = true,
            canAddShowHiddenButton = true,
            showFavoritesButton = true
        ) { destination ->
            handleSAFDialog(destination) {
                if (it) {
                    ensureBackgroundThread {
                        decompressTo(destination)
                    }
                }
            }
        }
    }

    private fun decompressTo(destination: String) {
        try {
            val inputStream = contentResolver.openInputStream(uri!!)
            val zipInputStream = ZipInputStream(BufferedInputStream(inputStream!!))
            if (password != null) {
                zipInputStream.setPassword(password?.toCharArray())
            }
            val buffer = ByteArray(1024)

            zipInputStream.use {
                while (true) {
                    val entry = zipInputStream.nextEntry ?: break
                    val filename = filename.substringBeforeLast(".")
                    val parent = "$destination/$filename"
                    val newPath = "$parent/${entry.fileName.trimEnd('/')}"

                    if (!getDoesFilePathExist(parent)) {
                        if (!createDirectorySync(parent)) {
                            continue
                        }
                    }

                    if (entry.isDirectory) {
                        continue
                    }

                    val isVulnerableForZipPathTraversal = !File(newPath).canonicalPath.startsWith(parent)
                    if (isVulnerableForZipPathTraversal) {
                        continue
                    }

                    val fos = getFileOutputStreamSync(newPath, newPath.getMimeType())
                    var count: Int
                    while (true) {
                        count = zipInputStream.read(buffer)
                        if (count == -1) {
                            break
                        }

                        fos!!.write(buffer, 0, count)
                    }
                    fos!!.close()
                }

                toast(R.string.decompression_successful)
                finish()
            }
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }

    private fun getFolderItems(parent: String): ArrayList<ListItem> {
        return allFiles.filter {
            val fileParent = if (it.path.contains("/")) {
                it.path.getParentPath()
            } else {
                ""
            }

            fileParent == parent
        }.sortedWith(compareBy({ !it.isDirectory }, { it.mName })).toMutableList() as ArrayList<ListItem>
    }

    private fun fillAllListItems(uri: Uri, callback: () -> Unit) = ensureBackgroundThread {
        val inputStream = try {
            contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            showErrorToast(e)
            return@ensureBackgroundThread
        }

        val zipInputStream = ZipInputStream(BufferedInputStream(inputStream))
        if (password != null) {
            zipInputStream.setPassword(password?.toCharArray())
        }
        var zipEntry: LocalFileHeader?
        while (true) {
            try {
                zipEntry = zipInputStream.nextEntry
            } catch (passwordException: ZipException) {
                if (passwordException.type == Type.WRONG_PASSWORD) {
                    if (password != null) {
                        toast(getString(R.string.invalid_password))
                        passwordDialog?.clearPassword()
                    } else {
                        runOnUiThread {
                            askForPassword()
                        }
                    }
                    return@ensureBackgroundThread
                } else {
                    break
                }
            } catch (ignored: Exception) {
                break
            }

            if (zipEntry == null) {
                break
            }

            // Show progress bar only after password dialog is dismissed.
            runOnUiThread {
                if (binding.progressIndicator.isGone()) {
                    binding.progressIndicator.show()
                }
            }

            if (passwordDialog != null) {
                passwordDialog?.dismiss(notify = false)
                passwordDialog = null
            }

            val lastModified = if (isOreoPlus()) zipEntry.lastModifiedTime else 0
            val filename = zipEntry.fileName.removeSuffix("/")
            allFiles.add(
                ListItem(
                    mPath = filename,
                    mName = filename.getFilenameFromPath(),
                    mIsDirectory = zipEntry.isDirectory,
                    mChildren = 0,
                    mSize = 0L,
                    mModified = lastModified,
                    isSectionTitle = false,
                    isGridTypeDivider = false
                )
            )
        }

        runOnUiThread {
            binding.progressIndicator.hide()
        }

        callback()
    }

    private fun askForPassword() {
        passwordDialog = EnterPasswordDialog(
            this,
            callback = { newPassword ->
                password = newPassword
                setupFilesList()
            },
            cancelCallback = {
                finish()
            }
        )
    }
}
