package com.msg91.hellosdk.upload

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import com.msg91.hellosdk.utils.LogUtil

/**
 * Self-contained file upload manager that handles file selection
 * using the modern Activity Result API without requiring consumer setup
 */
class FileUploadManager(
    private val context: Context,
    private val fragment: Fragment? = null,
    private val preRegisteredLauncher: ActivityResultLauncher<Intent>? = null
) {
    
    private var currentFileCallback: ValueCallback<Array<Uri>>? = null
    private var fileChooserLauncher: ActivityResultLauncher<Intent>? = null
    private var registrationAttempted = false
    
    companion object {
        private const val TAG = "FileUploadManager"
    }
    
    init {
        LogUtil.log("[$TAG] Initializing FileUploadManager")
        LogUtil.log("[$TAG] Fragment provided: ${fragment != null}")
        LogUtil.log("[$TAG] Pre-registered launcher provided: ${preRegisteredLauncher != null}")
        LogUtil.log("[$TAG] Context type: ${context.javaClass.simpleName}")
        
        // Use pre-registered launcher if available, otherwise try to register
        if (preRegisteredLauncher != null) {
            fileChooserLauncher = preRegisteredLauncher
            LogUtil.log("[$TAG] Using pre-registered ActivityResultLauncher")
        } else {
            LogUtil.log("[$TAG] No pre-registered launcher, attempting registration")
            tryRegisterLauncher()
        }
    }
    
    /**
     * Try to register ActivityResultLauncher - safe to call multiple times
     */
    private fun tryRegisterLauncher(): Boolean {
        if (fileChooserLauncher != null) {
            LogUtil.log("[$TAG] Launcher already registered")
            return true
        }
        
        if (registrationAttempted) {
            LogUtil.log("[$TAG] Registration already attempted, skipping retry")
            return false
        }
        
        registrationAttempted = true
        
        try {
            when {
                fragment != null -> {
                    LogUtil.log("[$TAG] Attempting to register launcher with fragment")
                    LogUtil.log("[$TAG] Fragment lifecycle state: ${fragment.lifecycle.currentState}")
                    LogUtil.log("[$TAG] Fragment is added: ${fragment.isAdded}")
                    LogUtil.log("[$TAG] Fragment activity: ${fragment.activity?.javaClass?.simpleName}")
                    
                    // Check if fragment is in a proper state for registration
                    if (fragment.isAdded && fragment.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                        // Use fragment's ActivityResultLauncher - this is the preferred method
                        fileChooserLauncher = fragment.registerForActivityResult(
                            ActivityResultContracts.StartActivityForResult()
                        ) { result ->
                            LogUtil.log("[$TAG] Fragment launcher callback received: ${result.resultCode}")
                            handleFileSelectionResult(result.resultCode, result.data)
                        }
                        LogUtil.log("[$TAG] File chooser launcher registered with fragment successfully")
                        return true
                    } else {
                        LogUtil.log("[$TAG] Fragment not ready for registration, trying activity fallback")
                        return tryRegisterWithActivity()
                    }
                }
                else -> {
                    LogUtil.log("[$TAG] No fragment available, attempting activity registration")
                    return tryRegisterWithActivity()
                }
            }
        } catch (e: Exception) {
            LogUtil.log("[$TAG] Error setting up file chooser launcher: ${e.message}")
            e.printStackTrace()
            fileChooserLauncher = null
            return false
        }
    }
    
    /**
     * Try to register with activity context
     */
    private fun tryRegisterWithActivity(): Boolean {
        val activity = getActivityFromContext()
        if (activity != null) {
            try {
                LogUtil.log("[$TAG] Found activity: ${activity.javaClass.simpleName}")
                
                fileChooserLauncher = activity.registerForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    LogUtil.log("[$TAG] Activity launcher callback received: ${result.resultCode}")
                    handleFileSelectionResult(result.resultCode, result.data)
                }
                LogUtil.log("[$TAG] File chooser launcher registered with activity successfully")
                return true
            } catch (e: Exception) {
                LogUtil.log("[$TAG] Failed to register with activity: ${e.message}")
                e.printStackTrace()
                return false
            }
        } else {
            LogUtil.log("[$TAG] Warning: Could not get Activity from context")
            return false
        }
    }
    
    /**
     * Retry launcher registration - useful when fragment becomes ready later
     */
    private fun retryRegistration(): Boolean {
        registrationAttempted = false
        return tryRegisterLauncher()
    }
    
    /**
     * Handle file chooser request from WebView
     */
    fun handleFileChooser(
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: android.webkit.WebChromeClient.FileChooserParams?
    ): Boolean {
        LogUtil.log("[$TAG] ===== File chooser requested =====")
        LogUtil.log("[$TAG] Fragment available: ${fragment != null}")
        LogUtil.log("[$TAG] File chooser launcher: ${fileChooserLauncher != null}")
        
        // Try to register launcher if not available yet
        if (fileChooserLauncher == null) {
            LogUtil.log("[$TAG] No launcher available, attempting registration...")
            val registered = retryRegistration()
            if (!registered) {
                LogUtil.log("[$TAG] Registration failed, attempting fallback approach...")
                return attemptFallbackFileChooser(filePathCallback, fileChooserParams)
            }
        }
        
        // Double-check we have a launcher now
        if (fileChooserLauncher == null) {
            LogUtil.log("[$TAG] ERROR: Still no file chooser launcher after retry!")
            return attemptFallbackFileChooser(filePathCallback, fileChooserParams)
        }
        
        try {
            // Store the callback for later use
            currentFileCallback = filePathCallback
            
            // Create intent for file selection
            val intent = fileChooserParams?.createIntent() ?: createDefaultFileIntent()
            
            LogUtil.log("[$TAG] Intent created: ${intent.action}")
            LogUtil.log("[$TAG] Intent type: ${intent.type}")
            LogUtil.log("[$TAG] Intent extras: ${intent.extras?.keySet()?.joinToString()}")
            
            // Launch file chooser
            fileChooserLauncher?.launch(intent)
            LogUtil.log("[$TAG] File chooser launched successfully")
            
            return true
            
        } catch (e: Exception) {
            LogUtil.log("[$TAG] Error in handleFileChooser: ${e.message}")
            e.printStackTrace()
            
            // Try fallback on error
            LogUtil.log("[$TAG] Attempting fallback due to launch error...")
            return attemptFallbackFileChooser(filePathCallback, fileChooserParams)
        }
    }
    
    /**
     * Handle the result from file selection
     */
    private fun handleFileSelectionResult(resultCode: Int, data: Intent?) {
        LogUtil.log("[$TAG] File selection result: $resultCode")
        
        try {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    val uris = getSelectedFileUris(data)
                    LogUtil.log("[$TAG] Selected files: ${uris?.size ?: 0}")
                    currentFileCallback?.onReceiveValue(uris)
                }
                Activity.RESULT_CANCELED -> {
                    LogUtil.log("[$TAG] File selection cancelled")
                    currentFileCallback?.onReceiveValue(null)
                }
                else -> {
                    LogUtil.log("[$TAG] Unknown result code: $resultCode")
                    currentFileCallback?.onReceiveValue(null)
                }
            }
        } catch (e: Exception) {
            LogUtil.log("[$TAG] Error handling file selection result: ${e.message}")
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
                LogUtil.log("[$TAG] Multiple files selected: ${uris.size}")
                uris
            }
            data?.data != null -> {
                // Single file selected
                val uri = data.data!!
                LogUtil.log("[$TAG] Single file selected: $uri")
                arrayOf(uri)
            }
            else -> {
                LogUtil.log("[$TAG] No files selected")
                null
            }
        }
    }
    
    /**
     * Create a default file selection intent if the WebView doesn't provide one
     */
    private fun createDefaultFileIntent(): Intent {
        return Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
    }
    
    /**
     * Get Activity from Context safely
     */
    private fun getActivityFromContext(): ComponentActivity? {
        return when (context) {
            is ComponentActivity -> {
                LogUtil.log("[$TAG] Found ComponentActivity directly: ${context.javaClass.simpleName}")
                context
            }
            else -> {
                LogUtil.log("[$TAG] Searching for ComponentActivity in context chain")
                // Try to find the activity through context chain
                var currentContext = context
                while (currentContext is android.content.ContextWrapper) {
                    val baseContext = currentContext.baseContext
                    if (baseContext is ComponentActivity) {
                        LogUtil.log("[$TAG] Found ComponentActivity in context chain: ${baseContext.javaClass.simpleName}")
                        return baseContext
                    }
                    currentContext = baseContext
                }
                LogUtil.log("[$TAG] No ComponentActivity found in context chain")
                null
            }
        }
    }
    
    /**
     * Fallback file chooser that creates a temporary fragment to handle the result
     * This is used when Activity Result API registration fails in the main context
     */
    private fun attemptFallbackFileChooser(
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: android.webkit.WebChromeClient.FileChooserParams?
    ): Boolean {
        try {
            val activity = getActivityFromContext()
            if (activity is FragmentActivity) {
                LogUtil.log("[$TAG] Using fallback approach with temporary fragment")
                
                // Create a temporary fragment to handle file selection
                val tempFragment = TempFilePickerFragment { result ->
                    LogUtil.log("[$TAG] Fallback file selection result received: $result")
                    val uris = result?.let { arrayOf(it) }
                    filePathCallback?.onReceiveValue(uris)
                }
                
                // Add the temporary fragment
                try {
                    activity.supportFragmentManager
                        .beginTransaction()
                        .add(tempFragment, "temp_file_picker_${System.currentTimeMillis()}")
                        .commitAllowingStateLoss()
                    
                    LogUtil.log("[$TAG] Temporary fragment added for fallback file selection")
                    return true
                } catch (e: Exception) {
                    LogUtil.log("[$TAG] Failed to add temporary fragment: ${e.message}")
                    e.printStackTrace()
                }
            } else {
                LogUtil.log("[$TAG] Activity is not FragmentActivity, cannot use fragment fallback")
                
                // Last resort: direct activity launch (results won't be handled)
                try {
                    val intent = fileChooserParams?.createIntent() ?: createDefaultFileIntent()
                    activity?.startActivity(intent)
                    LogUtil.log("[$TAG] Direct activity launched as last resort")
                    
                    // Since we can't handle the result, notify that no files were selected
                    // This prevents the WebView from hanging
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        LogUtil.log("[$TAG] Timeout for direct activity result, clearing callback")
                        filePathCallback?.onReceiveValue(null)
                    }, 1000) // 1 second timeout
                    
                    return true
                } catch (e: Exception) {
                    LogUtil.log("[$TAG] Failed to start direct activity: ${e.message}")
                }
            }
        } catch (e: Exception) {
            LogUtil.log("[$TAG] Error in fallback file chooser: ${e.message}")
            e.printStackTrace()
        }
        
        // If all else fails, return false to let WebView handle it
        LogUtil.log("[$TAG] All file chooser approaches failed, clearing callback")
        filePathCallback?.onReceiveValue(null)
        return false
    }
    
    /**
     * Temporary fragment for fallback file selection
     */
    private class TempFilePickerFragment(
        private val onResult: (Uri?) -> Unit
    ) : Fragment() {
        
        private lateinit var filePickerLauncher: ActivityResultLauncher<String>
        
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            
            // Register the launcher
            filePickerLauncher = registerForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri ->
                LogUtil.log("[TempFilePickerFragment] File selected: $uri")
                onResult(uri)
                
                // Remove this fragment
                parentFragmentManager.beginTransaction()
                    .remove(this)
                    .commitAllowingStateLoss()
            }
            
            // Launch file picker immediately
            try {
                filePickerLauncher.launch("*/*")
                LogUtil.log("[TempFilePickerFragment] File picker launched")
            } catch (e: Exception) {
                LogUtil.log("[TempFilePickerFragment] Error launching file picker: ${e.message}")
                onResult(null)
                parentFragmentManager.beginTransaction()
                    .remove(this)
                    .commitAllowingStateLoss()
            }
        }
        
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            // No UI needed - this is a headless fragment
            return null
        }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        currentFileCallback?.onReceiveValue(null)
        currentFileCallback = null
        fileChooserLauncher = null
        registrationAttempted = false // Allow re-registration after cleanup
        LogUtil.log("[$TAG] File upload manager cleaned up")
    }
}
