package org.fossify.filemanager.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.inputmethod.EditorInfo
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.TextView
import org.fossify.commons.dialogs.ConfirmationAdvancedDialog
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.REAL_FILE_PATH
import org.fossify.commons.helpers.SAVE_DISCARD_PROMPT_INTERVAL
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.views.MyEditText
import org.fossify.filemanager.R
import org.fossify.filemanager.databinding.ActivityReadTextBinding
import org.fossify.filemanager.dialogs.SaveAsDialog
import org.fossify.filemanager.extensions.openPath
import org.fossify.filemanager.views.GestureEditText
import java.io.File
import java.io.OutputStream

class ReadTextActivity : SimpleActivity() {
    companion object {
        private const val SELECT_SAVE_FILE_INTENT = 1
        private const val SELECT_SAVE_FILE_AND_EXIT_INTENT = 2
    }

    private val binding by viewBinding(ActivityReadTextBinding::inflate)
    private val keyUnsavedText = "KEY_UNSAVED_TEXT"

    private var filePath = ""
    private var originalText = ""
    private var searchIndex = 0
    private var lastSavePromptTS = 0L
    private var searchMatches = emptyList<Int>()
    private var isSearchActive = false

    private lateinit var searchQueryET: MyEditText
    private lateinit var searchPrevBtn: ImageView
    private lateinit var searchNextBtn: ImageView
    private lateinit var searchClearBtn: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupOptionsMenu()
        binding.apply {
            updateMaterialActivityViews(readTextCoordinator, readTextView, useTransparentNavigation = true, useTopSearchMenu = false)
            setupMaterialScrollListener(readTextHolder, readTextToolbar)
        }

        searchQueryET = findViewById(R.id.search_query)
        searchPrevBtn = findViewById(R.id.search_previous)
        searchNextBtn = findViewById(R.id.search_next)
        searchClearBtn = findViewById(R.id.search_clear)

        if (checkAppSideloading()) {
            return
        }

        val uri = if (intent.extras?.containsKey(REAL_FILE_PATH) == true) {
            Uri.fromFile(File(intent.extras?.get(REAL_FILE_PATH).toString()))
        } else {
            intent.data
        }

        if (uri == null) {
            finish()
            return
        }

        val filename = getFilenameFromUri(uri)
        if (filename.isNotEmpty()) {
            binding.readTextToolbar.title = Uri.decode(filename)
        }

        binding.readTextView.onGlobalLayout {
            ensureBackgroundThread {
                checkIntent(uri, savedInstanceState)
            }
        }

