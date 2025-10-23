package com.msg91.hellosdk.service

import com.msg91.hellosdk.config.HelloConfig
import com.msg91.hellosdk.core.interfaces.HtmlContentProvider
import org.json.JSONObject

/**
 * Implementation of HtmlContentProvider that generates HTML content for the chat widget.
 * 
 * This class follows Single Responsibility Principle by focusing solely on HTML generation
 * and Dependency Inversion Principle by implementing the HtmlContentProvider interface.
 */
class ChatHtmlContentProvider : HtmlContentProvider {
    
    companion object {
        private const val SCRIPT_URL = "https://blacksea.msg91.com/chat-widget.js"
    }
    
    override fun generateHtmlContent(
        config: HelloConfig,
        widgetColor: String?,
        isCloseButtonVisible: Boolean
    ): String {
        val sdkConfig = createSdkConfig(widgetColor)
        val finalConfig = buildFinalConfig(config, sdkConfig, isCloseButtonVisible)
        val configJson = JSONObject(finalConfig).toString()
        
        return buildHtmlTemplate(configJson, isCloseButtonVisible)
    }
    
    /**
     * Create SDK-specific configuration.
     * Follows Single Responsibility by separating SDK config creation.
     */
    private fun createSdkConfig(widgetColor: String?): Map<String, Any> {
        val sdkConfig = mutableMapOf<String, Any>(
            "callBackWithoutClose" to true,
            "borderRadiusDisable" to true
        )
        
        // Only add widgetColor if it is provided
        if (!widgetColor.isNullOrBlank()) {
            sdkConfig["customTheme"] = widgetColor
        }
        
        return sdkConfig
    }
    
    /**
     * Build the final configuration by merging user config with SDK config.
     * Follows Single Responsibility by separating config merging logic.
     */
    private fun buildFinalConfig(
        userConfig: HelloConfig,
        sdkConfig: Map<String, Any>,
        isCloseButtonVisible: Boolean
    ): Map<String, Any> {
        return userConfig.toMap() + mapOf(
            "sdkConfig" to sdkConfig,
            "isMobileSDK" to true,
            "show_close_button" to isCloseButtonVisible
        )
    }
    
