package org.fossify.filemanager

import com.github.ajalt.reprint.core.Reprint
import org.fossify.commons.FossifyApp
import org.fossify.filemanager.dependencies.AppComposition

class App : FossifyApp() {
    lateinit var appComposition: AppComposition
    override val isAppLockFeatureAvailable = true
    override fun onCreate() {
        super.onCreate()
        Reprint.initialize(this)
        appComposition = AppComposition(this)
    }
}
