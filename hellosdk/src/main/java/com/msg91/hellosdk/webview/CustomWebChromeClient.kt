package com.msg91.hellosdk.webview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.msg91.hellosdk.utils.LogUtil
import com.msg91.hellosdk.upload.FileUploadManager
import androidx.lifecycle.Lifecycle

class CustomWebChromeClient(
    private val context: Context,
    private val fragment: Fragment? = null,
    private val filePickerLauncher: ActivityResultLauncher<Intent>? = null
) : WebChromeClient() {
    
    companion object {
        private const val TAG = "CustomWebChromeClient"
        private const val FILE_CHOOSER_REQUEST_CODE = 1001
    }
    
    // FileUploadManager with proper initialization timing
    private var fileUploadManager: FileUploadManager? = null
    private var currentFileCallback: ValueCallback<Array<Uri>>? = null
    
    private fun getOrCreateFileUploadManager(): FileUploadManager {
        if (fileUploadManager == null) {
            try {
                fileUploadManager = FileUploadManager(context, fragment, filePickerLauncher)
            } catch (e: Exception) {
                e.printStackTrace()
                // Return a basic instance that will try fallback approaches
                fileUploadManager = FileUploadManager(context, null, null)
            }
        }
        return fileUploadManager!!
    }
    
    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {

        try {
            // Validate that we have essential components
            if (filePathCallback == null) {
                LogUtil.log("[$TAG] ERROR: FilePathCallback is null - cannot proceed")
                return false
            }
            
            // Store the callback for the result handler
            currentFileCallback = filePathCallback
            
            // Try to use pre-registered launcher first
            if (filePickerLauncher != null) {
                try {
                    val intent = fileChooserParams?.createIntent() ?: createDefaultFileIntent()
                    filePickerLauncher.launch(intent)
                    return true
                } catch (e: Exception) {
                    LogUtil.log("[$TAG] Error with pre-registered launcher: ${e.message}")
                }
            }
            
            // Fallback to FileUploadManager
            val manager = getOrCreateFileUploadManager()
            val result = manager.handleFileChooser(filePathCallback, fileChooserParams)
            
            return result
            
        } catch (e: Exception) {
            e.printStackTrace()
            
            // Always call the callback to prevent WebView from hanging
            try {
                filePathCallback?.onReceiveValue(null)
            } catch (callbackError: Exception) {
                LogUtil.log("[$TAG] Error clearing callback: ${callbackError.message}")
            }
            
            return false
        }
    }
    
    /**
     * Handle file picker result from pre-registered ActivityResultLauncher
     */
    fun handleFilePickerResult(resultCode: Int, data: Intent?) {

        try {
            val uris = when (resultCode) {
                Activity.RESULT_OK -> {
                    getSelectedFileUris(data)
                }
                Activity.RESULT_CANCELED -> {
                    LogUtil.log("[$TAG] File selection cancelled")
                    null
                }
                else -> {
                    LogUtil.log("[$TAG] Unknown result code: $resultCode")
                    null
                }
            }
            
            currentFileCallback?.onReceiveValue(uris)
            
        } catch (e: Exception) {
            LogUtil.log("[$TAG] Error handling file picker result: ${e.message}")
            e.printStackTrace()
            currentFileCallback?.onReceiveValue(null)
        } finally {
            currentFileCallback = null
        }
    }
    
    /**
     * Extract selected file URIs from the result intent
     */
    private fun getSelectedFileUris(data: Intent?): Array<Uri>? {
        return when {
            data?.clipData != null -> {
                // Multiple files selected
                val clipData = data.clipData!!
                val uris = Array(clipData.itemCount) { i ->
                    clipData.getItemAt(i).uri
                }
                uris
            }
            data?.data != null -> {
                // Single file selected
                val uri = data.data!!
                arrayOf(uri)
            }
            else -> {
                null
            }
        }
    }
    
    /**
     * Create a default file selection intent
     */
    private fun createDefaultFileIntent(): Intent {
        return Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
    }
    
    // Add other WebChromeClient methods for better debugging
    override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
        try {
            request?.grant(request.resources)
        } catch (e: Exception) {
            request?.deny()
        }
    }
    
    override fun onConsoleMessage(message: android.webkit.ConsoleMessage?): Boolean {
        message?.let { msg ->
            val level = when (msg.messageLevel()) {
                android.webkit.ConsoleMessage.MessageLevel.ERROR -> "ERROR"
                android.webkit.ConsoleMessage.MessageLevel.WARNING -> "WARNING"
                android.webkit.ConsoleMessage.MessageLevel.DEBUG -> "DEBUG"
                else -> "INFO"
            }
        }
        return true
    }
    
    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        if (newProgress == 100) {
            LogUtil.log("[$TAG] Page loading completed")
        }
    }
    
    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        LogUtil.log("[$TAG] Page title received: $title")
    }
    
    /**
     * Clean up resources when the WebChromeClient is no longer needed
     */
    fun cleanup() {
        try {
            fileUploadManager?.cleanup()
            fileUploadManager = null
        } catch (e: Exception) {
            LogUtil.log("[$TAG] Error during cleanup: ${e.message}")
            e.printStackTrace()
        }
    }
}
