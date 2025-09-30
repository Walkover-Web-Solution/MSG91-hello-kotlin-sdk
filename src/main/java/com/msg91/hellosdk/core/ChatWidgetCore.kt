package com.msg91.hellosdk.core

import android.content.Context
import android.content.Intent
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.msg91.hellosdk.config.HelloConfig
import com.msg91.hellosdk.core.interfaces.ChatWidgetLifecycle
import com.msg91.hellosdk.core.interfaces.ChatWidgetRenderer
import com.msg91.hellosdk.core.interfaces.ConfigurationManager
import com.msg91.hellosdk.core.interfaces.HtmlContentProvider
import com.msg91.hellosdk.service.ChatHtmlContentProvider
import com.msg91.hellosdk.utils.LogUtil
import com.msg91.hellosdk.webview.ChatWebViewManager

/**
 * Core implementation of the ChatWidget functionality.
 * 
 * This class follows SOLID principles:
 * - Single Responsibility: Manages chat widget core functionality
 * - Open/Closed: Extensible through interfaces
 * - Liskov Substitution: Implements well-defined interfaces
 * - Interface Segregation: Implements focused interfaces
 * - Dependency Inversion: Depends on abstractions (interfaces)
 */
class ChatWidgetCore(
    private val context: Context,
    private val fragment: Fragment? = null,
    private val filePickerLauncher: ActivityResultLauncher<Intent>? = null,
    private val htmlContentProvider: HtmlContentProvider = ChatHtmlContentProvider(),
    private val onClose: (() -> Unit)? = null
) : ChatWidgetRenderer, ChatWidgetLifecycle, ConfigurationManager {
    
    companion object {
        private const val TAG = "ChatWidgetCore"
    }
    
    private var currentConfig: HelloConfig? = null
    private var webViewManager: ChatWebViewManager? = null
    private var container: FrameLayout? = null
    private var isInitialized = false
    
    // UI Configuration
    private var widgetColor: String? = null
    private var isCloseButtonVisible: Boolean = true
    private var useKeyboardAvoidingView: Boolean = true
    
    // ChatWidgetLifecycle implementation
    override fun initialize() {
        if (isInitialized) {
            return
        }

        try {
            // Initialize WebView manager with proper dependencies
            webViewManager = ChatWebViewManager(
                context = context,
                fragment = fragment,
                filePickerLauncher = filePickerLauncher,
                onReload = { reloadWidget() },
                onClose = { handleClose() }
            )
            
            isInitialized = true
        } catch (e: Exception) {
            LogUtil.log("[$TAG] Error during initialization: ${e.message}")
            throw e
        }
    }
    
    override fun destroy() {
        try {
            webViewManager?.cleanup()
            webViewManager = null
            container = null
            currentConfig = null
            isInitialized = false
            
        } catch (e: Exception) {
            LogUtil.log("[$TAG] Error during destruction: ${e.message}")
        }
    }
    
    override fun pause() {
        LogUtil.log("[$TAG] Pausing ChatWidgetCore")
        // WebView automatically handles pause/resume with activity lifecycle
        // Additional pause logic can be added here if needed
    }
    
    override fun resume() {
        LogUtil.log("[$TAG] Resuming ChatWidgetCore")
        // WebView automatically handles pause/resume with activity lifecycle
        // Additional resume logic can be added here if needed
    }
    
    // ConfigurationManager implementation
    override fun setConfiguration(config: HelloConfig) {
        LogUtil.log("[$TAG] Setting configuration: ${config.toMap()}")
        
        if (!validateConfiguration(config)) {
            throw IllegalArgumentException("Invalid configuration provided")
        }
        
        currentConfig = config
    }
    
    override fun getCurrentConfiguration(): HelloConfig? {
        return currentConfig
    }
    
    override fun updateConfiguration(config: HelloConfig) {
        LogUtil.log("[$TAG] Updating configuration: ${config.toMap()}")
        
        if (!validateConfiguration(config)) {
            throw IllegalArgumentException("Invalid configuration provided")
        }
        
        currentConfig = config
        
        // Reload widget with new configuration if already loaded
        if (isWidgetReady()) {
            loadWidget(config)
        }
    }
    
    override fun validateConfiguration(config: HelloConfig): Boolean {
        return try {
            // HelloConfig constructor already validates widgetToken
            // Additional validation can be added here if needed
            config.hasProperty("widgetToken")
        } catch (e: Exception) {
            LogUtil.log("[$TAG] Configuration validation failed: ${e.message}")
            false
        }
    }
    
    // ChatWidgetRenderer implementation
    override fun loadWidget(config: HelloConfig) {
        LogUtil.log("[$TAG] Loading widget with config: ${config.toMap()}")
        
        if (!isInitialized) {
            throw IllegalStateException("ChatWidgetCore must be initialized before loading widget")
        }
        
        setConfiguration(config)
        
        val webManager = webViewManager
            ?: throw IllegalStateException("WebViewManager not available")
        
        try {
            val html = htmlContentProvider.generateHtmlContent(
                config = config,
                widgetColor = widgetColor,
                isCloseButtonVisible = isCloseButtonVisible
            )
            
            webManager.loadHtmlContent(html)
            LogUtil.log("[$TAG] Widget loaded successfully")
            
        } catch (e: Exception) {
            LogUtil.log("[$TAG] Error loading widget: ${e.message}")
            throw e
        }
    }
    
    override fun reloadWidget() {
        LogUtil.log("[$TAG] Reloading widget")
        
        val config = currentConfig
        if (config != null && isWidgetReady()) {
            loadWidget(config)
        } else {
            LogUtil.log("[$TAG] Cannot reload: no configuration or widget not ready")
        }
    }
    
    override fun isWidgetReady(): Boolean {
        return isInitialized && webViewManager != null && currentConfig != null
    }
    
    // Additional methods for UI configuration
    /**
     * Set the widget color theme.
     */
    fun setWidgetColor(color: String?) {
        this.widgetColor = color
    }
    
    /**
     * Set whether the close button should be visible.
     */
    fun setCloseButtonVisible(visible: Boolean) {
        this.isCloseButtonVisible = visible
    }
    
    /**
     * Set whether to use keyboard avoiding view.
     */
    fun setKeyboardAvoidingView(enabled: Boolean) {
        this.useKeyboardAvoidingView = enabled
    }
    
    /**
     * Get the WebView container for embedding in UI.
     */
    fun getContainer(): FrameLayout? {
        return webViewManager?.getContainer()
    }
    
    /**
     * Handle file picker result from Activity Result API.
     */
    fun handleFilePickerResult(resultCode: Int, data: Intent?) {
        webViewManager?.handleFilePickerResult(resultCode, data)
    }
    
    /**
     * Evaluate JavaScript in the WebView.
     */
    fun evaluateJavascript(script: String, resultCallback: ((String?) -> Unit)? = null) {
        webViewManager?.evaluateJavascript(script, resultCallback)
    }
    
    /**
     * Handle close action.
     */
    private fun handleClose() {
        LogUtil.log("[$TAG] Close action triggered")
        onClose?.invoke()
    }
}
