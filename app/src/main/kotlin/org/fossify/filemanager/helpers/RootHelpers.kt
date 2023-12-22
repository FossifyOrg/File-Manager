package org.fossify.filemanager.helpers

import android.app.Activity
import com.stericson.RootShell.execution.Command
import com.stericson.RootTools.RootTools
import org.fossify.commons.extensions.areDigitsOnly
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.SORT_BY_SIZE
import org.fossify.commons.models.FileDirItem
import org.fossify.filemanager.R
import org.fossify.filemanager.extensions.config
import org.fossify.filemanager.models.ListItem
import java.io.File

class RootHelpers(val activity: Activity) {
    fun askRootIfNeeded(callback: (success: Boolean) -> Unit) {
        val cmd = "ls -lA"
        val command = object : Command(0, cmd) {
            override fun commandOutput(id: Int, line: String) {
                callback(true)
                super.commandOutput(id, line)
            }
        }

        try {
            RootTools.getShell(true).add(command)
        } catch (exception: Exception) {
            activity.showErrorToast(exception)
            callback(false)
        }
    }

    fun getFiles(path: String, callback: (originalPath: String, listItems: ArrayList<ListItem>) -> Unit) {
        getFullLines(path) {
            val fullLines = it

            val files = ArrayList<ListItem>()
            val hiddenArgument = if (activity.config.shouldShowHidden()) "-A " else ""
            val cmd = "ls $hiddenArgument$path"

            val command = object : Command(0, cmd) {
                override fun commandOutput(id: Int, line: String) {
                    val file = File(path, line)
                    val fullLine = fullLines.firstOrNull { it.endsWith(" $line") }
                    val isDirectory = fullLine?.startsWith('d') ?: file.isDirectory
                    val fileDirItem = ListItem(file.absolutePath, line, isDirectory, 0, 0, 0, false, false)
                    files.add(fileDirItem)
                    super.commandOutput(id, line)
                }

                override fun commandCompleted(id: Int, exitcode: Int) {
                    if (files.isEmpty()) {
                        callback(path, files)
                    } else {
                        getChildrenCount(files, path, callback)
                    }

                    super.commandCompleted(id, exitcode)
                }
            }

            runCommand(command)
        }
    }

    private fun getFullLines(path: String, callback: (ArrayList<String>) -> Unit) {
        val fullLines = ArrayList<String>()
        val hiddenArgument = if (activity.config.shouldShowHidden()) "-Al " else "-l "
        val cmd = "ls $hiddenArgument$path"

        val command = object : Command(0, cmd) {
            override fun commandOutput(id: Int, line: String) {
                fullLines.add(line)
                super.commandOutput(id, line)
            }

            override fun commandCompleted(id: Int, exitcode: Int) {
                callback(fullLines)
                super.commandCompleted(id, exitcode)
            }
        }

        runCommand(command)
    }

    private fun getChildrenCount(files: ArrayList<ListItem>, path: String, callback: (originalPath: String, listItems: ArrayList<ListItem>) -> Unit) {
        val hiddenArgument = if (activity.config.shouldShowHidden()) "-A " else ""
        var cmd = ""
        files.filter { it.isDirectory }.forEach {
            cmd += "ls $hiddenArgument${it.path} |wc -l;"
        }
        cmd = cmd.trimEnd(';') + " | cat"

        val lines = ArrayList<String>()
        val command = object : Command(0, cmd) {
            override fun commandOutput(id: Int, line: String) {
                lines.add(line)
                super.commandOutput(id, line)
            }

            override fun commandCompleted(id: Int, exitcode: Int) {
                files.filter { it.isDirectory }.forEachIndexed { index, fileDirItem ->
                    val childrenCount = lines[index]
                    if (childrenCount.areDigitsOnly()) {
                        fileDirItem.children = childrenCount.toInt()
                    }
                }

                if (activity.config.getFolderSorting(path) and SORT_BY_SIZE == 0) {
                    callback(path, files)
                } else {
                    getFileSizes(files, path, callback)
                }
                super.commandCompleted(id, exitcode)
            }
        }

        runCommand(command)
    }

    private fun getFileSizes(files: ArrayList<ListItem>, path: String, callback: (originalPath: String, listItems: ArrayList<ListItem>) -> Unit) {
        var cmd = ""
        files.filter { !it.isDirectory }.forEach {
            cmd += "stat -t ${it.path};"
        }

        val lines = ArrayList<String>()
        val command = object : Command(0, cmd) {
            override fun commandOutput(id: Int, line: String) {
                lines.add(line)
                super.commandOutput(id, line)
            }

            override fun commandCompleted(id: Int, exitcode: Int) {
                files.filter { !it.isDirectory }.forEachIndexed { index, fileDirItem ->
                    var line = lines[index]
                    if (line.isNotEmpty() && line != "0") {
                        if (line.length >= fileDirItem.path.length) {
                            line = line.substring(fileDirItem.path.length).trim()
                            val size = line.split(" ")[0]
                            if (size.areDigitsOnly()) {
                                fileDirItem.size = size.toLong()
                            }
                        }
                    }
                }

                callback(path, files)
                super.commandCompleted(id, exitcode)
            }
        }

        runCommand(command)
    }

