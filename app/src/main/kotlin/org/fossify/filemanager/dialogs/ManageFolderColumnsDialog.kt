package org.fossify.filemanager.dialogs

import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.views.MyAppCompatCheckbox
import org.fossify.filemanager.databinding.DialogManageFolderColumsBinding
import org.fossify.filemanager.extensions.config

class ManageFolderColumnsDialog(val activity: BaseSimpleActivity) {
    private var config = activity.config
    private val binding: DialogManageFolderColumsBinding = DialogManageFolderColumsBinding
        .inflate(activity.layoutInflater)
    private var showFolderSize : Boolean = config.showFolderSize
    private var showChildrenCount : Boolean = config.showFolderChildrenCount
    private var showModifiedAt : Boolean = config.showFolderLastModifiedAt

    init {

        setupColumnCheckboxSelection()

        activity.getAlertDialogBuilder()
            .setPositiveButton("OK",{dialog,which -> dialogConfirmed()})
            .setNegativeButton("Cancel",null)
            .apply {
                activity.setupDialogStuff(binding.root, this)
            }
    }

    private fun setupColumnCheckboxSelection(){
        setupCheckbox(binding.manageFolderColumnsSize, showFolderSize) { showFolderSize = it }
        setupCheckbox(binding.manageFolderColumnsChildrenCount, showChildrenCount) { showChildrenCount = it }
        setupCheckbox(binding.manageFolderColumnsModifiedAt, showModifiedAt) { showModifiedAt = it }
    }

    private fun setupCheckbox(checkbox: MyAppCompatCheckbox, initialState: Boolean, onChanged: (Boolean) -> Unit) {
        checkbox.isChecked = initialState
        checkbox.setOnClickListener {
            onChanged(checkbox.isChecked)
        }
    }

    private fun dialogConfirmed(){
        if(config.showFolderSize != showFolderSize){
            config.showFolderSize = showFolderSize
        }

        if(config.showFolderChildrenCount != showChildrenCount){
            config.showFolderChildrenCount = showChildrenCount
        }

        if(config.showFolderLastModifiedAt != showModifiedAt){
            config.showFolderLastModifiedAt = showModifiedAt
        }
    }
}
