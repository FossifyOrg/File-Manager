package org.fossify.filemanager.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.usage.StorageStatsManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.provider.Settings
import android.util.AttributeSet
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.view.isVisible
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.*
import org.fossify.commons.models.FileDirItem
import org.fossify.commons.views.MyGridLayoutManager
import org.fossify.filemanager.R
import org.fossify.filemanager.activities.MimeTypesActivity
import org.fossify.filemanager.activities.SimpleActivity
import org.fossify.filemanager.adapters.ItemsAdapter
import org.fossify.filemanager.databinding.ItemStorageVolumeBinding
import org.fossify.filemanager.databinding.StorageFragmentBinding
import org.fossify.filemanager.extensions.config
import org.fossify.filemanager.extensions.formatSizeThousand
import org.fossify.filemanager.extensions.getAllVolumeNames
import org.fossify.filemanager.helpers.*
import org.fossify.filemanager.interfaces.ItemOperationsListener
import org.fossify.filemanager.models.ListItem
import java.util.Locale

class StorageFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment<MyViewPagerFragment.StorageInnerBinding>(context, attributeSet),
    ItemOperationsListener {
    private val SIZE_DIVIDER = 100000
    private var allDeviceListItems = ArrayList<ListItem>()
    private var lastSearchedText = ""
    private lateinit var binding: StorageFragmentBinding
    private val volumes = mutableMapOf<String, ItemStorageVolumeBinding>()

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = StorageFragmentBinding.bind(this)
        innerBinding = StorageInnerBinding(binding)
    }

    override fun setupFragment(activity: SimpleActivity) {
        if (this.activity == null) {
            this.activity = activity
        }

        val volumeNames = activity.getAllVolumeNames()
        volumeNames.forEach { volumeName ->
            val volumeBinding = ItemStorageVolumeBinding.inflate(activity.layoutInflater)
            volumes[volumeName] = volumeBinding
            volumeBinding.apply {
                if (volumeName == PRIMARY_VOLUME_NAME) {
                    storageName.setText(R.string.internal)
                } else {
                    storageName.setText(R.string.sd_card)
                }

                totalSpace.text = String.format(context.getString(R.string.total_storage), "…")
                getSizes(volumeName)

                if (volumeNames.size > 1) {
                    root.children.forEach { it.beGone() }
                    freeSpaceHolder.beVisible()
                    expandButton.applyColorFilter(context.getProperPrimaryColor())
                    expandButton.setImageResource(R.drawable.ic_arrow_down_vector)

                    expandButton.setOnClickListener { _ ->
                        if (imagesHolder.isVisible) {
                            root.children.filterNot { it == freeSpaceHolder }.forEach { it.beGone() }
                            expandButton.setImageResource(R.drawable.ic_arrow_down_vector)
                        } else {
                            root.children.filterNot { it == freeSpaceHolder }.forEach { it.beVisible() }
                            expandButton.setImageResource(R.drawable.ic_arrow_up_vector)
                        }
                    }
                } else {
                    expandButton.beGone()
                }

                freeSpaceHolder.setOnClickListener {
                    try {
                        val storageSettingsIntent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                        activity.startActivity(storageSettingsIntent)
                    } catch (e: Exception) {
                        activity.showErrorToast(e)
                    }
                }

                imagesHolder.setOnClickListener { launchMimetypeActivity(IMAGES, volumeName) }
                videosHolder.setOnClickListener { launchMimetypeActivity(VIDEOS, volumeName) }
                audioHolder.setOnClickListener { launchMimetypeActivity(AUDIO, volumeName) }
                documentsHolder.setOnClickListener { launchMimetypeActivity(DOCUMENTS, volumeName) }
                archivesHolder.setOnClickListener { launchMimetypeActivity(ARCHIVES, volumeName) }
                othersHolder.setOnClickListener { launchMimetypeActivity(OTHERS, volumeName) }
            }
            binding.storageVolumesHolder.addView(volumeBinding.root)
        }

        ensureBackgroundThread {
            getVolumeStorageStats(context)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            refreshFragment()
        }, 2000)
    }

    override fun onResume(textColor: Int) {
        context.updateTextColors(binding.root)

        val properPrimaryColor = context.getProperPrimaryColor()
        val redColor = context.resources.getColor(R.color.md_red_700)
        val greenColor = context.resources.getColor(R.color.md_green_700)
        val lightBlueColor = context.resources.getColor(R.color.md_light_blue_700)
        val yellowColor = context.resources.getColor(R.color.md_yellow_700)
        val tealColor = context.resources.getColor(R.color.md_teal_700)
        val pinkColor = context.resources.getColor(R.color.md_pink_700)

        volumes.entries.forEach { (it, volumeBinding) ->
            getSizes(it)
            volumeBinding.apply {
                mainStorageUsageProgressbar.setIndicatorColor(properPrimaryColor)
                mainStorageUsageProgressbar.trackColor = properPrimaryColor.adjustAlpha(LOWER_ALPHA)

                imagesProgressbar.setIndicatorColor(redColor)
                imagesProgressbar.trackColor = redColor.adjustAlpha(LOWER_ALPHA)

                videosProgressbar.setIndicatorColor(greenColor)
                videosProgressbar.trackColor = greenColor.adjustAlpha(LOWER_ALPHA)

                audioProgressbar.setIndicatorColor(lightBlueColor)
                audioProgressbar.trackColor = lightBlueColor.adjustAlpha(LOWER_ALPHA)

                documentsProgressbar.setIndicatorColor(yellowColor)
                documentsProgressbar.trackColor = yellowColor.adjustAlpha(LOWER_ALPHA)

                archivesProgressbar.setIndicatorColor(tealColor)
                archivesProgressbar.trackColor = tealColor.adjustAlpha(LOWER_ALPHA)

                othersProgressbar.setIndicatorColor(pinkColor)
                othersProgressbar.trackColor = pinkColor.adjustAlpha(LOWER_ALPHA)

                expandButton.applyColorFilter(context.getProperPrimaryColor())
            }
        }

        binding.apply {
            searchHolder.setBackgroundColor(context.getProperBackgroundColor())
            progressBar.setIndicatorColor(properPrimaryColor)
            progressBar.trackColor = properPrimaryColor.adjustAlpha(LOWER_ALPHA)
        }

        ensureBackgroundThread {
            getVolumeStorageStats(context)
        }
    }

    private fun launchMimetypeActivity(mimetype: String, volumeName: String) {
        Intent(context, MimeTypesActivity::class.java).apply {
            putExtra(SHOW_MIMETYPE, mimetype)
            putExtra(VOLUME_NAME, volumeName)
            context.startActivity(this)
        }
    }

    private fun getSizes(volumeName: String) {
        if (!isOreoPlus()) {
            return
        }

        ensureBackgroundThread {
            val filesSize = getSizesByMimeType(volumeName)
            val fileSizeImages = filesSize[IMAGES]!!
            val fileSizeVideos = filesSize[VIDEOS]!!
            val fileSizeAudios = filesSize[AUDIO]!!
            val fileSizeDocuments = filesSize[DOCUMENTS]!!
            val fileSizeArchives = filesSize[ARCHIVES]!!
            val fileSizeOthers = filesSize[OTHERS]!!

            post {
                volumes[volumeName]!!.apply {
                    imagesSize.text = fileSizeImages.formatSize()
                    imagesProgressbar.progress = (fileSizeImages / SIZE_DIVIDER).toInt()

                    videosSize.text = fileSizeVideos.formatSize()
                    videosProgressbar.progress = (fileSizeVideos / SIZE_DIVIDER).toInt()

                    audioSize.text = fileSizeAudios.formatSize()
                    audioProgressbar.progress = (fileSizeAudios / SIZE_DIVIDER).toInt()

                    documentsSize.text = fileSizeDocuments.formatSize()
                    documentsProgressbar.progress = (fileSizeDocuments / SIZE_DIVIDER).toInt()

                    archivesSize.text = fileSizeArchives.formatSize()
                    archivesProgressbar.progress = (fileSizeArchives / SIZE_DIVIDER).toInt()

                    othersSize.text = fileSizeOthers.formatSize()
                    othersProgressbar.progress = (fileSizeOthers / SIZE_DIVIDER).toInt()
                }
            }
        }
    }

    private fun getSizesByMimeType(volumeName: String): HashMap<String, Long> {
        val uri = MediaStore.Files.getContentUri(volumeName)
        val projection = arrayOf(
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATA
        )

        var imagesSize = 0L
        var videosSize = 0L
        var audioSize = 0L
        var documentsSize = 0L
        var archivesSize = 0L
        var othersSize = 0L
        try {
            context.queryCursor(uri, projection) { cursor ->
                try {
                    val mimeType = cursor.getStringValue(MediaStore.Files.FileColumns.MIME_TYPE)?.lowercase(Locale.getDefault())
                    val size = cursor.getLongValue(MediaStore.Files.FileColumns.SIZE)
                    if (mimeType == null) {
                        if (size > 0 && size != 4096L) {
                            val path = cursor.getStringValue(MediaStore.Files.FileColumns.DATA)
                            if (!context.getIsPathDirectory(path)) {
                                othersSize += size
                            }
                        }
                        return@queryCursor
                    }

                    when (mimeType.substringBefore("/")) {
                        "image" -> imagesSize += size
                        "video" -> videosSize += size
                        "audio" -> audioSize += size
                        "text" -> documentsSize += size
                        else -> {
                            when {
                                extraDocumentMimeTypes.contains(mimeType) -> documentsSize += size
                                extraAudioMimeTypes.contains(mimeType) -> audioSize += size
                                archiveMimeTypes.contains(mimeType) -> archivesSize += size
                                else -> othersSize += size
                            }
                        }
                    }
                } catch (e: Exception) {
                }
            }
        } catch (e: Exception) {
        }

        val mimeTypeSizes = HashMap<String, Long>().apply {
            put(IMAGES, imagesSize)
            put(VIDEOS, videosSize)
            put(AUDIO, audioSize)
            put(DOCUMENTS, documentsSize)
            put(ARCHIVES, archivesSize)
            put(OTHERS, othersSize)
        }

        return mimeTypeSizes
    }

    @SuppressLint("NewApi")
    private fun getVolumeStorageStats(context: Context) {
        val externalDirs = context.getExternalFilesDirs(null)
        val storageManager = context.getSystemService(AppCompatActivity.STORAGE_SERVICE) as StorageManager

        externalDirs.forEach { file ->
            val volumeName: String
            val totalStorageSpace: Long
            val freeStorageSpace: Long
            val storageVolume = storageManager.getStorageVolume(file) ?: return
            if (storageVolume.isPrimary) {
                // internal storage
                volumeName = PRIMARY_VOLUME_NAME
                if (isOreoPlus()) {
                    val storageStatsManager = context.getSystemService(AppCompatActivity.STORAGE_STATS_SERVICE) as StorageStatsManager
                    val uuid = StorageManager.UUID_DEFAULT
                    totalStorageSpace = storageStatsManager.getTotalBytes(uuid)
                    freeStorageSpace = storageStatsManager.getFreeBytes(uuid)
                } else {
                    totalStorageSpace = file.totalSpace
                    freeStorageSpace = file.freeSpace
                }
            } else {
                volumeName = storageVolume.uuid!!.lowercase(Locale.US)
                totalStorageSpace = file.totalSpace
                freeStorageSpace = file.freeSpace
            }

            post {
                volumes[volumeName]?.apply {
                    arrayOf(
                        mainStorageUsageProgressbar, imagesProgressbar, videosProgressbar, audioProgressbar, documentsProgressbar,
                        archivesProgressbar, othersProgressbar
                    ).forEach {
                        it.max = (totalStorageSpace / SIZE_DIVIDER).toInt()
                    }

                    mainStorageUsageProgressbar.progress = ((totalStorageSpace - freeStorageSpace) / SIZE_DIVIDER).toInt()

                    mainStorageUsageProgressbar.beVisible()
                    freeSpaceValue.text = freeStorageSpace.formatSizeThousand()
                    totalSpace.text = String.format(context.getString(R.string.total_storage), totalStorageSpace.formatSizeThousand())
                    freeSpaceLabel.beVisible()
                }
            }
        }
    }

    override fun searchQueryChanged(text: String) {
        lastSearchedText = text
        binding.apply {
            if (text.isNotEmpty()) {
                if (searchHolder.alpha < 1f) {
                    searchHolder.fadeIn()
                }
            } else {
                searchHolder.animate().alpha(0f).setDuration(SHORT_ANIMATION_DURATION).withEndAction {
                    searchHolder.beGone()
                    (searchResultsList.adapter as? ItemsAdapter)?.updateItems(allDeviceListItems, text)
                }.start()
            }

            if (text.length == 1) {
                searchResultsList.beGone()
                searchPlaceholder.beVisible()
                searchPlaceholder2.beVisible()
                hideProgressBar()
            } else if (text.isEmpty()) {
                searchResultsList.beGone()
                hideProgressBar()
            } else {
                showProgressBar()
                ensureBackgroundThread {
                    val start = System.currentTimeMillis()
                    val filtered = allDeviceListItems.filter { it.mName.contains(text, true) }.toMutableList() as ArrayList<ListItem>
                    if (lastSearchedText != text) {
                        return@ensureBackgroundThread
                    }

                    (context as? Activity)?.runOnUiThread {
                        (searchResultsList.adapter as? ItemsAdapter)?.updateItems(filtered, text)
                        searchResultsList.beVisible()
                        searchPlaceholder.beVisibleIf(filtered.isEmpty())
                        searchPlaceholder2.beGone()
                        hideProgressBar()
                    }
                }
            }
        }
    }

    private fun setupLayoutManager() {
        if (context!!.config.getFolderViewType("") == VIEW_TYPE_GRID) {
            currentViewType = VIEW_TYPE_GRID
            setupGridLayoutManager()
        } else {
            currentViewType = VIEW_TYPE_LIST
            setupListLayoutManager()
        }

        binding.searchResultsList.adapter = null
        addItems()
    }

    private fun setupGridLayoutManager() {
        val layoutManager = binding.searchResultsList.layoutManager as MyGridLayoutManager
        layoutManager.spanCount = context?.config?.fileColumnCnt ?: 3
    }

    private fun setupListLayoutManager() {
        val layoutManager = binding.searchResultsList.layoutManager as MyGridLayoutManager
        layoutManager.spanCount = 1
    }

    private fun addItems() {
        ItemsAdapter(context as SimpleActivity, ArrayList(), this, binding.searchResultsList, false, null, false) {
            clickedPath((it as FileDirItem).path)
        }.apply {
            binding.searchResultsList.adapter = this
        }
    }

    private fun getAllFiles(volumeName: String): ArrayList<FileDirItem> {
        val fileDirItems = ArrayList<FileDirItem>()
        val showHidden = context?.config?.shouldShowHidden() ?: return fileDirItems
        val uri = MediaStore.Files.getContentUri(volumeName)
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )

        try {
            if (isOreoPlus()) {
                val queryArgs = bundleOf(
                    ContentResolver.QUERY_ARG_SORT_COLUMNS to arrayOf(MediaStore.Files.FileColumns.DATE_MODIFIED),
                    ContentResolver.QUERY_ARG_SORT_DIRECTION to ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                )
                context?.contentResolver?.query(uri, projection, queryArgs, null)
            } else {
                val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
                context?.contentResolver?.query(uri, projection, null, null, sortOrder)
            }?.use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        try {
                            val name = cursor.getStringValue(MediaStore.Files.FileColumns.DISPLAY_NAME)
                            if (!showHidden && name.startsWith(".")) {
                                continue
                            }

                            val size = cursor.getLongValue(MediaStore.Files.FileColumns.SIZE)
                            if (size == 0L) {
                                continue
                            }

                            val path = cursor.getStringValue(MediaStore.Files.FileColumns.DATA)
                            val lastModified = cursor.getLongValue(MediaStore.Files.FileColumns.DATE_MODIFIED) * 1000
                            fileDirItems.add(FileDirItem(path, name, false, 0, size, lastModified))
                        } catch (e: Exception) {
                        }
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            context?.showErrorToast(e)
        }

        return fileDirItems
    }

    private fun showProgressBar() {
        binding.progressBar.show()
    }

    private fun hideProgressBar() {
        binding.progressBar.hide()
    }

    private fun getRecyclerAdapter() = binding.searchResultsList.adapter as? ItemsAdapter

    override fun refreshFragment() {
        ensureBackgroundThread {
            val fileDirItems = volumes.keys.map { getAllFiles(it) }.flatten()
            allDeviceListItems = getListItemsFromFileDirItems(ArrayList(fileDirItems))
        }
        setupLayoutManager()
    }

    override fun deleteFiles(files: ArrayList<FileDirItem>) {
        handleFileDeleting(files, false)
    }

    override fun selectedPaths(paths: ArrayList<String>) {}

    override fun setupDateTimeFormat() {
        getRecyclerAdapter()?.updateDateTimeFormat()
    }

    override fun setupFontSize() {
        getRecyclerAdapter()?.updateFontSizes()
    }

    override fun toggleFilenameVisibility() {
        getRecyclerAdapter()?.updateDisplayFilenamesInGrid()
    }

    override fun columnCountChanged() {
        (binding.searchResultsList.layoutManager as MyGridLayoutManager).spanCount = context!!.config.fileColumnCnt
        getRecyclerAdapter()?.apply {
            notifyItemRangeChanged(0, listItems.size)
        }
    }

    override fun finishActMode() {
        getRecyclerAdapter()?.finishActMode()
    }
}
