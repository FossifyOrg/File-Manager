package org.fossify.filemanager.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.thegrizzlylabs.sardineandroid.DavResource
import jcifs.smb.SmbFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.schmizz.sshj.sftp.RemoteResourceInfo
import org.apache.commons.net.ftp.FTPFile
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.dialogs.StoragePickerDialog
import org.fossify.commons.enums.ConnectionTypes
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.*
import org.fossify.commons.models.FileDirItem
import org.fossify.commons.views.Breadcrumbs
import org.fossify.commons.views.MyGridLayoutManager
import org.fossify.commons.views.MyRecyclerView
import org.fossify.filemanager.App
import org.fossify.filemanager.R
import org.fossify.filemanager.activities.MainActivity
import org.fossify.filemanager.activities.SimpleActivity
import org.fossify.filemanager.adapters.ItemsAdapter
import org.fossify.filemanager.databinding.ItemsFragmentBinding
import org.fossify.filemanager.dialogs.CreateNewItemDialog
import org.fossify.filemanager.extensions.config
import org.fossify.filemanager.extensions.isPathOnRoot
import org.fossify.filemanager.extensions.networkSharePaths
import org.fossify.filemanager.extensions.setAsNetworkPath
import org.fossify.filemanager.fileSystems.FileHelpers
import org.fossify.filemanager.helpers.MAX_COLUMN_COUNT
import org.fossify.filemanager.helpers.RootHelpers
import org.fossify.filemanager.interfaces.ItemOperationsListener
import org.fossify.filemanager.mapper.toFileItem
import org.fossify.filemanager.models.ApiResponse
import org.fossify.filemanager.models.ListItem
import org.fossify.filemanager.viewmodels.NetworkBrowserViewModel
import java.io.File

class ItemsFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment<MyViewPagerFragment.ItemsInnerBinding>(context, attributeSet),
    ItemOperationsListener,
    Breadcrumbs.BreadcrumbsListener {
    private lateinit var viewModel: NetworkBrowserViewModel
    private var showHidden = false
    private var lastSearchedText = ""
    private var scrollStates = HashMap<String, Parcelable>()
    private var zoomListener: MyRecyclerView.MyZoomListener? = null

    private var storedItems = ArrayList<ListItem>()
    private var itemsIgnoringSearch = ArrayList<ListItem>()
    private lateinit var binding: ItemsFragmentBinding
    private var connectionType: ConnectionTypes = ConnectionTypes.Default


    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = ItemsFragmentBinding.bind(this)
        innerBinding = ItemsInnerBinding(binding)
    }

    override fun setupFragment(activity: SimpleActivity) {
        if (this.activity == null) {
            this.activity = activity
            binding.apply {
                breadcrumbs.listener = this@ItemsFragment
                itemsSwipeRefresh.setOnRefreshListener { refreshFragment() }
                itemsFab.setOnClickListener {
                    if (isCreateDocumentIntent) {
                        (activity as MainActivity).createDocumentConfirmed(currentPath)
                    } else {
                        createNewItem()
                    }
                }
            }
            setUpViewModel()
        }
    }

    private fun setUpViewModel() {
        val composition = (activity?.application as App).appComposition
        val factory = composition.provideNetworkBrowserViewModelFactory()
        activity?.let {
            viewModel = ViewModelProvider(it, factory)
                .get(NetworkBrowserViewModel::class.java)
        }
    }

    override fun onResume(textColor: Int) {
        context!!.updateTextColors(this)
        getRecyclerAdapter()?.apply {
            updatePrimaryColor()
            updateTextColor(textColor)
            initDrawables()
        }

        binding.apply {
            val properPrimaryColor = context!!.getProperPrimaryColor()
            itemsFastscroller.updateColors(properPrimaryColor)
            progressBar.setIndicatorColor(properPrimaryColor)
            progressBar.trackColor = properPrimaryColor.adjustAlpha(LOWER_ALPHA)

            if (currentPath != "") {
                breadcrumbs.updateColor(textColor, connectionType)
            }

            itemsSwipeRefresh.isEnabled = lastSearchedText.isEmpty() && activity?.config?.enablePullToRefresh != false
        }
    }

    override fun setupFontSize() {
        getRecyclerAdapter()?.updateFontSizes()
        if (currentPath != "") {
            binding.breadcrumbs.updateFontSize(context!!.getTextSize(), false)
        }
    }

    override fun setupDateTimeFormat() {
        getRecyclerAdapter()?.updateDateTimeFormat()
    }

    override fun finishActMode() {
        getRecyclerAdapter()?.finishActMode()
    }


    fun openPath(path: String, forceRefresh: Boolean = false, pathName: String = "", connectionType: ConnectionTypes = ConnectionTypes.Default) {
        if ((activity as? BaseSimpleActivity)?.isAskingPermissions == true) {
            return
        }

        var realPath = path.trimEnd('/')
        if (realPath.isEmpty()) {
            realPath = "/"
        }
        this.connectionType = connectionType
        scrollStates[currentPath] = getScrollState()!!
        currentPath = realPath
        showHidden = context!!.config.shouldShowHidden()
        showProgressBar()
        getItems(currentPath, connectionType = connectionType) { originalPath, listItems ->
            if (currentPath != originalPath) {
                return@getItems
            }

            FileDirItem.sorting = context!!.config.getFolderSorting(currentPath)
            if (connectionType != ConnectionTypes.Default) {
                listItems.forEach {
                    it.parent = originalPath.substringAfter('/')
                }
            }
            listItems.sort()

            if (context!!.config.getFolderViewType(currentPath) == VIEW_TYPE_GRID && listItems.none { it.isSectionTitle }) {
                if (listItems.any { it.mIsDirectory } && listItems.any { !it.mIsDirectory }) {
                    val firstFileIndex = listItems.indexOfFirst { !it.mIsDirectory }
                    if (firstFileIndex != -1) {
                        val sectionTitle = ListItem("", "", false, 0, 0, 0, false, true, mConnectionType = connectionType)
                        listItems.add(firstFileIndex, sectionTitle)
                    }
                }
            }

            itemsIgnoringSearch = listItems
            activity?.runOnUiThread {
                (activity as? MainActivity)?.refreshMenuItems()
                addItems(listItems, forceRefresh,pathName = pathName, connectionType)
                if (context != null && currentViewType != context!!.config.getFolderViewType(currentPath)) {
                    setupLayoutManager(connectionType)
                }
                hideProgressBar()
            }
        }
    }

    private fun addItems(items: ArrayList<ListItem>, forceRefresh: Boolean = false,pathName: String = "", connectionType: ConnectionTypes) {
        activity?.runOnUiThread {
            binding.itemsSwipeRefresh.isRefreshing = false
            if (connectionType != ConnectionTypes.DAVx5){
                binding.breadcrumbs.setBreadcrumb(currentPath, connectionType)
            }
            if (!forceRefresh && items.hashCode() == storedItems.hashCode()) {
                return@runOnUiThread
            }

            storedItems = items
            if (binding.itemsList.adapter == null) {
                binding.breadcrumbs.updateFontSize(context!!.getTextSize(), true, connectionType)
            }
            var lastClickedItem: ListItem? = null
            ItemsAdapter(activity as SimpleActivity, storedItems, this, binding.itemsList, isPickMultipleIntent, binding.itemsSwipeRefresh) {
                lastClickedItem = it as? ListItem
                if ((it as? ListItem)?.mIsDirectory == false) {
                    if (connectionType == ConnectionTypes.SMB) {
                        it?.let { item ->
                            FileHelpers.launchSMB(item.mPath, this@ItemsFragment.context)
                        }
                    } else if (connectionType == ConnectionTypes.WebDav) {
                        it?.let { item ->
                            FileHelpers.launchWebDav(item.mPath, context = this@ItemsFragment.context)
                        }
                    } else if (connectionType == ConnectionTypes.SFTP) {
                        it?.let { item ->
                            FileHelpers.launchSFTP(item.mPath, context = this@ItemsFragment.context)
                        }
                    } else if (connectionType == ConnectionTypes.FTP) {
                        it?.let { item ->
                            FileHelpers.launchFTP( item.mPath, context = this@ItemsFragment.context)
                        }
                    } else {
                        itemClicked(it as FileDirItem, connectionType)
                    }
                } else if ((it as? ListItem)?.isSectionTitle == true) {
                    openDirectory(it.mPath)
                    searchClosed()
                } else {
                    itemClicked(it as FileDirItem, connectionType)
                }
                if (connectionType == ConnectionTypes.DAVx5){
                    if (lastClickedItem != null){
                        binding.breadcrumbs.setBreadcrumbWithName(lastClickedItem!!.mPath,lastClickedItem?.mName,connectionType)
                    }
                }
            }.apply {
                setupZoomListener(zoomListener)
                binding.itemsList.adapter = this
            }

            if (lastClickedItem == null && connectionType == ConnectionTypes.DAVx5){
                binding.breadcrumbs.setBreadcrumbWithName(currentPath,pathName, connectionType)
            }
            if (context.areSystemAnimationsEnabled) {
                binding.itemsList.scheduleLayoutAnimation()
            }

            getRecyclerLayoutManager().onRestoreInstanceState(scrollStates[currentPath])
        }
    }

    private fun getScrollState() = getRecyclerLayoutManager().onSaveInstanceState()

    private fun getRecyclerLayoutManager() = (binding.itemsList.layoutManager as MyGridLayoutManager)

    @SuppressLint("NewApi")
    private fun getItems(
        path: String,
        connectionType: ConnectionTypes,
        callback: (originalPath: String, items: ArrayList<ListItem>) -> Unit
    ) {
        ensureBackgroundThread {
            if (activity?.isDestroyed == false && activity?.isFinishing == false) {
                val config = context!!.config
                if (connectionType.equals(ConnectionTypes.SMB)) {
                    val fileItems = viewModel.getFilesFromNetworkPath(path)
                    handleApiResponse(fileItems, path, connectionType, callback)
                } else if (connectionType.equals(ConnectionTypes.WebDav)) {
                    CoroutineScope(Dispatchers.IO).launch {
                        viewModel.listWebDavFiles(path)
                        viewModel.webDavFiles.collectLatest {
                            handleApiResponse(it, path, connectionType, callback)
                        }
                    }

                } else if (connectionType.equals(ConnectionTypes.SFTP)) {
                    CoroutineScope(Dispatchers.IO).launch {
                        viewModel.listAllFilesSFTPRoot(path)
                        viewModel.sftpFiles.collectLatest {
                            handleApiResponse(it, path, connectionType, callback)
                        }
                    }
                } else if (connectionType.equals(ConnectionTypes.FTP)) {
                    CoroutineScope(Dispatchers.IO).launch {
                        viewModel.listAllFTPFiles(path)
                        viewModel.ftpFiles.collectLatest {
                            handleApiResponse(it, path, connectionType, callback)
                        }
                    }
                }
                else if (connectionType.equals(ConnectionTypes.DAVx5)){
                    handleApiResponse<Object>(null, path, connectionType, callback)
                }
                else if (context.isRestrictedSAFOnlyRoot(path)) {
                    activity?.runOnUiThread { hideProgressBar() }
                    activity?.handleAndroidSAFDialog(path, openInSystemAppAllowed = true) {
                        if (!it) {
                            activity?.toast(R.string.no_storage_permissions)
                            return@handleAndroidSAFDialog
                        }
                        val getProperChildCount = context!!.config.getFolderViewType(currentPath) == VIEW_TYPE_LIST
                        context.getAndroidSAFFileItems(path, context.config.shouldShowHidden(), getProperChildCount) { fileItems ->
                            callback(path, getListItemsFromFileDirItems(fileItems))
                        }
                    }
                } else if (context!!.isPathOnOTG(path) && config.OTGTreeUri.isNotEmpty()) {
                    val getProperFileSize = context!!.config.getFolderSorting(currentPath) and SORT_BY_SIZE != 0
                    context!!.getOTGItems(path, config.shouldShowHidden(), getProperFileSize) {
                        callback(path, getListItemsFromFileDirItems(it))
                    }
                } else if (!config.enableRootAccess || !context!!.isPathOnRoot(path) && (connectionType.equals(ConnectionTypes.DAVx5) || connectionType.equals(
                        ConnectionTypes.Default
                    ))
                ) {
                    getRegularItemsOf(path, callback, connectionType)
                } else {
                    RootHelpers(activity!!).getFiles(path, callback)
                }
            }
        }
    }

    private fun getRegularItemsOf(path: String, callback: (originalPath: String, items: ArrayList<ListItem>) -> Unit, connectionType: ConnectionTypes) {
        val items = ArrayList<ListItem>()
        val getProperChildCount = context!!.config.getFolderViewType(currentPath) == VIEW_TYPE_LIST

        if (connectionType == ConnectionTypes.DAVx5) {
            val uri = Uri.parse(path)
            val docFile = DocumentFile.fromTreeUri(context, uri)
            val files = docFile?.listFiles()

            files?.forEach { file ->
                items.add(
                    ListItem(
                        mPath = file.uri.toString(),
                        mName = file.name ?: "",
                        mIsDirectory = file.isDirectory,
                        mChildren = if (file.isDirectory) 1 else 0,
                        mSize = if (file.isFile) file.length() else 0L,
                        mModified = file.lastModified(),
                        isSectionTitle = false,
                        isGridTypeDivider = false,
                        parent = path
                    )
                )
            }
        } else {
            val files = File(path).listFiles()?.filterNotNull()
            if (context == null || files == null) {
                callback(path, items)
                return
            }

            val isSortingBySize = context!!.config.getFolderSorting(currentPath) and SORT_BY_SIZE != 0
            val lastModifieds = context!!.getFolderLastModifieds(path)

            for (file in files) {
                val listItem = getListItemFromFile(file, isSortingBySize, lastModifieds, false)
                if (listItem != null) {
                    if (wantedMimeTypes.any { isProperMimeType(it, file.absolutePath, file.isDirectory) }) {
                        items.add(listItem)
                    }
                }
            }
        }

        // send out the initial item list asap, get proper child count asynchronously as it can be slow
        callback(path, items)

        if (getProperChildCount) {
            items.filter { it.mIsDirectory }.forEach {
                if (context != null) {
                    val childrenCount = it.getDirectChildrenCount(activity as BaseSimpleActivity, showHidden)
                    if (childrenCount != 0) {
                        activity?.runOnUiThread {
                            getRecyclerAdapter()?.updateChildCount(it.mPath, childrenCount)
                        }
                    }
                }
            }
        }
    }

    private fun getListItemFromFile(file: File, isSortingBySize: Boolean, lastModifieds: HashMap<String, Long>, getProperChildCount: Boolean): ListItem? {
        val curPath = file.absolutePath
        val curName = file.name
        if (!showHidden && curName.startsWith(".")) {
            return null
        }

        var lastModified = lastModifieds.remove(curPath)
        val isDirectory = file.isDirectory
        val children = if (isDirectory && getProperChildCount) file.getDirectChildrenCount(context, showHidden) else 0
        val size = if (isDirectory) {
            if (isSortingBySize) {
                file.getProperSize(showHidden)
            } else {
                0L
            }
        } else {
            file.length()
        }

        if (lastModified == null) {
            lastModified = file.lastModified()
        }

        return ListItem(curPath, curName, isDirectory, children, size, lastModified, false, false)
    }

    private fun getListItemsFromFileDirItems(fileDirItems: ArrayList<FileDirItem>): ArrayList<ListItem> {
        val listItems = ArrayList<ListItem>()
        fileDirItems.forEach {
            val listItem = ListItem(it.path, it.name, it.isDirectory, it.children, it.size, it.modified, false, false, mConnectionType = it.connectionType)
            if (wantedMimeTypes.any { mimeType -> isProperMimeType(mimeType, it.path, it.isDirectory) }) {
                listItems.add(listItem)
            }
        }
        return listItems
    }

    private fun itemClicked(item: FileDirItem, connectionType: ConnectionTypes) {
        if (item.isDirectory) {
            openDirectory(item.path, connectionType)
        } else {
            clickedPath(item.path)
        }
    }

    private fun openDirectory(path: String, connectionType: ConnectionTypes = ConnectionTypes.Default) {
        (activity as? MainActivity)?.apply {
            openedDirectory()
        }
        openPath(path, connectionType = connectionType)
    }

    override fun searchQueryChanged(text: String) {
        lastSearchedText = text
        if (context == null) {
            return
        }

        binding.apply {
            itemsSwipeRefresh.isEnabled = text.isEmpty() && activity?.config?.enablePullToRefresh != false
            when {
                text.isEmpty() -> {
                    itemsFastscroller.beVisible()
                    getRecyclerAdapter()?.updateItems(itemsIgnoringSearch)
                    itemsPlaceholder.beGone()
                    itemsPlaceholder2.beGone()
                    hideProgressBar()
                }

                text.length == 1 -> {
                    itemsFastscroller.beGone()
                    itemsPlaceholder.beVisible()
                    itemsPlaceholder2.beVisible()
                    hideProgressBar()
                }

                else -> {
                    showProgressBar()
                    ensureBackgroundThread {
                        val files = searchFiles(text, currentPath)
                        files.sortBy { it.getParentPath() }

                        if (lastSearchedText != text) {
                            return@ensureBackgroundThread
                        }

                        val listItems = ArrayList<ListItem>()

                        var previousParent = ""
                        files.forEach {
                            val parent = it.mPath.getParentPath()
                            if (!it.isDirectory && parent != previousParent && context != null) {
                                val sectionTitle = ListItem(parent, context!!.humanizePath(parent), false, 0, 0, 0, true, false)
                                listItems.add(sectionTitle)
                                previousParent = parent
                            }

                            if (it.isDirectory) {
                                val sectionTitle = ListItem(it.path, context!!.humanizePath(it.path), true, 0, 0, 0, true, false)
                                listItems.add(sectionTitle)
                                previousParent = parent
                            }

                            if (!it.isDirectory) {
                                listItems.add(it)
                            }
                        }

                        activity?.runOnUiThread {
                            getRecyclerAdapter()?.updateItems(listItems, text)
                            itemsFastscroller.beVisibleIf(listItems.isNotEmpty())
                            itemsPlaceholder.beVisibleIf(listItems.isEmpty())
                            itemsPlaceholder2.beGone()
                            hideProgressBar()
                        }
                    }
                }
            }
        }
    }

    private fun searchFiles(text: String, path: String): ArrayList<ListItem> {
        val files = ArrayList<ListItem>()
        if (context == null) {
            return files
        }

        val normalizedText = text.normalizeString()
        val sorting = context!!.config.getFolderSorting(path)
        FileDirItem.sorting = context!!.config.getFolderSorting(currentPath)
        val isSortingBySize = sorting and SORT_BY_SIZE != 0
        File(path).listFiles()?.sortedBy { it.isDirectory }?.forEach {
            if (!showHidden && it.isHidden) {
                return@forEach
            }

            if (it.isDirectory) {
                if (it.name.normalizeString().contains(normalizedText, true)) {
                    val fileDirItem = getListItemFromFile(it, isSortingBySize, HashMap(), false)
                    if (fileDirItem != null) {
                        files.add(fileDirItem)
                    }
                }

                files.addAll(searchFiles(text, it.absolutePath))
            } else {
                if (it.name.normalizeString().contains(normalizedText, true)) {
                    val fileDirItem = getListItemFromFile(it, isSortingBySize, HashMap(), false)
                    if (fileDirItem != null) {
                        files.add(fileDirItem)
                    }
                }
            }
        }
        return files
    }

    private fun searchClosed() {
        binding.apply {
            lastSearchedText = ""
            itemsSwipeRefresh.isEnabled = activity?.config?.enablePullToRefresh != false
            itemsFastscroller.beVisible()
            itemsPlaceholder.beGone()
            itemsPlaceholder2.beGone()
            hideProgressBar()
        }
    }

    private fun createNewItem() {
        CreateNewItemDialog(activity as SimpleActivity, currentPath, connectionType, viewModel) {
            if (it) {
                refreshFragment()
            } else {
                activity?.toast(R.string.unknown_error_occurred)
            }
        }
    }

    private fun getRecyclerAdapter() = binding.itemsList.adapter as? ItemsAdapter

    private fun setupLayoutManager(connectionType: ConnectionTypes) {
        if (context!!.config.getFolderViewType(currentPath) == VIEW_TYPE_GRID) {
            currentViewType = VIEW_TYPE_GRID
            setupGridLayoutManager()
        } else {
            currentViewType = VIEW_TYPE_LIST
            setupListLayoutManager()
        }

        binding.itemsList.adapter = null
        initZoomListener()
        addItems(storedItems, true, connectionType = connectionType)
    }

    private fun setupGridLayoutManager() {
        val layoutManager = binding.itemsList.layoutManager as MyGridLayoutManager
        layoutManager.spanCount = context?.config?.fileColumnCnt ?: 3

        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (getRecyclerAdapter()?.isASectionTitle(position) == true || getRecyclerAdapter()?.isGridTypeDivider(position) == true) {
                    layoutManager.spanCount
                } else {
                    1
                }
            }
        }
    }

    private fun setupListLayoutManager() {
        val layoutManager = binding.itemsList.layoutManager as MyGridLayoutManager
        layoutManager.spanCount = 1
        zoomListener = null
    }

    private fun initZoomListener() {
        if (context?.config?.getFolderViewType(currentPath) == VIEW_TYPE_GRID) {
            val layoutManager = binding.itemsList.layoutManager as MyGridLayoutManager
            zoomListener = object : MyRecyclerView.MyZoomListener {
                override fun zoomIn() {
                    if (layoutManager.spanCount > 1) {
                        reduceColumnCount()
                        getRecyclerAdapter()?.finishActMode()
                    }
                }

                override fun zoomOut() {
                    if (layoutManager.spanCount < MAX_COLUMN_COUNT) {
                        increaseColumnCount()
                        getRecyclerAdapter()?.finishActMode()
                    }
                }
            }
        } else {
            zoomListener = null
        }
    }

    private fun increaseColumnCount() {
        if (currentViewType == VIEW_TYPE_GRID) {
            context!!.config.fileColumnCnt += 1
            (activity as? MainActivity)?.updateFragmentColumnCounts()
        }
    }

    private fun reduceColumnCount() {
        if (currentViewType == VIEW_TYPE_GRID) {
            context!!.config.fileColumnCnt -= 1
            (activity as? MainActivity)?.updateFragmentColumnCounts()
        }
    }

    private fun <T> handleApiResponse(
        apiResponse: ApiResponse<T>?,
        path: String,
        connectionType: ConnectionTypes,
        callback: (originalPath: String, items: ArrayList<ListItem>) -> Unit
    ) {
        if (apiResponse?.exception != null) {
            apiResponse.exception.message?.let { exp ->
                activity?.toast(exp)
            }
        } else {
            if (connectionType == ConnectionTypes.SMB) {
                apiResponse?.response?.let { item ->
                    val fileItems = item as Array<SmbFile>
                    val items = fileItems.map { it -> it.toFileItem(connectionType) }
                    callback(path, getListItemsFromFileDirItems(ArrayList(items?.toList())))
                }
            } else if (connectionType == ConnectionTypes.WebDav) {
                apiResponse?.response?.let { item ->
                    val fileItems = item as List<DavResource>
                    val items = fileItems.map { it -> it.toFileItem(connectionType) }
                    callback(path, getListItemsFromFileDirItems(ArrayList(items?.toList())))
                }
            } else if (connectionType == ConnectionTypes.SFTP) {
                apiResponse?.response?.let { item ->
                    val fileItems = item as List<RemoteResourceInfo>
                    val items = fileItems?.map { it -> it.toFileItem(path, connectionType) }
                    callback(path, getListItemsFromFileDirItems(ArrayList(items?.toList())))
                }

            } else if (connectionType == ConnectionTypes.FTP) {
                apiResponse?.response?.let { item ->
                    val fileItems = item as List<FTPFile>
                    val items = fileItems?.map { it -> it.toFileItem(path, connectionType) }
                    callback(path, getListItemsFromFileDirItems(ArrayList(items?.toList())))
                }
            }
            else if (connectionType == ConnectionTypes.DAVx5) {
                val items = ArrayList<ListItem>()
                val uri = Uri.parse(path)
                val docFile = DocumentFile.fromTreeUri(context, uri)
                val files = docFile?.listFiles()

                files?.forEach { file ->
                    items.add(
                        ListItem(
                            mConnectionType = connectionType,
                            mPath = file.uri.toString(),
                            mName = file.name ?: "",
                            mIsDirectory = file.isDirectory,
                            mChildren = if (file.isDirectory) 1 else 0,
                            mSize = if (file.isFile) file.length() else 0L,
                            mModified = file.lastModified(),
                            isSectionTitle = false,
                            isGridTypeDivider = false,
                            parent = path
                        )
                    )
                }
                callback(path,items)
            }
        }
    }

        override fun columnCountChanged() {
            (binding.itemsList.layoutManager as MyGridLayoutManager).spanCount = context!!.config.fileColumnCnt
            (activity as? MainActivity)?.refreshMenuItems()
            getRecyclerAdapter()?.apply {
                notifyItemRangeChanged(0, listItems.size)
            }
        }

        fun showProgressBar() {
            binding.progressBar.show()
        }

        private fun hideProgressBar() {
            binding.progressBar.hide()
        }

        fun getBreadcrumbs() = binding.breadcrumbs

        override fun toggleFilenameVisibility() {
            getRecyclerAdapter()?.updateDisplayFilenamesInGrid()
        }

        override fun breadcrumbClicked(id: Int) {
            val item = binding.breadcrumbs.getItem(id)
            if (id == 0) {
                StoragePickerDialog(activity as SimpleActivity, currentPath, context!!.config.enableRootAccess, true) {
                    getRecyclerAdapter()?.finishActMode()
                    if (item.connectionType != ConnectionTypes.Default) {
                        openPath(item.path, connectionType = item.connectionType)
                    } else {
                        openPath(item.path)
                    }
                }
            } else {
                var path = ""
                if (item.connectionType == ConnectionTypes.WebDav || item.connectionType == ConnectionTypes.SMB) {
                    val items = binding.breadcrumbs.getItemsTillIndex(id)
                    items.forEach { item ->
                        path += "${item.path}/"
                    }
                }
                else
                    path = item.path

                openPath(path, connectionType = item.connectionType)
            }
        }

        override fun refreshFragment() {
            openPath(currentPath, connectionType = connectionType)
        }

        override fun deleteFiles(files: ArrayList<FileDirItem>) {
            if (connectionType != ConnectionTypes.Default) {
                collectLatest()
            }
            val hasFolder = files.any { it.isDirectory }
            deleteFileOrFolder(files, hasFolder)
        }


        private fun deleteFileOrFolder(files: ArrayList<FileDirItem>, hasFolder: Boolean) {
            when (connectionType) {
                ConnectionTypes.SMB -> {
                    files.forEach {
                        viewModel.deleteItemSMB(it.path)
                    }
                }

                ConnectionTypes.WebDav -> {
                    files.forEach {
                        viewModel.deleteItemWebDav(it.path)
                    }
                }

                ConnectionTypes.SFTP -> {
                    files.forEach {
                        viewModel.deleteItemSFTP(it.path, it.isDirectory)
                    }
                }

                ConnectionTypes.FTP -> {
                    files.forEach {
                        viewModel.deleteItemFTP(it.path, it.isDirectory)
                    }
                }

                ConnectionTypes.Default -> {
                    handleFileDeleting(files, hasFolder)
                }

                else -> Unit
            }
        }

        private fun collectLatest() {
            CoroutineScope(Dispatchers.IO).launch {
                when (connectionType) {
                    ConnectionTypes.SMB -> {
                        viewModel.smbDelete.collectLatest {
                            it.exception?.message?.let { msg ->
                                activity?.toast(msg)
                            }
                        }
                    }

                    ConnectionTypes.WebDav -> {
                        viewModel.webDavDelete.collectLatest {
                            it.exception?.message?.let { msg ->
                                activity?.toast(msg)
                            }
                        }
                    }

                    ConnectionTypes.SFTP -> {
                        viewModel.sftpDelete.collectLatest {
                            it.exception?.message?.let { msg ->
                                activity?.toast(msg)
                            }
                        }
                    }

                    ConnectionTypes.FTP -> {
                        viewModel.ftpDelete.collectLatest {
                            it.exception?.message?.let { msg ->
                                activity?.toast(msg)
                            }
                        }
                    }

                    else -> Unit
                }
            }
        }

        override fun selectedPaths(paths: ArrayList<String>) {
            (activity as MainActivity).pickedPaths(paths)
        }

        override fun shareFile(paths: ArrayList<String>) {
            collectFileShared(paths)
            if (connectionType == ConnectionTypes.SMB) {
                paths.forEach {
                    viewModel.writeSmbFileToCache(it, context)
                }
            } else if (connectionType == ConnectionTypes.WebDav) {
                paths.forEach {
                    viewModel.writeWebDavFileToCache(it, context)
                }
            } else if (connectionType == ConnectionTypes.SFTP) {
                paths.forEach {
                    viewModel.writeSftpFileToCache(it, context)
                }
            } else if (connectionType == ConnectionTypes.FTP) {
                paths.forEach {
                    viewModel.writeFtpFileToCache(it, context)
                }
            }
        }

        override fun openWith(path: String, mimType: String?) {
            when (connectionType) {
                ConnectionTypes.SMB -> {
                    FileHelpers.launchSMB(path, context, mimType)
                }

                ConnectionTypes.WebDav -> {
                    FileHelpers.launchWebDav(path, context, mimType)
                }

                ConnectionTypes.SFTP -> {
                    FileHelpers.launchSFTP( path, context, mimType)
                }

                ConnectionTypes.FTP -> {
                    FileHelpers.launchFTP( path, context, mimType)
                }

                else -> Unit
            }
        }


        private fun collectFileShared(paths: ArrayList<String>) {
            CoroutineScope(Dispatchers.IO).launch {
                when (connectionType) {
                    ConnectionTypes.SMB -> {
                        viewModel.smbFileShare.collectLatest {
                            handleFileSharedResponse(it, paths)
                        }
                    }

                    ConnectionTypes.WebDav -> {
                        viewModel.webDavFileShare.collectLatest {
                            handleFileSharedResponse(it, paths)
                        }
                    }

                    ConnectionTypes.SFTP -> {
                        viewModel.sftpFileShare.collectLatest {
                            handleFileSharedResponse(it, paths)
                        }
                    }

                    ConnectionTypes.FTP -> {
                        viewModel.ftpFileShare.collectLatest {
                            handleFileSharedResponse(it, paths)
                        }
                    }

                    else -> Unit
                }
            }
        }

        private fun handleFileSharedResponse(response: ApiResponse<File>, paths: ArrayList<String>) {
            if (response.exception != null) {
                response.exception?.message?.let { msg ->
                    activity?.toast(msg)
                }
            } else {
                activity?.networkSharePaths(paths, response.response as File)
            }
        }

        private fun handleFileWriteResponse(response: ApiResponse<File>, paths: String) {
            if (response.exception != null) {
                response.exception?.message?.let { msg ->
                    activity?.toast(msg)
                }
            } else {
                activity?.setAsNetworkPath(paths, response.response as File)
            }
        }


        override fun setAs(path: String) {
            collectFileCopied(path)
            if (connectionType == ConnectionTypes.SMB) {
                viewModel.writeSmbFileToCache(path, context)

            } else if (connectionType == ConnectionTypes.WebDav) {
                viewModel.writeWebDavFileToCache(path, context)

            } else if (connectionType == ConnectionTypes.SFTP) {
                viewModel.writeSftpFileToCache(path, context)

            } else if (connectionType == ConnectionTypes.FTP) {
                viewModel.writeFtpFileToCache(path, context)
            }
        }

        private fun collectFileCopied(path: String) {
            CoroutineScope(Dispatchers.IO).launch {
                when (connectionType) {
                    ConnectionTypes.SMB -> {
                        viewModel.smbFileShare.collectLatest {
                            handleFileWriteResponse(it, path)
                        }
                    }

                    ConnectionTypes.WebDav -> {
                        viewModel.webDavFileShare.collectLatest {
                            handleFileWriteResponse(it, path)
                        }
                    }

                    ConnectionTypes.SFTP -> {
                        viewModel.sftpFileShare.collectLatest {
                            handleFileWriteResponse(it, path)
                        }
                    }

                    ConnectionTypes.FTP -> {
                        viewModel.ftpFileShare.collectLatest {
                            handleFileWriteResponse(it, path)
                        }
                    }

                    else -> Unit
                }
            }
        }

    }
