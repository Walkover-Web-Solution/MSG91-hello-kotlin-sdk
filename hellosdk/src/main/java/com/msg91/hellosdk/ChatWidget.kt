package com.msg91.hellosdk

import android.content.Context
import androidx.fragment.app.Fragment
import com.msg91.hellosdk.config.HelloConfig
import com.msg91.hellosdk.core.ChatWidgetCore
import com.msg91.hellosdk.utils.LogUtil

/**
 * Main entry point for the HelloChatWidget SDK.
 *
 * Usage:
 * ```kotlin
 * val config = HelloConfig("your-widget-token")
 *     .withProperty("mail", "user@example.com")
 * 
 * ChatWidget.initialize(config)
 * 
 * // Later, to update configuration:
 * val updatedConfig = config.withProperty("mail", "new@example.com")
 * ChatWidget.update(updatedConfig)
 * ```
 */
object ChatWidget {
    
    private const val TAG = "ChatWidget"
    
    // Internal reference to the core implementation
    // This follows Dependency Inversion Principle by depending on abstraction
    private var core: ChatWidgetCore? = null
    private var isInitialized = false
    
    // Keep track of active fragment instances to notify them of config updates
    private val activeFragments = mutableSetOf<ChatWidgetFragment>()
    
    /**
     * Initialize the ChatWidget SDK with the provided configuration.
     * 
     * This method must be called before any other widget operations.
     * It sets up the widget with the provided HelloConfig and prepares
     * it for use within a Fragment or Activity context.
     * 
     * @param helloConfig Configuration containing widgetToken and optional properties
     * @throws IllegalArgumentException if configuration is invalid
     * @throws IllegalStateException if already initialized (call update() instead)
     */
    @JvmStatic
    fun initialize(helloConfig: HelloConfig) {
        if (isInitialized) {
            throw IllegalStateException("ChatWidget is already initialized. Use update() to change configuration.")
        }

        try {
            // Validate configuration before proceeding
            validateConfiguration(helloConfig)
            
            // Store the configuration
            storeConfiguration(helloConfig)

            isInitialized = true
            
        } catch (e: Exception) {
            throw e
        }
    }
    
    /**
     * Update the ChatWidget configuration.
     * 
     * This method allows updating the widget configuration after initialization.
     * It can be used to change user information, widget settings, or any other
     * configuration parameters.
     * 
     * @param helloConfig Updated configuration containing widgetToken and optional properties
     * @throws IllegalArgumentException if configuration is invalid
     * @throws IllegalStateException if not initialized (call initialize() first)
     */
    @JvmStatic
    fun update(helloConfig: HelloConfig) {
        if (!isInitialized) {
            throw IllegalStateException("ChatWidget is not initialized. Call initialize() first.")
        }
        
        LogUtil.log("[$TAG] Updating ChatWidget configuration: ${helloConfig.toMap()}")
        
        try {
            // Validate configuration before proceeding
            validateConfiguration(helloConfig)
            
            // Store the updated configuration
            storeConfiguration(helloConfig)
            
            // Update the core instance if it exists
            core?.updateConfiguration(helloConfig)
            
            // Update all active fragment instances
            val configMap = helloConfig.toMap()
            activeFragments.forEach { fragment ->
                try {
                    fragment.updateHelloConfig(configMap)
                } catch (e: Exception) {
                    LogUtil.log("[$TAG] Error updating fragment configuration: ${e.message}")
                }
            }
            
        } catch (e: Exception) {
            LogUtil.log("[$TAG] Failed to update ChatWidget configuration: ${e.message}")
            throw e
        }
    }
    
