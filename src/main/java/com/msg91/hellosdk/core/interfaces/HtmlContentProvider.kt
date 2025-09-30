package com.msg91.hellosdk.core.interfaces

import com.msg91.hellosdk.config.HelloConfig

/**
 * Interface for providing HTML content for the chat widget.
 * Follows Interface Segregation Principle by separating content generation concerns.
 */
interface HtmlContentProvider {
    /**
     * Generate HTML content for the given configuration.
     */
    fun generateHtmlContent(
        config: HelloConfig,
        widgetColor: String? = null,
        isCloseButtonVisible: Boolean = true
    ): String
}
