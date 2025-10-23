package com.msg91.hellosdk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.msg91.hellosdk.config.HelloConfig
import com.msg91.hellosdk.core.ChatWidgetCore
import com.msg91.hellosdk.utils.LogUtil
import androidx.core.graphics.toColorInt

class ChatWidgetFragment : Fragment() {

    companion object {
        private const val ARG_HELLO_CONFIG = "helloConfig"
        private const val ARG_WIDGET_COLOR = "widgetColor"
        private const val ARG_CLOSE_BUTTON_VISIBLE = "isCloseButtonVisible"
        private const val ARG_KEYBOARD_AVOIDING = "useKeyboardAvoidingView"

        fun newInstance(
            helloConfig: Map<String, Any>,
            widgetColor: String? = null,
            isCloseButtonVisible: Boolean = true,
            useKeyboardAvoidingView: Boolean = true
        ): ChatWidgetFragment {
            val fragment = ChatWidgetFragment()
            val args = Bundle().apply {
                putSerializable(ARG_HELLO_CONFIG, HashMap(helloConfig))
                putString(ARG_WIDGET_COLOR, widgetColor)
                putBoolean(ARG_CLOSE_BUTTON_VISIBLE, isCloseButtonVisible)
                putBoolean(ARG_KEYBOARD_AVOIDING, useKeyboardAvoidingView)
            }
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var helloConfig: MutableMap<String, Any>
    private var widgetColor: String? = null
    private var isCloseButtonVisible: Boolean = true
    private var useKeyboardAvoidingView: Boolean = true

    private lateinit var chatWidgetCore: ChatWidgetCore
    private lateinit var container: FrameLayout
    
    // Pre-register ActivityResultLauncher for file uploads
    private val filePickerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Delegate result to ChatWidgetCore when it's ready
        if (::chatWidgetCore.isInitialized) {
            chatWidgetCore.handleFilePickerResult(result.resultCode, result.data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            @Suppress("UNCHECKED_CAST", "DEPRECATION")
            helloConfig = (args.getSerializable(ARG_HELLO_CONFIG) as? HashMap<String, Any>)?.toMutableMap()
                ?: mutableMapOf()
            widgetColor = args.getString(ARG_WIDGET_COLOR)
            isCloseButtonVisible = args.getBoolean(ARG_CLOSE_BUTTON_VISIBLE, true)
            useKeyboardAvoidingView = args.getBoolean(ARG_KEYBOARD_AVOIDING, true)
        }
        
        validateConfig()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Create the main container programmatically
        this.container = FrameLayout(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor("#F5F5F5".toColorInt())
        }
        return this.container
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        LogUtil.log("[ChatWidgetFragment] onViewCreated started")
        LogUtil.log("[ChatWidgetFragment] Config: $helloConfig")
        
        // Initialize ChatWidgetCore with fragment context for proper Activity Result API
        chatWidgetCore = ChatWidgetCore(
            context = requireContext(),
            fragment = this, // Pass fragment for proper file upload handling
            filePickerLauncher = filePickerLauncher, // Pass pre-registered launcher
            onClose = { handleClose() }
        )
        
        // Initialize the core
        chatWidgetCore.initialize()
        
        // Configure the core with UI settings
        chatWidgetCore.setWidgetColor(widgetColor)
        chatWidgetCore.setCloseButtonVisible(isCloseButtonVisible)
        chatWidgetCore.setKeyboardAvoidingView(useKeyboardAvoidingView)
        
        // Add the webview container to our fragment's container
        val coreContainer = chatWidgetCore.getContainer()
        if (coreContainer != null) {
            container.addView(coreContainer)
            LogUtil.log("[ChatWidgetFragment] Core container added")
        }
        
        // Setup keyboard handling if enabled
        if (useKeyboardAvoidingView) {
            setupKeyboardAnimation()
        }
        loadHtmlContent()
        
        LogUtil.log("[ChatWidgetFragment] Fragment initialized successfully")
    }

    private fun validateConfig() {
        if (!helloConfig.containsKey("widgetToken")) {
            throw IllegalArgumentException("Missing 'widgetToken' in helloConfig")
        }
    }

    private fun loadHtmlContent() {
        if (::chatWidgetCore.isInitialized) {
            try {
                val config = HelloConfig.fromMap(helloConfig)
                chatWidgetCore.loadWidget(config)
                LogUtil.log("[ChatWidgetFragment] Widget content loaded")
            } catch (e: Exception) {
                LogUtil.log("[ChatWidgetFragment] Error loading widget content: ${e.message}")
            }
        }
    }

    private fun setupKeyboardAnimation() {
        ViewCompat.setWindowInsetsAnimationCallback(
            container,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                    container.translationY = -imeHeight.toFloat()
                    return insets
                }
            }
        )
    }

    private fun handleClose() {
        // Handle close action - could remove fragment or notify parent
        parentFragmentManager.beginTransaction()
            .remove(this)
            .commit()
    }

    /**
     * Update the widget configuration and reload content
     */
    fun updateHelloConfig(newHelloConfig: Map<String, Any>) {
        if (!newHelloConfig.containsKey("widgetToken")) {
            throw IllegalArgumentException("Missing 'widgetToken' in updated helloConfig")
        }

        helloConfig.clear()
        helloConfig.putAll(newHelloConfig)
        
        if (::chatWidgetCore.isInitialized) {
            try {
                val config = HelloConfig.fromMap(helloConfig)
                chatWidgetCore.updateConfiguration(config)
                LogUtil.log("[ChatWidgetFragment] Configuration updated via core: $newHelloConfig")
            } catch (e: Exception) {
                LogUtil.log("[ChatWidgetFragment] Error updating configuration: ${e.message}")
                // Fallback to direct load
                loadHtmlContent()
            }
        }
    }

    /**
     * Reload the widget content
     */
    fun loadWidget() {
        if (::chatWidgetCore.isInitialized) {
            chatWidgetCore.reloadWidget()
        } else {
            loadHtmlContent()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::chatWidgetCore.isInitialized) {
            chatWidgetCore.destroy()
        }
        // Unregister this fragment from the ChatWidget
        ChatWidget.unregisterFragment(this)
    }
}
