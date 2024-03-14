package org.fossify.filemanager.adapters

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import org.fossify.commons.adapters.MyRecyclerViewAdapter
import org.fossify.commons.extensions.getColoredDrawableWithColor
import org.fossify.commons.extensions.getTextSize
import org.fossify.commons.extensions.getTimeFormat
import org.fossify.commons.helpers.getFilePlaceholderDrawables
import org.fossify.commons.views.MyRecyclerView
import org.fossify.filemanager.R
import org.fossify.filemanager.activities.SimpleActivity
import org.fossify.filemanager.databinding.ItemDecompressionListFileDirBinding
import org.fossify.filemanager.extensions.config
import org.fossify.filemanager.models.ListItem
import java.util.Locale

class DecompressItemsAdapter(activity: SimpleActivity, var listItems: MutableList<ListItem>, recyclerView: MyRecyclerView, itemClick: (Any) -> Unit) :
    MyRecyclerViewAdapter(activity, recyclerView, itemClick) {

    private lateinit var fileDrawable: Drawable
    private lateinit var folderDrawable: Drawable
    private var fileDrawables = HashMap<String, Drawable>()
    private var fontSize = 0f
    private var smallerFontSize = 0f
    private var dateFormat = ""
    private var timeFormat = ""

    init {
        initDrawables()
        fontSize = activity.getTextSize()
        smallerFontSize = fontSize * 0.8f
        dateFormat = activity.config.dateFormat
        timeFormat = activity.getTimeFormat()
    }

    override fun getActionMenuId() = 0

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {}

    override fun getSelectableItemCount() = 0

    override fun getIsItemSelectable(position: Int) = false

    override fun getItemSelectionKey(position: Int) = 0

    override fun getItemKeyPosition(key: Int) = 0

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return createViewHolder(ItemDecompressionListFileDirBinding.inflate(layoutInflater, parent, false).root)
    }

    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, position: Int) {
        val fileDirItem = listItems[position]
        holder.bindView(fileDirItem, true, false) { itemView, layoutPosition ->
            setupView(itemView, fileDirItem)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = listItems.size

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        if (!activity.isDestroyed && !activity.isFinishing) {
            ItemDecompressionListFileDirBinding.bind(holder.itemView).apply {
                if (itemIcon != null) {
                    Glide.with(activity).clear(itemIcon)
                }
            }
        }
    }

    private fun setupView(view: View, listItem: ListItem) {
        ItemDecompressionListFileDirBinding.bind(view).apply {
            val fileName = listItem.name
            itemName.text = fileName
            itemName.setTextColor(textColor)
            itemName.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)

            if (listItem.isDirectory) {
                itemIcon.setImageDrawable(folderDrawable)
            } else {
                val drawable = fileDrawables.getOrElse(fileName.substringAfterLast(".").lowercase(Locale.getDefault()), { fileDrawable })
                val options = RequestOptions()
                    .signature(listItem.getKey())
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .error(drawable)
                    .centerCrop()

                val itemToLoad = getImagePathToLoad(listItem.path)
                if (!activity.isDestroyed) {
                    Glide.with(activity)
                        .load(itemToLoad)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .apply(options)
                        .into(itemIcon)
                }
            }
        }
    }

    private fun getImagePathToLoad(path: String): Any {
        return if (path.endsWith(".apk", true)) {
            val packageInfo = activity.packageManager.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES)
            if (packageInfo != null) {
                val appInfo = packageInfo.applicationInfo
                appInfo.sourceDir = path
                appInfo.publicSourceDir = path
                appInfo.loadIcon(activity.packageManager)
            } else {
                path
            }
        } else {
            path
        }
    }

    private fun initDrawables() {
        folderDrawable = resources.getColoredDrawableWithColor(R.drawable.ic_folder_vector, properPrimaryColor)
        folderDrawable.alpha = 180
        fileDrawable = resources.getDrawable(R.drawable.ic_file_generic)
        fileDrawables = getFilePlaceholderDrawables(activity)
    }
}
