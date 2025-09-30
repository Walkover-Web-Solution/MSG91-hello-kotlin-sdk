package com.msg91.hellosdk.core.interfaces

import com.msg91.hellosdk.config.HelloConfig

/**
 * Interface for rendering chat widget content.
 * Follows Interface Segregation Principle by focusing only on rendering concerns.
 */
interface ChatWidgetRenderer {
    /**
     * Load and display the chat widget with the given configuration.
     */
    fun loadWidget(config: HelloConfig)
    
    /**
     * Reload the current widget content.
     */
    fun reloadWidget()
    
    /**
     * Check if the widget is currently loaded and ready.
     */
    fun isWidgetReady(): Boolean
}
