package org.fossify.filemanager.adapters

import android.util.TypedValue
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import org.fossify.commons.adapters.MyRecyclerViewAdapter
import org.fossify.commons.views.MyRecyclerView
import org.fossify.filemanager.activities.SimpleActivity
import org.fossify.filemanager.databinding.ItemDecompressionListFileDirBinding
import org.fossify.filemanager.databinding.ItemNetworkConnectionBinding
import org.fossify.filemanager.models.ListItem
import org.fossify.filemanager.models.NetworkConnection
import java.util.Locale

class ConnectionItemsAdapter(activity: SimpleActivity, var listItems: MutableList<NetworkConnection>, recyclerView: MyRecyclerView, itemClick: (Any) -> Unit) :
    MyRecyclerViewAdapter(activity, recyclerView, itemClick) {
    override fun getActionMenuId(): Int {
        TODO("Not yet implemented")
    }

    override fun prepareActionMode(menu: Menu) {
        TODO("Not yet implemented")
    }

    override fun actionItemPressed(id: Int) {
        TODO("Not yet implemented")
    }

    override fun getSelectableItemCount(): Int {
        TODO("Not yet implemented")
    }

    override fun getIsItemSelectable(position: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun getItemSelectionKey(position: Int): Int? {
        TODO("Not yet implemented")
    }

    override fun getItemKeyPosition(key: Int): Int {
        TODO("Not yet implemented")
    }

    override fun onActionModeCreated() {
        TODO("Not yet implemented")
    }

    override fun onActionModeDestroyed() {
        TODO("Not yet implemented")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return createViewHolder(ItemNetworkConnectionBinding.inflate(layoutInflater, parent, false).root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fileDirItem = listItems[position]
        holder.bindView(fileDirItem, true, false) { itemView, layoutPosition ->
            setupView(itemView, fileDirItem)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = listItems.size

    private fun setupView(view: View, listItem: NetworkConnection) {
        ItemNetworkConnectionBinding.bind(view).apply {
            tvHost.text = listItem.host
            tvType.text = listItem.connectionType.toString()
            tvDisplayName.text = listItem.displayName
            tvSharedPath.text = listItem.sharedPath
        }
    }
}
