package org.fossify.filemanager.dialogs

import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.helpers.TAB_FILES
import org.fossify.commons.helpers.TAB_RECENT_FILES
import org.fossify.commons.helpers.TAB_STORAGE_ANALYSIS
import org.fossify.commons.helpers.isOreoPlus
import org.fossify.commons.views.MyAppCompatCheckbox
import org.fossify.filemanager.R
import org.fossify.filemanager.databinding.DialogManageVisibleTabsBinding
import org.fossify.filemanager.extensions.config
import org.fossify.filemanager.helpers.ALL_TABS_MASK

class ManageVisibleTabsDialog(val activity: BaseSimpleActivity) {
    private val binding = DialogManageVisibleTabsBinding.inflate(activity.layoutInflater)
    private val tabs = LinkedHashMap<Int, Int>()

    init {
        tabs.apply {
            put(TAB_FILES, R.id.manage_visible_tabs_files)
            put(TAB_RECENT_FILES, R.id.manage_visible_tabs_recent_files)
            put(TAB_STORAGE_ANALYSIS, R.id.manage_visible_tabs_storage_analysis)
        }

        if (!isOreoPlus()) {
            binding.manageVisibleTabsStorageAnalysis.beGone()
        }

        val showTabs = activity.config.showTabs
        for ((key, value) in tabs) {
            binding.root.findViewById<MyAppCompatCheckbox>(value).isChecked = showTabs and key != 0
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this)
            }
    }

    private fun dialogConfirmed() {
        var result = 0
        for ((key, value) in tabs) {
            if (binding.root.findViewById<MyAppCompatCheckbox>(value).isChecked) {
                result += key
            }
        }

        if (result == 0) {
            result = ALL_TABS_MASK
        }

        activity.config.showTabs = result
    }
}