    /**
     * Create a ChatWidgetFragment for embedding in your UI.
     * 
     * This method creates a Fragment that can be added to your Activity or Fragment.
     * The fragment will display the chat widget using the current configuration.
     * 
     * @param widgetColor Optional color theme for the widget
     * @param isCloseButtonVisible Whether to show the close button (default: true)
     * @param useKeyboardAvoidingView Whether to use keyboard avoiding behavior (default: true)
     * @return ChatWidgetFragment ready to be added to your UI
     * @throws IllegalStateException if not initialized
     */
    @JvmStatic
    fun createFragment(
        widgetColor: String? = null,
        isCloseButtonVisible: Boolean = true,
        useKeyboardAvoidingView: Boolean = true
    ): Fragment {
        if (!isInitialized) {
            throw IllegalStateException("ChatWidget is not initialized. Call initialize() first.")
        }
        
        // Return the refactored ChatWidgetFragment
        // This maintains backward compatibility while using the new architecture internally
        val config = getStoredConfiguration()
            ?: throw IllegalStateException("No configuration available")
        
        val fragment = ChatWidgetFragment.newInstance(
            helloConfig = config.toMap(),
            widgetColor = widgetColor,
            isCloseButtonVisible = isCloseButtonVisible,
            useKeyboardAvoidingView = useKeyboardAvoidingView
        )
        
        // Register the fragment for future configuration updates
        registerFragment(fragment)
        
        return fragment
    }
    
    /**
     * Get the current configuration.
     * 
     * @return Current HelloConfig or null if not initialized
     */
    @JvmStatic
    fun getCurrentConfiguration(): HelloConfig {
        if (!isInitialized) {
            throw IllegalStateException("ChatWidget is not initialized.")
        }
        
        return getStoredConfiguration()
            ?: throw IllegalStateException("No configuration available")
    }
    
    /**
     * Check if the SDK is initialized.
     * 
     * @return true if initialized, false otherwise
     */
    @JvmStatic
    fun isInitialized(): Boolean {
        return isInitialized
    }
    
    /**
     * Clean up and destroy the ChatWidget SDK.
     * 
     * This method should be called when the SDK is no longer needed,
     * typically in your Application's onTerminate() or when the user logs out.
     */
    @JvmStatic
    fun destroy() {
        LogUtil.log("[$TAG] Destroying ChatWidget SDK")
        
        try {
            core?.destroy()
            core = null
            isInitialized = false
            activeFragments.clear()
            
            LogUtil.log("[$TAG] ChatWidget SDK destroyed successfully")
        } catch (e: Exception) {
            LogUtil.log("[$TAG] Error during ChatWidget SDK destruction: ${e.message}")
        }
    }
    
    /**
     * Register a fragment instance for configuration updates.
     * This is called internally when fragments are created.
     */
    @JvmStatic
    internal fun registerFragment(fragment: ChatWidgetFragment) {
        activeFragments.add(fragment)
        LogUtil.log("[$TAG] Fragment registered, active count: ${activeFragments.size}")
    }
    
    /**
     * Unregister a fragment instance.
     * This should be called when fragments are destroyed.
     */
    @JvmStatic
    internal fun unregisterFragment(fragment: ChatWidgetFragment) {
        activeFragments.remove(fragment)
        LogUtil.log("[$TAG] Fragment unregistered, active count: ${activeFragments.size}")
    }
    
    /**
     * Internal method to get or create the core instance.
     * This follows Dependency Inversion Principle by managing the core dependency.
     */
    internal fun getOrCreateCore(
        context: Context,
        fragment: Fragment? = null,
        filePickerLauncher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>? = null,
        onClose: (() -> Unit)? = null
    ): ChatWidgetCore {
        if (core == null) {
            core = ChatWidgetCore(
                context = context,
                fragment = fragment,
                filePickerLauncher = filePickerLauncher,
                onClose = onClose
            )
            core!!.initialize()
        }
        return core!!
    }
    
    /**
     * Validate the provided configuration.
     * 
     * @param config Configuration to validate
     * @throws IllegalArgumentException if configuration is invalid
     */
    private fun validateConfiguration(config: HelloConfig) {
        // HelloConfig constructor already validates widgetToken
        // Additional SDK-level validation can be added here
        
        if (!config.hasProperty("widgetToken")) {
            throw IllegalArgumentException("Configuration must contain a valid widgetToken")
        }
        
        val widgetToken = config.getProperty("widgetToken") as? String
        if (widgetToken.isNullOrBlank()) {
            throw IllegalArgumentException("widgetToken cannot be null or blank")
        }
    }
    
    // Simple configuration storage for the facade
    // In a production environment, this might use SharedPreferences or a database
    private var currentConfig: HelloConfig? = null
    
    // Store current configuration when initializing or updating
    private fun storeConfiguration(config: HelloConfig) {
        currentConfig = config
    }
    
    // Get stored configuration
    private fun getStoredConfiguration(): HelloConfig? {
        return currentConfig
    }
}
