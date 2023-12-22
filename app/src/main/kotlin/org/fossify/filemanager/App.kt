package org.fossify.filemanager

import android.app.Application
import com.github.ajalt.reprint.core.Reprint
import org.fossify.commons.extensions.checkUseEnglish

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        checkUseEnglish()
        Reprint.initialize(this)
    }
}
