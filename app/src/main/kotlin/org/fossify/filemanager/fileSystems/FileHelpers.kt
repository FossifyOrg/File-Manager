package org.fossify.filemanager.fileSystems

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.fossify.commons.enums.ConnectionTypes
import org.fossify.filemanager.helpers.Helpers

object FileHelpers {
    fun launchSMB(mPath: String, context: Context, mimType: String? = null) {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                val extractedPath = Helpers.retrievePath(mPath)
                val uri = Helpers.createNanoHttpdUrl(ConnectionTypes.SMB,extractedPath).toUri()
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

    fun launchWebDav(mPath: String, context: Context, mimType: String? = null) {
        try {
            CoroutineScope(Dispatchers.IO).launch {

                val extractedPath = Helpers.retrievePath(mPath)
                val uri = Helpers.createNanoHttpdUrl(ConnectionTypes.WebDav,extractedPath).toUri()
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

    fun launchSFTP(mPath: String, context: Context,mimType: String? = null) {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                val uri = Helpers.createNanoHttpdUrl(ConnectionTypes.SMB).toUri()
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

    fun launchFTP(mPath: String, context: Context,mimType: String? = null) {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                val uri = Helpers.createNanoHttpdUrl(ConnectionTypes.SMB).toUri()
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
