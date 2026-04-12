package org.fossify.filemanager.fileSystems

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import jcifs.smb.SmbFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.fossify.commons.enums.ConnectionTypes
import org.fossify.filemanager.helpers.Helpers
import org.fossify.filemanager.models.ListItem
import java.io.File

object FileHelpers {
    val URL: String = "http://127.0.0.1:7871/"
    fun launchSMB(item: ListItem, context: Context, smb: SmbFile) {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                val uri = "${URL}${item.parent}${(item.path.toUri()).path}".toUri()
                val i =
                    Intent(Intent.ACTION_VIEW)
                i.setDataAndType(uri, MimeTypes.getMimeTypes(item.mPath))
                val packageManager: PackageManager = context.packageManager
                val resInfos = packageManager.queryIntentActivities(i, 0)
                if (resInfos.size > 0) {
                    context.startActivity(i)
                }
            }
        } catch (exp: Exception) {
            Log.e("Activity Launch Failed", exp.toString())
        }
    }

    fun launchWebDav(connectionTypes: ConnectionTypes, item: ListItem, context: Context){
        try {
            CoroutineScope(Dispatchers.IO).launch {
                val port = Helpers.getPortForEachService(connectionTypes)
                val uri = Helpers.createUrl(connectionTypes, item.mPath, port = port).toUri()
                val i =
                    Intent(Intent.ACTION_VIEW)
                i.setDataAndType(uri, MimeTypes.getMimeTypes(item.mPath))

                val packageManager: PackageManager = context.packageManager
                val resInfos = packageManager.queryIntentActivities(i, 0)
                if (resInfos.size > 0) {
                    context.startActivity(i)
                }
            }
        }
        catch (exp: Exception){
            Log.e("Activity Launch Failed", exp.toString())
        }
    }

    fun launchSFTP(connectionTypes: ConnectionTypes,item: ListItem,context: Context){
        try{
            CoroutineScope(Dispatchers.IO).launch {
                val port = Helpers.getPortForEachService(connectionTypes)
                val uri = Helpers.createUrl(connectionTypes, item.mPath, port = port).toUri()
                val i =
                    Intent(Intent.ACTION_VIEW)
                i.setDataAndType(uri, MimeTypes.getMimeTypes(item.mPath))

                val packageManager: PackageManager = context.packageManager
                val resInfos = packageManager.queryIntentActivities(i, 0)
                if (resInfos.size > 0) {
                    context.startActivity(i)
                }
            }
        }
        catch (exp: Exception){
            Log.e("Activity Launch Failed", exp.toString())
        }
    }
    fun launchFTP(connectionTypes: ConnectionTypes,item: ListItem,context: Context){
        try{
            CoroutineScope(Dispatchers.IO).launch {
                val port = Helpers.getPortForEachService(connectionTypes)
                val uri = Helpers.createUrl(connectionTypes, item.mPath, port = port).toUri()
                val i =
                    Intent(Intent.ACTION_VIEW)
                i.setDataAndType(uri, MimeTypes.getMimeTypes(item.mPath))

                val packageManager: PackageManager = context.packageManager
                val resInfos = packageManager.queryIntentActivities(i, 0)
                if (resInfos.size > 0) {
                    context.startActivity(i)
                }
            }
        }
        catch (exp: Exception){
            Log.e("Activity Launch Failed", exp.toString())
        }
    }
}
