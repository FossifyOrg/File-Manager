package org.fossify.filemanager.dialogs

import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.helpers.*
import org.fossify.filemanager.R
import org.fossify.filemanager.databinding.DialogChangeSortingBinding
import org.fossify.filemanager.extensions.config

class ChangeSortingDialog(val activity: BaseSimpleActivity, val path: String = "", val callback: () -> Unit) {
    private var currSorting = 0
    private var config = activity.config
    private val binding: DialogChangeSortingBinding

    init {
        currSorting = config.getFolderSorting(path)
        binding = DialogChangeSortingBinding.inflate(activity.layoutInflater).apply {
            sortingDialogUseForThisFolder.isChecked = config.hasCustomSorting(path)

            sortingDialogNumericSorting.beVisibleIf(currSorting and SORT_BY_NAME != 0)
            sortingDialogNumericSorting.isChecked = currSorting and SORT_USE_NUMERIC_VALUE != 0
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.sort_by)
            }

        setupSortRadio()
        setupOrderRadio()
    }

    private fun setupSortRadio() {
        binding.apply {
            sortingDialogRadioSorting.setOnCheckedChangeListener { group, checkedId ->
                val isSortingByName = checkedId == sortingDialogRadioName.id
                binding.sortingDialogNumericSorting.beVisibleIf(isSortingByName)
            }

            val sortBtn = when {
                currSorting and SORT_BY_SIZE != 0 -> sortingDialogRadioSize
                currSorting and SORT_BY_DATE_MODIFIED != 0 -> sortingDialogRadioLastModified
                currSorting and SORT_BY_EXTENSION != 0 -> sortingDialogRadioExtension
                else -> sortingDialogRadioName
            }
            sortBtn.isChecked = true
        }
    }

    private fun setupOrderRadio() {
        var orderBtn = binding.sortingDialogRadioAscending

        if (currSorting and SORT_DESCENDING != 0) {
            orderBtn = binding.sortingDialogRadioDescending
        }
        orderBtn.isChecked = true
    }

    private fun dialogConfirmed() {
        val sortingRadio = binding.sortingDialogRadioSorting
        var sorting = when (sortingRadio.checkedRadioButtonId) {
            R.id.sorting_dialog_radio_name -> SORT_BY_NAME
            R.id.sorting_dialog_radio_size -> SORT_BY_SIZE
            R.id.sorting_dialog_radio_last_modified -> SORT_BY_DATE_MODIFIED
            else -> SORT_BY_EXTENSION
        }

        if (binding.sortingDialogRadioOrder.checkedRadioButtonId == R.id.sorting_dialog_radio_descending) {
            sorting = sorting or SORT_DESCENDING
        }

        if (binding.sortingDialogNumericSorting.isChecked) {
            sorting = sorting or SORT_USE_NUMERIC_VALUE
        }

        if (binding.sortingDialogUseForThisFolder.isChecked) {
            config.saveCustomSorting(path, sorting)
        } else {
            config.removeCustomSorting(path)
            config.sorting = sorting
        }
        callback()
    }
}
