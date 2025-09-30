package com.msg91.hellosdk.webview

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
//import android.widget.Toast
import com.msg91.hellosdk.utils.LogUtil

class CustomWebViewClient(
    private val context: Context
) : WebViewClient() {
    
    // Modern method (API 24+)
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        LogUtil.log("🌐 [WebViewClient] Modern shouldOverrideUrlLoading called: $url")
//        Toast.makeText(context, "🌐 Modern URL: $url", Toast.LENGTH_SHORT).show()
        return handleUrlLoading(url)
    }
    
    // Legacy method (older Android versions)
//    @Suppress("DEPRECATION")
//    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
//        LogUtil.log("🌐 [WebViewClient] Legacy shouldOverrideUrlLoading called: $url")
//        Toast.makeText(context, "🌐 Legacy URL: $url", Toast.LENGTH_SHORT).show()
//        return handleUrlLoading(url ?: "")
//    }
    
    private fun handleUrlLoading(url: String): Boolean {
        LogUtil.log("🎯 [WebViewClient] Processing URL: $url")
        
        return if (
            url.startsWith("https://blacksea.msg91.com") ||
            url.startsWith("https://ctest.msg91.com") ||
            url.startsWith("about:blank") ||
            url.isBlank()
        ) {
            LogUtil.log("✅ [WebViewClient] Allowing WebView to handle: $url")
            false  // Let WebView handle it
        } else {
            LogUtil.log("🔗 [WebViewClient] Opening in external browser: $url")
//            Toast.makeText(context, "Opening: $url", Toast.LENGTH_SHORT).show()
            
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true  // We handled it
            } catch (e: Exception) {
                LogUtil.log("❌ [WebViewClient] Error opening URL: ${e.message}")
                false  // Let WebView try to handle it
            }
        }
    }
    
    override fun onPageFinished(view: WebView?, url: String?) {
        LogUtil.log("[WebViewClient] Page finished loading: $url")
        super.onPageFinished(view, url)
    }
    
    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: android.webkit.WebResourceError?) {
        LogUtil.log("[WebViewClient] Error loading: ${error?.description}")
        super.onReceivedError(view, request, error)
    }
}
