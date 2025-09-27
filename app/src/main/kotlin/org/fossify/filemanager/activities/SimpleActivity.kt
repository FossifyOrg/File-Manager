package org.fossify.filemanager.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Environment
import android.provider.Settings
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.dialogs.ConfirmationAdvancedDialog
import org.fossify.commons.extensions.hasPermission
import android.net.Uri
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.PERMISSION_WRITE_STORAGE
import org.fossify.commons.helpers.isRPlus
import org.fossify.filemanager.R

open class SimpleActivity : BaseSimpleActivity() {
    override fun getAppIconIDs() = arrayListOf(
        R.mipmap.ic_launcher_red,
        R.mipmap.ic_launcher_pink,
        R.mipmap.ic_launcher_purple,
        R.mipmap.ic_launcher_deep_purple,
        R.mipmap.ic_launcher_indigo,
        R.mipmap.ic_launcher_blue,
        R.mipmap.ic_launcher_light_blue,
        R.mipmap.ic_launcher_cyan,
        R.mipmap.ic_launcher_teal,
        R.mipmap.ic_launcher,
        R.mipmap.ic_launcher_light_green,
        R.mipmap.ic_launcher_lime,
        R.mipmap.ic_launcher_yellow,
        R.mipmap.ic_launcher_amber,
        R.mipmap.ic_launcher_orange,
        R.mipmap.ic_launcher_deep_orange,
        R.mipmap.ic_launcher_brown,
        R.mipmap.ic_launcher_blue_grey,
        R.mipmap.ic_launcher_grey_black
    )

    companion object {
        private const val MANAGE_STORAGE_RC = 201
    }

    override fun getAppLauncherName() = getString(R.string.app_launcher_name)

    override fun getRepositoryName() = "File-Manager"

   @SuppressLint("NewApi")
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        isAskingPermissions = false
        if (requestCode == MANAGE_STORAGE_RC && isRPlus()) {
            actionOnPermission?.invoke(Environment.isExternalStorageManager())
        }
    }

    @SuppressLint("NewApi")
    fun hasStoragePermission(): Boolean {
        return if (isRPlus()) {
            Environment.isExternalStorageManager()
        } else {
            hasPermission(PERMISSION_WRITE_STORAGE)
        }
    }

    @SuppressLint("InlinedApi")
    fun handleStoragePermission(callback: (granted: Boolean) -> Unit) {
        actionOnPermission = null
        if (hasStoragePermission()) {
            callback(true)
        } else {
            if (isRPlus()) {
                ConfirmationAdvancedDialog(this, "", R.string.access_storage_prompt, R.string.ok, 0, false) { success ->
                    if (success) {
                        isAskingPermissions = true
                        actionOnPermission = callback
                        try {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                            intent.addCategory("android.intent.category.DEFAULT")
                            intent.data = Uri.parse("package:$packageName")
                            startActivityForResult(intent, MANAGE_STORAGE_RC)
                        } catch (e: android.content.ActivityNotFoundException) {
                            showErrorToast(e)
                            val intent = Intent()
                            intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                            startActivityForResult(intent, MANAGE_STORAGE_RC)
                        } catch (e: SecurityException) {
                            showErrorToast(e)
                            finish()
                        }
                    } else {
                        finish()
                    }
                }
            } else {
                handlePermission(PERMISSION_WRITE_STORAGE, callback)
            }
        }
    }
}
