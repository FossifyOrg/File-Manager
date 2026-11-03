package org.fossify.filemanager

import com.github.ajalt.reprint.core.Reprint
import org.fossify.commons.FossifyApp
import org.fossify.filemanager.dependencies.AppComposition

class App : FossifyApp() {
    val appComposition: AppComposition by lazy {
        AppComposition(this)
    }
    override val isAppLockFeatureAvailable = true
    override fun onCreate() {
        super.onCreate()
        Reprint.initialize(this)
    }
}
