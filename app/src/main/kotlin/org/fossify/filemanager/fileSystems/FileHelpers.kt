package org.fossify.filemanager.fileSystems

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.fossify.filemanager.models.ListItem
import java.io.File

object FileHelpers {
    val URL: String = "http://127.0.0.1:7871/"
    fun launchSMB(item: ListItem, context: Context) {
        try {
            CoroutineScope(Dispatchers.Main).launch {
                val uri = "${URL}${item.parent}${(item.path.toUri()).path}".toUri()
                kotlinx . coroutines . delay (50)
                val i =
                    Intent(Intent.ACTION_VIEW)
                i.setDataAndType(uri, MimeTypes.getMimeTypes(item.mPath))
                val packageManager: PackageManager = context.packageManager
                val resInfos = packageManager.queryIntentActivities(i, 0)
                if (resInfos.size > 0){ context.startActivity(i) } } }
                catch(exp: Exception) {
                    Log.e("Activity Launch Failed", exp.toString())
                }
            }
        }

    }
}
