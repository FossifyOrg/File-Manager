package org.fossify.filemanager.adapters

import android.view.*
import android.widget.PopupMenu
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.adapters.MyRecyclerViewAdapter
import org.fossify.commons.extensions.getPopupMenuTheme
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.setupViewBackground
import org.fossify.commons.interfaces.RefreshRecyclerViewListener
import org.fossify.commons.views.MyRecyclerView
import org.fossify.filemanager.R
import org.fossify.filemanager.databinding.ItemManageFavoriteBinding
import org.fossify.filemanager.extensions.config

class ManageFavoritesAdapter(
    activity: BaseSimpleActivity, var favorites: ArrayList<String>, val listener: RefreshRecyclerViewListener?,
    recyclerView: MyRecyclerView, itemClick: (Any) -> Unit
) : MyRecyclerViewAdapter(activity, recyclerView, itemClick) {

    private val config = activity.config

    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_remove_only

    override fun actionItemPressed(id: Int) {
        when (id) {
            R.id.cab_remove -> removeSelection()
        }
    }

    override fun getSelectableItemCount() = favorites.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = favorites.getOrNull(position)?.hashCode()

    override fun getItemKeyPosition(key: Int) = favorites.indexOfFirst { it.hashCode() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun prepareActionMode(menu: Menu) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return createViewHolder(ItemManageFavoriteBinding.inflate(layoutInflater, parent, false).root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val favorite = favorites[position]
        holder.bindView(favorite, true, true) { itemView, layoutPosition ->
            setupView(itemView, favorite, selectedKeys.contains(favorite.hashCode()))
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = favorites.size

    private fun setupView(view: View, favorite: String, isSelected: Boolean) {
        ItemManageFavoriteBinding.bind(view).apply {
            root.setupViewBackground(activity)
            manageFavoriteTitle.apply {
                text = favorite
                setTextColor(activity.getProperTextColor())
            }

            manageFavoriteHolder.isSelected = isSelected

            overflowMenuIcon.drawable.apply {
                mutate()
                setTint(activity.getProperTextColor())
            }

            overflowMenuIcon.setOnClickListener {
                showPopupMenu(overflowMenuAnchor, favorite)
            }
        }
    }

    private fun showPopupMenu(view: View, favorite: String) {
        finishActMode()
        val theme = activity.getPopupMenuTheme()
        val contextTheme = ContextThemeWrapper(activity, theme)

        PopupMenu(contextTheme, view, Gravity.END).apply {
            inflate(getActionMenuId())
            setOnMenuItemClickListener { item ->
                val eventTypeId = favorite.hashCode()
                when (item.itemId) {
                    R.id.cab_remove -> {
                        executeItemMenuOperation(eventTypeId) {
                            removeSelection()
                        }
                    }
                }
                true
            }
            show()
        }
    }

    private fun executeItemMenuOperation(eventTypeId: Int, callback: () -> Unit) {
        selectedKeys.clear()
        selectedKeys.add(eventTypeId)
        callback()
    }

    private fun removeSelection() {
        val removeFavorites = ArrayList<String>(selectedKeys.size)
        val positions = ArrayList<Int>()
        selectedKeys.forEach { key ->
            val position = favorites.indexOfFirst { it.hashCode() == key }
            if (position != -1) {
                positions.add(position)

                val favorite = getItemWithKey(key)
                if (favorite != null) {
                    removeFavorites.add(favorite)
                    config.removeFavorite(favorite)
                }
            }
        }

        positions.sortDescending()
        removeSelectedItems(positions)

        favorites.removeAll(removeFavorites.toSet())
        if (favorites.isEmpty()) {
            listener?.refreshItems()
        }
    }

    private fun getItemWithKey(key: Int): String? = favorites.firstOrNull { it.hashCode() == key }
}