    private fun runCommand(command: Command) {
        try {
            RootTools.getShell(true).add(command)
        } catch (e: Exception) {
            activity.showErrorToast(e)
        }
    }

    fun createFileFolder(path: String, isFile: Boolean, callback: (success: Boolean) -> Unit) {
        if (!RootTools.isRootAvailable()) {
            activity.toast(R.string.rooted_device_only)
            return
        }

        tryMountAsRW(path) {
            val mountPoint = it
            val targetPath = path.trim('/')
            val mainCommand = if (isFile) "touch" else "mkdir"
            val cmd = "$mainCommand \"/$targetPath\""
            val command = object : Command(0, cmd) {
                override fun commandCompleted(id: Int, exitcode: Int) {
                    callback(exitcode == 0)
                    mountAsRO(mountPoint)
                    super.commandCompleted(id, exitcode)
                }
            }

            runCommand(command)
        }
    }

    private fun mountAsRO(mountPoint: String?) {
        if (mountPoint != null) {
            val cmd = "umount -r \"$mountPoint\""
            val command = object : Command(0, cmd) {}
            runCommand(command)
        }
    }

    // inspired by Amaze File Manager
    private fun tryMountAsRW(path: String, callback: (mountPoint: String?) -> Unit) {
        val mountPoints = ArrayList<String>()
        val cmd = "mount"
        val command = object : Command(0, cmd) {
            override fun commandOutput(id: Int, line: String) {
                mountPoints.add(line)
                super.commandOutput(id, line)
            }

            override fun commandCompleted(id: Int, exitcode: Int) {
                var mountPoint = ""
                var types: String? = null
                for (line in mountPoints) {
                    val words = line.split(" ").filter { it.isNotEmpty() }

                    if (path.contains(words[2])) {
                        if (words[2].length > mountPoint.length) {
                            mountPoint = words[2]
                            types = words[5]
                        }
                    }
                }

                if (mountPoint.isNotEmpty() && types != null) {
                    if (types.contains("rw")) {
                        callback(null)
                    } else if (types.contains("ro")) {
                        val mountCommand = "mount -o rw,remount $mountPoint"
                        mountAsRW(mountCommand) {
                            callback(it)
                        }
                    }
                }

                super.commandCompleted(id, exitcode)
            }
        }

        runCommand(command)
    }

    private fun mountAsRW(cmd: String, callback: (mountPoint: String) -> Unit) {
        val command = object : Command(0, cmd) {
            override fun commandOutput(id: Int, line: String) {
                callback(line)
                super.commandOutput(id, line)
            }
        }

        runCommand(command)
    }

    fun deleteFiles(fileDirItems: ArrayList<FileDirItem>) {
        if (!RootTools.isRootAvailable()) {
            activity.toast(R.string.rooted_device_only)
            return
        }

        tryMountAsRW(fileDirItems.first().path) {
            fileDirItems.forEach {
                val targetPath = it.path.trim('/')
                if (targetPath.isEmpty()) {
                    return@forEach
                }

                val mainCommand = if (it.isDirectory) "rm -rf" else "rm"
                val cmd = "$mainCommand \"/$targetPath\""
                val command = object : Command(0, cmd) {}

                runCommand(command)
            }
        }
    }

    fun copyMoveFiles(fileDirItems: ArrayList<FileDirItem>, destination: String, isCopyOperation: Boolean, successes: Int = 0, callback: (Int) -> Unit) {
        if (!RootTools.isRootAvailable()) {
            activity.toast(R.string.rooted_device_only)
            return
        }

        val fileDirItem = fileDirItems.first()
        val mainCommand = if (isCopyOperation) {
            if (fileDirItem.isDirectory) "cp -R" else "cp"
        } else {
            "mv"
        }

        val cmd = "$mainCommand \"${fileDirItem.path}\" \"$destination\""
        val command = object : Command(0, cmd) {
            override fun commandCompleted(id: Int, exitcode: Int) {
                val newSuccesses = successes + (if (exitcode == 0) 1 else 0)
                if (fileDirItems.size == 1) {
                    callback(newSuccesses)
                } else {
                    fileDirItems.removeAt(0)
                    copyMoveFiles(fileDirItems, destination, isCopyOperation, newSuccesses, callback)
                }
                super.commandCompleted(id, exitcode)
            }
        }
        runCommand(command)
    }
}
