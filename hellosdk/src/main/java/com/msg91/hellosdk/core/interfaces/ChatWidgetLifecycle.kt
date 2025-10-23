package com.msg91.hellosdk.core.interfaces

/**
 * Interface for managing chat widget lifecycle.
 * Follows Interface Segregation Principle by separating lifecycle concerns.
 */
interface ChatWidgetLifecycle {
    /**
     * Initialize the chat widget components.
     */
    fun initialize()
    
    /**
     * Clean up resources and destroy the widget.
     */
    fun destroy()
    
    /**
     * Pause widget operations (e.g., when app goes to background).
     */
    fun pause()
    
    /**
     * Resume widget operations (e.g., when app comes to foreground).
     */
    fun resume()
}
