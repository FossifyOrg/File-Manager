package org.fossify.filemanager.fileSystems

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.net.toUri
import jcifs.smb.SmbFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.fossify.commons.enums.ConnectionTypes
import org.fossify.filemanager.helpers.Helpers
import org.fossify.filemanager.helpers.PORT_SMB
import org.fossify.filemanager.helpers.PORT_WEBDAV
import org.fossify.filemanager.models.ListItem

object FileHelpers {
    val URL: String = "http://127.0.0.1"
    fun launchSMB(mPath: String, context: Context, mimType: String? = null) {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                val extractedPath = Helpers.retrievePath(mPath)
                val uri = "${URL}:${PORT_SMB}${extractedPath}"
                var i: Intent
                if (mimType != null) {
                    i =
                        Intent(Intent.ACTION_VIEW)
                    i.setDataAndType(uri.toUri(), mimType)
                } else {
                    i =
                        Intent(Intent.ACTION_VIEW)
                    i.setDataAndType(uri.toUri(), MimeTypes.getMimeTypes(mPath))
                }
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

    fun launchWebDav(mPath: String, context: Context, mimType: String? = null) {
        try {
            CoroutineScope(Dispatchers.IO).launch {

                val extractedPath = Helpers.retrievePath(mPath)
                val uri = "${URL}:${PORT_WEBDAV}/${extractedPath}"

                var i: Intent
                if (mimType != null) {
                    i =
                        Intent(Intent.ACTION_VIEW)
                    i.setDataAndType(uri.toUri(), mimType)
                } else {
                    i =
                        Intent(Intent.ACTION_VIEW)
                    i.setDataAndType(uri.toUri(), MimeTypes.getMimeTypes(mPath))
                }
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

    fun launchSFTP(connectionTypes: ConnectionTypes, mPath: String, context: Context,mimType: String? = null) {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                val port = Helpers.getPortForEachService(connectionTypes)
                val uri = Helpers.createNanoHttpdUrl(connectionTypes, mPath, port = port).toUri()
                var i: Intent
                if (mimType != null) {
                    i =
                        Intent(Intent.ACTION_VIEW)
                    i.setDataAndType(uri, mimType)
                } else {
                    i =
                        Intent(Intent.ACTION_VIEW)
                    i.setDataAndType(uri, MimeTypes.getMimeTypes(mPath))
                }

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

    fun launchFTP(connectionTypes: ConnectionTypes, mPath: String, context: Context,mimType: String? = null) {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                val port = Helpers.getPortForEachService(connectionTypes)
                val uri = Helpers.createNanoHttpdUrl(connectionTypes, mPath, port = port).toUri()
                var i: Intent
                if (mimType != null) {
                    i =
                        Intent(Intent.ACTION_VIEW)
                    i.setDataAndType(uri,mimType)
                } else {
                    i =
                        Intent(Intent.ACTION_VIEW)
                    i.setDataAndType(uri, MimeTypes.getMimeTypes(mPath))
                }

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
}
