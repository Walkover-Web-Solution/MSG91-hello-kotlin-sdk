package com.msg91.hellosdk.webview

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.msg91.hellosdk.utils.LogUtil

class ChatWebViewManager(
    private val context: Context,
    private val fragment: Fragment? = null, // Fragment for proper Activity Result API
    private val filePickerLauncher: ActivityResultLauncher<Intent>? = null, // Pre-registered launcher
    private val onReload: () -> Unit,
    private val onClose: () -> Unit
) {
    
    private val webView = WebView(context)
    private val container = FrameLayout(context)
    private val webChromeClient = CustomWebChromeClient(context, fragment, filePickerLauncher)
    
    companion object {
        private const val JAVASCRIPT_INTERFACE = "ReactNativeWebView"
    }
    
    init {
        setupWebView()
    }
    
    fun getWebView(): WebView = webView
    
    fun getContainer(): FrameLayout = container
    
    fun getWebChromeClient(): CustomWebChromeClient = webChromeClient
    
    /**
     * Internal reload method - can be called from anywhere within ChatWebViewManager
     * or passed to WebAppInterface. Ensures WebView operations run on main thread.
     */
    private fun reloadWebView() {
        try {
            LogUtil.log("[ChatWebViewManager] Reloading WebView content")
            
            // Ensure WebView operations run on main thread
            Handler(Looper.getMainLooper()).post {
                try {
                    // Reload current page
                    webView.reload()
                    
                    // Call external callback to notify parent components
                    onReload()
                    
                    LogUtil.log("[ChatWebViewManager] WebView reload completed successfully")
                } catch (e: Exception) {
                    LogUtil.log("[ChatWebViewManager] Error during WebView reload on main thread: ${e.message}")
                }
            }
            
        } catch (e: Exception) {
            LogUtil.log("[ChatWebViewManager] Error setting up WebView reload: ${e.message}")
        }
    }
    
    /**
     * Public method for programmatic reload from external components
     */
    fun reload() {
        reloadWebView()
    }

    private fun setupWebView() {
        // Enable WebView debugging for console logs
        WebView.setWebContentsDebuggingEnabled(true)
        
        webView.apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, 
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            setBackgroundColor(Color.TRANSPARENT)
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.domStorageEnabled = true
            
            // Enable file upload capabilities
            // settings.setSupportMultipleWindows(true)
            settings.javaScriptCanOpenWindowsAutomatically = true
            
            // Enable mixed content (if needed for file uploads)
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            
            // Ensure URL loading callbacks are triggered
            // settings.loadsImagesAutomatically = true
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            
            // Additional settings for file upload
            settings.mediaPlaybackRequiresUserGesture = false
            settings.setGeolocationEnabled(true)
            settings.cacheMode = WebSettings.LOAD_DEFAULT


            addJavascriptInterface(
                WebAppInterface(context, ::reloadWebView, onClose),
                JAVASCRIPT_INTERFACE
            )
            
            // Handle file chooser for file uploads - completely automatic
            webChromeClient = this@ChatWebViewManager.webChromeClient
            
            // Handle URL loading behavior
            val customWebViewClient = CustomWebViewClient(context)
            webViewClient = customWebViewClient
            LogUtil.log("🔧 [ChatWebViewManager] CustomWebViewClient attached successfully")
        }

        container.apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, 
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            addView(webView)
            setBackgroundColor(Color.TRANSPARENT)
        }
    }

    private fun onReloadWebview() {
        webView.reload();
    }
    
    fun loadHtmlContent(html: String) {
        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }
    
    fun evaluateJavascript(script: String, resultCallback: ((String?) -> Unit)? = null) {
        webView.evaluateJavascript(script, resultCallback)
    }
    
    /**
     * Handle file picker result from the pre-registered ActivityResultLauncher
     */
    fun handleFilePickerResult(resultCode: Int, data: Intent?) {
        webChromeClient.handleFilePickerResult(resultCode, data)
    }
    
    /**
     * Clean up resources when the WebView manager is no longer needed
     */
    fun cleanup() {
        try {
            webChromeClient.cleanup()
            webView.destroy()
            LogUtil.log("[ChatWebViewManager] Cleanup completed")
        } catch (e: Exception) {
            LogUtil.log("[ChatWebViewManager] Error during cleanup: ${e.message}")
        }
    }
}