        setupSearchButtons()
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.readTextToolbar, NavigationIcon.Arrow)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (originalText != binding.readTextView.text.toString()) {
            outState.putString(keyUnsavedText, binding.readTextView.text.toString())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == SELECT_SAVE_FILE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            val outputStream = contentResolver.openOutputStream(resultData.data!!)

            val shouldExitAfterSaving = requestCode == SELECT_SAVE_FILE_AND_EXIT_INTENT

            val selectedFilePath = getRealPathFromURI(intent.data!!)
            val shouldOverwriteOriginalText = selectedFilePath == filePath

            saveTextContent(outputStream, shouldExitAfterSaving, shouldOverwriteOriginalText)
        }
    }

    override fun onBackPressed() {
        val hasUnsavedChanges = originalText != binding.readTextView.text.toString()
        when {
            isSearchActive -> closeSearch()
            hasUnsavedChanges && System.currentTimeMillis() - lastSavePromptTS > SAVE_DISCARD_PROMPT_INTERVAL -> {
                lastSavePromptTS = System.currentTimeMillis()
                ConfirmationAdvancedDialog(this, "", R.string.save_before_closing, R.string.save, R.string.discard) {
                    if (it) {
                        saveText(true)
                    } else {
                        super.onBackPressed()
                    }
                }
            }

            else -> super.onBackPressed()
        }
    }

    private fun setupOptionsMenu() {
        binding.readTextToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_search -> openSearch()
                R.id.menu_save -> saveText()
                R.id.menu_open_with -> openPath(intent.dataString!!, true)
                R.id.menu_print -> printText()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun openSearch() {
        isSearchActive = true
        binding.searchWrapper.beVisible()
        showKeyboard(searchQueryET)

        binding.readTextView.requestFocus()
        binding.readTextView.setSelection(0)

        searchQueryET.postDelayed({
            searchQueryET.requestFocus()
        }, 250)
    }

    private fun saveText(shouldExitAfterSaving: Boolean = false) {
        if (filePath.isEmpty()) {
            filePath = getRealPathFromURI(intent.data!!) ?: ""
        }

        if (filePath.isEmpty()) {
            SaveAsDialog(this, filePath, true) { _, filename ->
                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TITLE, filename)
                    addCategory(Intent.CATEGORY_OPENABLE)

                    val requestCode = if (shouldExitAfterSaving) {
                        SELECT_SAVE_FILE_AND_EXIT_INTENT
                    } else {
                        SELECT_SAVE_FILE_INTENT
                    }
                    startActivityForResult(this, requestCode)
                }
            }
        } else {
            SaveAsDialog(this, filePath, false) { path, _ ->
                if (hasStoragePermission()) {
                    val file = File(path)
                    getFileOutputStream(file.toFileDirItem(this), true) {
                        val shouldOverwriteOriginalText = path == filePath
                        saveTextContent(it, shouldExitAfterSaving, shouldOverwriteOriginalText)
                    }
                } else {
                    toast(R.string.no_storage_permissions)
                }
            }
        }
    }

    private fun saveTextContent(outputStream: OutputStream?, shouldExitAfterSaving: Boolean, shouldOverwriteOriginalText: Boolean) {
        if (outputStream != null) {
            val currentText = binding.readTextView.text.toString()
            outputStream.bufferedWriter().use { it.write(currentText) }
            toast(R.string.file_saved)
            hideKeyboard()

            if (shouldOverwriteOriginalText) {
                originalText = currentText
            }

            if (shouldExitAfterSaving) {
                super.onBackPressed()
            }
        } else {
            toast(R.string.unknown_error_occurred)
        }
    }

    private fun printText() {
        try {
            val webView = WebView(this)
            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = false

                override fun onPageFinished(view: WebView, url: String) {
                    createWebPrintJob(view)
                }
            }

            webView.loadData(binding.readTextView.text.toString(), "text/plain", "UTF-8")
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }

    private fun createWebPrintJob(webView: WebView) {
        val jobName = if (filePath.isNotEmpty()) {
            filePath.getFilenameFromPath()
        } else {
            getString(R.string.app_name)
        }

        val printAdapter = webView.createPrintDocumentAdapter(jobName)

        (getSystemService(Context.PRINT_SERVICE) as? PrintManager)?.apply {
            print(jobName, printAdapter, PrintAttributes.Builder().build())
        }
    }

    private fun checkIntent(uri: Uri, savedInstanceState: Bundle?) {
        originalText = if (uri.scheme == "file") {
            filePath = uri.path!!
            val file = File(filePath)
            if (file.exists()) {
                try {
                    file.readText()
                } catch (e: Exception) {
                    showErrorToast(e)
                    ""
                }
            } else {
                toast(R.string.unknown_error_occurred)
                ""
            }
        } else {
            try {
                contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
            } catch (e: OutOfMemoryError) {
                showErrorToast(e.toString())
                return
            } catch (e: Exception) {
                showErrorToast(e)
                finish()
                return
            }
        }

        runOnUiThread {
            var textToSet = originalText

            if (savedInstanceState != null) {
                textToSet = savedInstanceState.getString(keyUnsavedText, originalText)
            }

            binding.readTextView.setText(textToSet)
            if (originalText.isNotEmpty()) {
                hideKeyboard()
            } else {
                showKeyboard(binding.readTextView)
            }
        }
    }

    private fun setupSearchButtons() {
        searchQueryET.onTextChangeListener {
            searchTextChanged(it)
        }

        searchPrevBtn.setOnClickListener {
            goToPrevSearchResult()
        }

        searchNextBtn.setOnClickListener {
            goToNextSearchResult()
        }

        searchClearBtn.setOnClickListener {
            closeSearch()
        }

        searchQueryET.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchNextBtn.performClick()
                return@OnEditorActionListener true
            }

            false
        })

        binding.searchWrapper.setBackgroundColor(getProperPrimaryColor())
        val contrastColor = getProperPrimaryColor().getContrastColor()
        arrayListOf(searchPrevBtn, searchNextBtn, searchClearBtn).forEach {
            it.applyColorFilter(contrastColor)
        }
    }

    private fun searchTextChanged(text: String) {
        binding.readTextView.text?.clearBackgroundSpans()

        if (text.isNotBlank() && text.length > 1) {
            searchMatches = binding.readTextView.text.toString().searchMatches(text)
            binding.readTextView.highlightText(text, getProperPrimaryColor())
        }

        if (searchMatches.isNotEmpty()) {
            binding.readTextView.requestFocus()
            binding.readTextView.setSelection(searchMatches.getOrNull(searchIndex) ?: 0)
        }

        searchQueryET.postDelayed({
            searchQueryET.requestFocus()
        }, 50)
    }

    private fun goToPrevSearchResult() {
        if (searchIndex > 0) {
            searchIndex--
        } else {
            searchIndex = searchMatches.lastIndex
        }

        selectSearchMatch(binding.readTextView)
    }

    private fun goToNextSearchResult() {
        if (searchIndex < searchMatches.lastIndex) {
            searchIndex++
        } else {
            searchIndex = 0
        }

        selectSearchMatch(binding.readTextView)
    }

    private fun closeSearch() {
        searchQueryET.text?.clear()
        isSearchActive = false
        binding.searchWrapper.beGone()
        hideKeyboard()
    }

    private fun selectSearchMatch(editText: GestureEditText) {
        if (searchMatches.isNotEmpty()) {
            editText.requestFocus()
            editText.setSelection(searchMatches.getOrNull(searchIndex) ?: 0)
        } else {
            hideKeyboard()
        }
    }
}
