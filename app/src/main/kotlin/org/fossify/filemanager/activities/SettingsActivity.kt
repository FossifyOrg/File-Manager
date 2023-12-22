package org.fossify.filemanager.activities

import android.content.Intent
import android.os.Bundle
import org.fossify.commons.dialogs.ChangeDateTimeFormatDialog
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.dialogs.RadioGroupDialog
import org.fossify.commons.dialogs.SecurityDialog
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.*
import org.fossify.commons.models.RadioItem
import org.fossify.filemanager.R
import org.fossify.filemanager.databinding.ActivitySettingsBinding
import org.fossify.filemanager.dialogs.ManageVisibleTabsDialog
import org.fossify.filemanager.extensions.config
import org.fossify.filemanager.helpers.RootHelpers
import java.util.Locale
import kotlin.system.exitProcess

class SettingsActivity : SimpleActivity() {
    private val binding by viewBinding(ActivitySettingsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.apply {
            updateMaterialActivityViews(settingsCoordinator, settingsHolder, useTransparentNavigation = true, useTopSearchMenu = false)
            setupMaterialScrollListener(settingsNestedScrollview, settingsToolbar)
        }
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.settingsToolbar, NavigationIcon.Arrow)

        setupCustomizeColors()
        setupUseEnglish()
        setupLanguage()
        setupManageFavorites()
        setupManageShownTabs()
        setupChangeDateTimeFormat()
        setupFontSize()
        setupShowHidden()
        setupEnablePullToRefresh()
        setupPressBackTwice()
        setupHiddenItemPasswordProtection()
        setupAppPasswordProtection()
        setupFileDeletionPasswordProtection()
        setupKeepLastModified()
        setupDeleteConfirmation()
        setupEnableRootAccess()
        updateTextColors(binding.settingsNestedScrollview)

