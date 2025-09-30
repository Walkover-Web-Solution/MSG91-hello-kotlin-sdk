package com.msg91.hellosdk.webview

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import com.msg91.hellosdk.utils.LogUtil
import org.json.JSONObject

class WebAppInterface(
    private val context: Context,
    private val onReloadWebview: () -> Unit,
    private val onClose: () -> Unit
) {
    
    @JavascriptInterface
    fun postMessage(jsonMessage: String) {
        val data = JSONObject(jsonMessage)
        val type = data.optString("type")


        when (type) {
            "reload" -> {
                onReloadWebview()
            }
            "close" -> {
//                onClose()
            }
            "uuid" -> {
                val uuid = data.optString("uuid")
                // Log or register UUID for co-browsing
                LogUtil.log("[postMessage]: [UUID]: $jsonMessage")
            }
            "downloadAttachment" -> {
//                Toast.makeText(context, "Download se", Toast.LENGTH_SHORT).show()
                val downloadUrl = data.optString("url")
                if (downloadUrl.isNotBlank()) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            else -> {
                LogUtil.log("[postMessage]: [Unhandled Event]: $type")
            }
        }
    }
}
