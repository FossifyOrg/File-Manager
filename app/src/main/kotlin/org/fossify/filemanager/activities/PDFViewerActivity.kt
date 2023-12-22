package org.fossify.filemanager.activities

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.WindowManager
import android.widget.RelativeLayout
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.shockwave.pdfium.PdfPasswordException
import org.fossify.commons.dialogs.EnterPasswordDialog
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.REAL_FILE_PATH
import org.fossify.commons.helpers.isPiePlus
import org.fossify.filemanager.R
import org.fossify.filemanager.databinding.ActivityPdfViewerBinding
import org.fossify.filemanager.extensions.hideSystemUI
import org.fossify.filemanager.extensions.showSystemUI
import org.fossify.filemanager.helpers.PdfDocumentAdapter

class PDFViewerActivity : SimpleActivity() {
    private val binding by viewBinding(ActivityPdfViewerBinding::inflate)
    private var realFilePath = ""
    private var isFullScreen = false
    private var passwordDialog: EnterPasswordDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        showTransparentTop = true

        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        if (checkAppSideloading()) {
            return
        }

        checkNotchSupport()
        binding.pdfViewerToolbar.apply {
            setTitleTextColor(Color.WHITE)
            overflowIcon = resources.getColoredDrawableWithColor(R.drawable.ic_three_dots_vector, Color.WHITE)
            navigationIcon = resources.getColoredDrawableWithColor(R.drawable.ic_arrow_left_vector, Color.WHITE)
        }

        if (intent.extras?.containsKey(REAL_FILE_PATH) == true) {
            realFilePath = intent.extras?.get(REAL_FILE_PATH)?.toString() ?: ""
            binding.pdfViewerToolbar.title = realFilePath.getFilenameFromPath()
        }

        setupMenu()
        checkIntent()
    }

    override fun onResume() {
        super.onResume()
        window.navigationBarColor = Color.TRANSPARENT
    }

    private fun setupMenu() {
        (binding.pdfViewerAppbar.layoutParams as RelativeLayout.LayoutParams).topMargin = statusBarHeight
        binding.pdfViewerToolbar.menu.apply {
            findItem(R.id.menu_print).isVisible = realFilePath.isNotEmpty()
            findItem(R.id.menu_print).setOnMenuItemClickListener {
                printText()
                true
            }
        }

        binding.pdfViewerToolbar.setNavigationOnClickListener {
            finish()
        }

        if (!portrait && navigationBarOnSide && navigationBarWidth > 0) {
            binding.pdfViewerAppbar.setPadding(0, 0, navigationBarWidth, 0)
        } else {
            binding.pdfViewerAppbar.setPadding(0, 0, 0, 0)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        (binding.pdfViewerAppbar.layoutParams as RelativeLayout.LayoutParams).topMargin = statusBarHeight
        if (!portrait && navigationBarOnSide && navigationBarWidth > 0) {
            binding.pdfViewerAppbar.setPadding(0, 0, navigationBarWidth, 0)
        } else {
            binding.pdfViewerAppbar.setPadding(0, 0, 0, 0)
        }
    }

    private fun checkIntent() {
        val uri = intent.data
        if (uri == null) {
            finish()
            return
        }

        loadPdfViewer(uri)
    }

    private fun loadPdfViewer(uri: Uri, filePassword: String? = null) {
        val primaryColor = getProperPrimaryColor()
        binding.pdfViewer.setBackgroundColor(getProperBackgroundColor())
        binding.pdfViewer.fromUri(uri)
            .password(filePassword)
            .scrollHandle(DefaultScrollHandle(this, primaryColor.getContrastColor(), primaryColor))
            .spacing(15)
            .onTap { toggleFullScreen() }
            .onError {
                if (it is PdfPasswordException) {
                    // already entered a password and it was wrong
                    if (filePassword != null) {
                        toast(getString(R.string.invalid_password))
                        passwordDialog?.clearPassword()
                    } else {
                        passwordDialog = EnterPasswordDialog(
                            this,
                            callback = { password ->
                                loadPdfViewer(uri, password)
                            },
                            cancelCallback = {
                                finish()
                            }
                        )
                    }
                } else {
                    showErrorToast(it.localizedMessage?.toString() ?: getString(R.string.unknown_error_occurred))
                    finish()
                }
            }
            .onLoad {
                passwordDialog?.dismiss(notify = false)
            }
            .load()

        showSystemUI(true)

        val filename = getFilenameFromUri(uri)
        if (filename.isNotEmpty()) {
            binding.pdfViewerToolbar.title = filename
        }
    }

    private fun printText() {
        val adapter = PdfDocumentAdapter(this, realFilePath)

        (getSystemService(Context.PRINT_SERVICE) as? PrintManager)?.apply {
            print(realFilePath.getFilenameFromPath(), adapter, PrintAttributes.Builder().build())
        }
    }

    private fun toggleFullScreen(): Boolean {
        isFullScreen = !isFullScreen
        val newAlpha: Float
        if (isFullScreen) {
            newAlpha = 0f
            hideSystemUI(true)
        } else {
            newAlpha = 1f
            showSystemUI(true)
        }

        binding.topShadow.animate().alpha(newAlpha).start()
        binding.pdfViewerAppbar.animate().alpha(newAlpha).withStartAction {
            if (newAlpha == 1f) {
                binding.pdfViewerAppbar.beVisible()
            }
        }.withEndAction {
            if (newAlpha == 0f) {
                binding.pdfViewerAppbar.beGone()
            }
        }.start()

        // return false to also toggle scroll handle
        return true
    }

    private fun checkNotchSupport() {
        if (isPiePlus()) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        }
    }
}