        binding.apply {
            arrayOf(
                settingsColorCustomizationSectionLabel,
                settingsGeneralSettingsLabel,
                settingsVisibilityLabel,
                settingsScrollingLabel,
                settingsFileOperationsLabel,
                settingsSecurityLabel
            ).forEach {
                it.setTextColor(getProperPrimaryColor())
            }
        }
    }

    private fun setupCustomizeColors() {
        binding.settingsColorCustomizationHolder.setOnClickListener {
            startCustomizationActivity()
        }
    }

    private fun setupUseEnglish() {
        binding.apply {
            settingsUseEnglishHolder.beVisibleIf((config.wasUseEnglishToggled || Locale.getDefault().language != "en") && !isTiramisuPlus())
            settingsUseEnglish.isChecked = config.useEnglish
            settingsUseEnglishHolder.setOnClickListener {
                settingsUseEnglish.toggle()
                config.useEnglish = settingsUseEnglish.isChecked
                exitProcess(0)
            }
        }
    }

    private fun setupLanguage() {
        binding.apply {
            settingsLanguage.text = Locale.getDefault().displayLanguage
            settingsLanguageHolder.beVisibleIf(isTiramisuPlus())
            settingsLanguageHolder.setOnClickListener {
                launchChangeAppLanguageIntent()
            }
        }
    }

    private fun setupManageFavorites() {
        binding.settingsManageFavoritesHolder.setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }
    }

    private fun setupManageShownTabs() {
        binding.settingsManageTabsHolder.setOnClickListener {
            ManageVisibleTabsDialog(this)
        }
    }

    private fun setupChangeDateTimeFormat() {
        binding.settingsChangeDateTimeFormatHolder.setOnClickListener {
            ChangeDateTimeFormatDialog(this) {}
        }
    }

    private fun setupFontSize() {
        binding.settingsFontSize.text = getFontSizeText()
        binding.settingsFontSizeHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(FONT_SIZE_SMALL, getString(R.string.small)),
                RadioItem(FONT_SIZE_MEDIUM, getString(R.string.medium)),
                RadioItem(FONT_SIZE_LARGE, getString(R.string.large)),
                RadioItem(FONT_SIZE_EXTRA_LARGE, getString(R.string.extra_large))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.fontSize) {
                config.fontSize = it as Int
                binding.settingsFontSize.text = getFontSizeText()
            }
        }
    }

    private fun setupShowHidden() {
        binding.settingsShowHidden.isChecked = config.showHidden
        binding.settingsShowHiddenHolder.setOnClickListener {
            if (config.showHidden) {
                toggleShowHidden()
            } else {
                handleHiddenFolderPasswordProtection {
                    toggleShowHidden()
                }
            }
        }
    }

    private fun toggleShowHidden() {
        binding.settingsShowHidden.toggle()
        config.showHidden = binding.settingsShowHidden.isChecked
    }

    private fun setupEnablePullToRefresh() {
        binding.apply {
            settingsEnablePullToRefresh.isChecked = config.enablePullToRefresh
            settingsEnablePullToRefreshHolder.setOnClickListener {
                settingsEnablePullToRefresh.toggle()
                config.enablePullToRefresh = settingsEnablePullToRefresh.isChecked
            }
        }
    }

    private fun setupPressBackTwice() {
        binding.apply {
            settingsPressBackTwice.isChecked = config.pressBackTwice
            settingsPressBackTwiceHolder.setOnClickListener {
                settingsPressBackTwice.toggle()
                config.pressBackTwice = settingsPressBackTwice.isChecked
            }
        }
    }

    private fun setupHiddenItemPasswordProtection() {
        binding.settingsPasswordProtection.isChecked = config.isHiddenPasswordProtectionOn
        binding.settingsPasswordProtectionHolder.setOnClickListener {
            val tabToShow = if (config.isHiddenPasswordProtectionOn) config.hiddenProtectionType else SHOW_ALL_TABS
            SecurityDialog(this, config.hiddenPasswordHash, tabToShow) { hash, type, success ->
                if (success) {
                    val hasPasswordProtection = config.isHiddenPasswordProtectionOn
                    binding.settingsPasswordProtection.isChecked = !hasPasswordProtection
                    config.isHiddenPasswordProtectionOn = !hasPasswordProtection
                    config.hiddenPasswordHash = if (hasPasswordProtection) "" else hash
                    config.hiddenProtectionType = type

                    if (config.isHiddenPasswordProtectionOn) {
                        val confirmationTextId = if (config.hiddenProtectionType == PROTECTION_FINGERPRINT)
                            R.string.fingerprint_setup_successfully else R.string.protection_setup_successfully
                        ConfirmationDialog(this, "", confirmationTextId, R.string.ok, 0) { }
                    }
                }
            }
        }
    }

    private fun setupAppPasswordProtection() {
        binding.settingsAppPasswordProtection.isChecked = config.isAppPasswordProtectionOn
        binding.settingsAppPasswordProtectionHolder.setOnClickListener {
            val tabToShow = if (config.isAppPasswordProtectionOn) config.appProtectionType else SHOW_ALL_TABS
            SecurityDialog(this, config.appPasswordHash, tabToShow) { hash, type, success ->
                if (success) {
                    val hasPasswordProtection = config.isAppPasswordProtectionOn
                    binding.settingsAppPasswordProtection.isChecked = !hasPasswordProtection
                    config.isAppPasswordProtectionOn = !hasPasswordProtection
                    config.appPasswordHash = if (hasPasswordProtection) "" else hash
                    config.appProtectionType = type

                    if (config.isAppPasswordProtectionOn) {
                        val confirmationTextId = if (config.appProtectionType == PROTECTION_FINGERPRINT)
                            R.string.fingerprint_setup_successfully else R.string.protection_setup_successfully
                        ConfirmationDialog(this, "", confirmationTextId, R.string.ok, 0) { }
                    }
                }
            }
        }
    }

    private fun setupFileDeletionPasswordProtection() {
        binding.settingsFileDeletionPasswordProtection.isChecked = config.isDeletePasswordProtectionOn
        binding.settingsFileDeletionPasswordProtectionHolder.setOnClickListener {
            val tabToShow = if (config.isDeletePasswordProtectionOn) config.deleteProtectionType else SHOW_ALL_TABS
            SecurityDialog(this, config.deletePasswordHash, tabToShow) { hash, type, success ->
                if (success) {
                    val hasPasswordProtection = config.isDeletePasswordProtectionOn
                    binding.settingsFileDeletionPasswordProtection.isChecked = !hasPasswordProtection
                    config.isDeletePasswordProtectionOn = !hasPasswordProtection
                    config.deletePasswordHash = if (hasPasswordProtection) "" else hash
                    config.deleteProtectionType = type

                    if (config.isDeletePasswordProtectionOn) {
                        val confirmationTextId = if (config.deleteProtectionType == PROTECTION_FINGERPRINT)
                            R.string.fingerprint_setup_successfully else R.string.protection_setup_successfully
                        ConfirmationDialog(this, "", confirmationTextId, R.string.ok, 0) { }
                    }
                }
            }
        }
    }

    private fun setupKeepLastModified() {
        binding.apply {
            settingsKeepLastModified.isChecked = config.keepLastModified
            settingsKeepLastModifiedHolder.setOnClickListener {
                settingsKeepLastModified.toggle()
                config.keepLastModified = settingsKeepLastModified.isChecked
            }
        }
    }

    private fun setupDeleteConfirmation() {
        binding.apply {
            settingsSkipDeleteConfirmation.isChecked = config.skipDeleteConfirmation
            settingsSkipDeleteConfirmationHolder.setOnClickListener {
                settingsSkipDeleteConfirmation.toggle()
                config.skipDeleteConfirmation = settingsSkipDeleteConfirmation.isChecked
            }
        }
    }

    private fun setupEnableRootAccess() {
        binding.apply {
            settingsEnableRootAccessHolder.beVisibleIf(config.isRootAvailable)
            settingsEnableRootAccess.isChecked = config.enableRootAccess
            settingsEnableRootAccessHolder.setOnClickListener {
                if (!config.enableRootAccess) {
                    RootHelpers(this@SettingsActivity).askRootIfNeeded {
                        toggleRootAccess(it)
                    }
                } else {
                    toggleRootAccess(false)
                }
            }
        }
    }

    private fun toggleRootAccess(enable: Boolean) {
        binding.settingsEnableRootAccess.isChecked = enable
        config.enableRootAccess = enable
    }
}