    /**
     * Build the complete HTML template.
     * Follows Single Responsibility by separating HTML template building.
     */
    private fun buildHtmlTemplate(configJson: String, isCloseButtonVisible: Boolean): String {
        val closeButtonHtml = if (isCloseButtonVisible) {
            """<button class="close-button" onclick="closePage()" title="Close"></button>"""
        } else ""
        
        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
            <title>Hello Chat Widget SDK</title>
            ${getCssStyles()}
            ${getJavaScript(configJson)}
        </head>
        <body onload="openChatIfReady()">
            $closeButtonHtml
            ${getDebugTestLinks()}
            ${getLoaderContainer()}
            <script src="$SCRIPT_URL" onload="handleScriptLoad()" onerror="handleScriptError()"></script>
        </body>
        </html>
        """.trimIndent()
    }
    
    /**
     * Get CSS styles for the widget.
     * Follows Single Responsibility by separating CSS generation.
     */
    private fun getCssStyles(): String {
        return """
            <style>
                body { margin: 0; padding: 0; background-color: #f8f9fa; height: 100vh; display: flex; flex-direction: column; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
                .close-button { position: absolute; top: 16px; right: 16px; width: 32px; height: 32px; background-color: rgba(0, 0, 0, 0.1); border: none; border-radius: 50%; cursor: pointer; display: flex; align-items: center; justify-content: center; font-size: 18px; color: #666; z-index: 1; transition: all 0.2s ease; }
                .close-button:hover { background-color: rgba(0, 0, 0, 0.2); color: #333; transform: scale(1.1); }
                .close-button::before { content: '×'; font-weight: bold; line-height: 1; }
                .loader-container { flex: 1; display: flex; flex-direction: column; justify-content: center; align-items: center; }
                .spinner { display: flex; gap: 8px; margin-bottom: 16px; }
                .spinner div { width: 12px; height: 12px; background-color: #007bff; border-radius: 50%; animation: bounce 1.4s ease-in-out infinite both; }
                .spinner div:nth-child(1) { animation-delay: -0.32s; }
                .spinner div:nth-child(2) { animation-delay: -0.16s; }
                @keyframes bounce { 0%, 80%, 100% { transform: scale(0); } 40% { transform: scale(1); } }
                .loader-text, .error-message, .reload-button { font-size: 14px; text-align: center; }
                .reload-button { background-color: #007bff; color: white; border: none; padding: 12px 24px; border-radius: 6px; font-size: 14px; cursor: pointer; margin-top: 16px; display: none; }
                .reload-button:hover { background-color: #0056b3; }
            </style>
        """
    }
    
    /**
     * Get JavaScript code for the widget.
     * Follows Single Responsibility by separating JavaScript generation.
     */
    private fun getJavaScript(configJson: String): String {
        return """
            <script>
                var helloConfig = $configJson;
                var scriptLoaded = false;
                var scriptError = false;

                function hideLoader() {
                    const loader = document.getElementById('loader');
                    if (loader) {
                        loader.classList.add('hidden');
                        setTimeout(() => { loader.style.display = 'none'; }, 300);
                    }
                }

                function showError() {
                    const spinner = document.querySelector('.spinner');
                    const loaderText = document.querySelector('.loader-text');
                    const errorMessage = document.querySelector('.error-message');
                    const reloadButton = document.querySelector('.reload-button');
                    if (spinner) spinner.style.display = 'none';
                    if (loaderText) loaderText.style.display = 'none';
                    if (errorMessage) { errorMessage.style.display = 'block'; errorMessage.textContent = 'Failed to load chat. Please check your connection and try again.'; }
                    if (reloadButton) reloadButton.style.display = 'inline-block';
                }

                function reloadPage() {
                    if (window.ReactNativeWebView) {
                        window.ReactNativeWebView.postMessage(JSON.stringify({ type: 'reload' }));
                    }
                }

                function openChatIfReady() {
                    if (scriptError) return showError();
                    if (typeof chatWidget !== 'undefined' && typeof chatWidget.open === 'function') {
                        chatWidget.open();
                        if (scriptLoaded) hideLoader();
                    } else {
                        setTimeout(() => {
                            if (typeof chatWidget !== 'undefined' && typeof chatWidget.open === 'function') {
                                chatWidget.open(); hideLoader();
                            } else {
                                showError();
                            }
                        }, 2000);
                    }
                }

                function handleScriptLoad() {
                    scriptLoaded = true;
                    try { initChatWidget(helloConfig, 0); } 
                    catch (e) { scriptError = true; showError(); }
                }

                function handleScriptError() {
                    scriptError = true;
                    showError();
                }

                function closePage() {
                    console.log("Calling Android Webview", window?.ReactNativeWebView?.postMessage)
                    if (window.ReactNativeWebView) {
                        window.ReactNativeWebView.postMessage(JSON.stringify({ type: 'close' }));
                    }
                }

                function testInterface() {
                    console.log("Testing interface availability...");
                    console.log("window.ReactNativeWebView:", window.ReactNativeWebView);
                    console.log("postMessage function:", window.ReactNativeWebView?.postMessage);
                    
                    if (window.ReactNativeWebView && window.ReactNativeWebView.postMessage) {
                        console.log("✅ Interface found! Sending test message...");
                        window.ReactNativeWebView.postMessage(JSON.stringify({ 
                            type: 'test', 
                            message: 'Interface test from JavaScript',
                            timestamp: new Date().toISOString()
                        }));
                    } else {
                        console.log("❌ Interface NOT found!");
                        alert("Interface not available!");
                    }
                }

                setTimeout(testInterface, 1000);
            </script>
        """
    }
    
    /**
     * Get debug test links HTML.
     * Follows Single Responsibility by separating debug UI generation.
     */
    private fun getDebugTestLinks(): String {
        return """
            <div style="position: absolute; top: 60px; right: 10px; z-index: 999; background: white; padding: 10px; border: 1px solid #ccc; border-radius: 5px;">
                <h4 style="margin: 0 0 10px 0; font-size: 12px;">Test Links:</h4>
                <a href="https://google.com" style="display: block; font-size: 12px; margin-bottom: 5px;">🔗 Google (External)</a>
                <a href="https://blacksea.msg91.com/test" style="display: block; font-size: 12px; margin-bottom: 5px;">🏠 Blacksea (Internal)</a>
                <a href="javascript:testInterface()" style="display: block; font-size: 12px; margin-bottom: 5px;">🧪 Test Interface</a>
            </div>
        """
    }
    
    /**
     * Get loader container HTML.
     * Follows Single Responsibility by separating loader UI generation.
     */
    private fun getLoaderContainer(): String {
        return """
            <div id="loader" class="loader-container">
                <div class="spinner"><div></div><div></div><div></div></div>
                <div class="loader-text">Loading chat...</div>
                <div class="error-message"></div>
                <button class="reload-button" onclick="reloadPage()">Try Again</button>
            </div>
        """
    }
}
