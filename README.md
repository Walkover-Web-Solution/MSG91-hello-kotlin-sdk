# HelloSDK for Android

A modern, embeddable chat widget SDK for Android applications that provides seamless chat functionality with built-in file upload support.

## Features

- 🔥 **Easy Integration** - Simple Fragment-based integration
- 📁 **File Upload Support** - Built-in file picker with no additional setup required
- ⚡ **Modern Architecture** - Built with SOLID principles and Activity Result API
- 🎨 **Customizable UI** - Configurable colors, close button, and keyboard behavior
- 🔄 **Dynamic Configuration** - Update configuration at runtime
- 📱 **Edge-to-Edge Support** - Full screen experience with proper insets handling

## Requirements

- **Minimum SDK:** API 24 (Android 7.0)
- **Target SDK:** API 36
- **Java Version:** 11
- **Kotlin:** Compatible

## Installation

### Add to your `build.gradle.kts` (Module level)

```kotlin
dependencies {
    implementation("com.msg91:hellosdk:latest_version")
    
    // Required dependencies (if not already included)
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
}
```

### Add to your `build.gradle.kts` (App level)

Ensure your app's minimum SDK is 24 or higher:

```kotlin
android {
    compileSdk = 36
    
    defaultConfig {
        minSdk = 24
        // ... other configurations
    }
}
```

## Quick Start

### 1. Basic Integration

```kotlin
import com.msg91.hellosdk.ChatWidget
import com.msg91.hellosdk.config.HelloConfig

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize ChatWidget with your widget token
        val config = HelloConfig("your-widget-token")
        ChatWidget.initialize(config)
        
        // Create and add the chat fragment
        val chatFragment = ChatWidget.createFragment()
        
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, chatFragment)
            .commit()
    }
}
```

### 2. Configuration with User Information

```kotlin
// Using the builder pattern
val config = HelloConfig.builder("your-widget-token")
    .addProperty("mail", "user@example.com")
    .addProperty("name", "John Doe")
    .addProperty("phone", "+1234567890")
    .build()

ChatWidget.initialize(config)
```

### 3. Customized Widget Appearance

```kotlin
val chatFragment = ChatWidget.createFragment(
    widgetColor = "#8686ac",                    // Custom color theme
    isCloseButtonVisible = false,               // Hide close button for embedded mode
    useKeyboardAvoidingView = true              // Enable keyboard handling
)
```

## Configuration

### HelloConfig

The `HelloConfig` class manages all configuration parameters for the chat widget.

#### Required Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `widgetToken` | String | Your unique widget token (required) |

#### Optional Parameters

You can add any additional properties using the builder pattern or `withProperty()` method:

| Common Parameters | Type | Description |
|-------------------|------|-------------|
| `mail` | String | User's email address |
| `name` | String | User's display name |
| `phone` | String | User's phone number |
| `customData` | Any | Custom user data |

### Creating Configuration

#### Method 1: Constructor + Builder Pattern

```kotlin
val config = HelloConfig.builder("your-widget-token")
    .addProperty("mail", "user@example.com")
    .addProperty("name", "John Doe")
    .addProperties(mapOf(
        "department" to "Support",
        "plan" to "Premium"
    ))
    .build()
```

#### Method 2: Using withProperty()

```kotlin
val config = HelloConfig("your-widget-token")
    .withProperty("mail", "user@example.com")
    .withProperty("name", "John Doe")
```

#### Method 3: From Map (Migration Support)

```kotlin
val configMap = mapOf(
    "widgetToken" to "your-widget-token",
    "mail" to "user@example.com",
    "name" to "John Doe"
)

val config = HelloConfig.fromMap(configMap)
```

## Advanced Usage

### Dynamic Configuration Updates

Update the chat configuration at runtime:

```kotlin
// Update user information
val updatedConfig = HelloConfig("your-widget-token")
    .withProperty("mail", "newemail@example.com")
    .withProperty("status", "premium")

ChatWidget.update(updatedConfig)
```

### Fragment Lifecycle Management

```kotlin
class ChatActivity : AppCompatActivity() {
    
    private lateinit var chatFragment: Fragment
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize only once
        if (savedInstanceState == null) {
            val config = HelloConfig("your-widget-token")
            ChatWidget.initialize(config)
            
            chatFragment = ChatWidget.createFragment(
                widgetColor = "#2196F3",
                isCloseButtonVisible = true,
                useKeyboardAvoidingView = true
            )
            
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, chatFragment)
                .commit()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up when no longer needed
        if (isFinishing) {
            ChatWidget.destroy()
        }
    }
}
```

### Checking Initialization Status

```kotlin
if (ChatWidget.isInitialized()) {
    // SDK is ready to use
    val currentConfig = ChatWidget.getCurrentConfiguration()
    // Update configuration if needed
} else {
    // Initialize first
    ChatWidget.initialize(config)
}
```

## File Upload Support

The HelloSDK includes **built-in file upload functionality** with **zero additional setup required**. File uploads are handled automatically when users interact with file upload elements in the chat.

### Features

- ✅ **No Setup Required** - File picker and upload handling is completely internal
- ✅ **Modern Activity Result API** - Uses latest Android file selection APIs
- ✅ **Multiple File Selection** - Supports single and multiple file selection
- ✅ **All File Types** - Images, documents, and any file type supported
- ✅ **Automatic Fallback** - Multiple fallback mechanisms ensure reliability
- ✅ **Permission Handling** - Automatic permission requests when needed

### How It Works

1. User taps a file upload button in the chat
2. SDK automatically opens the system file picker
3. User selects file(s)
4. Files are automatically processed and uploaded
5. Chat receives the uploaded files

**No additional code required from your side!**

## UI Customization

### Fragment Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `widgetColor` | String? | null | Hex color code for theming |
| `isCloseButtonVisible` | Boolean | true | Show/hide close button |
| `useKeyboardAvoidingView` | Boolean | true | Enable keyboard avoidance |

### Example Customizations

```kotlin
// Minimal embedded widget
val embeddedWidget = ChatWidget.createFragment(
    widgetColor = "#FF5722",
    isCloseButtonVisible = false,
    useKeyboardAvoidingView = true
)

// Full-screen modal widget
val modalWidget = ChatWidget.createFragment(
    widgetColor = "#9C27B0",
    isCloseButtonVisible = true,
    useKeyboardAvoidingView = false
)
```

## Edge-to-Edge Support

The SDK fully supports edge-to-edge layouts. Handle window insets in your Activity:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Handle window insets
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_container)) { v, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
        
        v.setPadding(
            systemBars.left,
            systemBars.top,
            systemBars.right,
            if (isImeVisible) 0 else systemBars.bottom
        )
        insets
    }
}
```

## Error Handling

### Common Exceptions

| Exception | Cause | Solution |
|-----------|-------|----------|
| `IllegalArgumentException` | Invalid configuration | Check widgetToken is not null/blank |
| `IllegalStateException` | Not initialized | Call `ChatWidget.initialize()` first |
| `IllegalStateException` | Already initialized | Use `ChatWidget.update()` instead |

### Error Handling Example

```kotlin
try {
    val config = HelloConfig("your-widget-token")
        .withProperty("mail", "user@example.com")
    
    ChatWidget.initialize(config)
    
} catch (e: IllegalArgumentException) {
    Log.e("ChatWidget", "Invalid configuration: ${e.message}")
} catch (e: IllegalStateException) {
    Log.e("ChatWidget", "Initialization error: ${e.message}")
}
```

## Best Practices

### 1. Initialize Early
```kotlin
// Initialize in onCreate, not onResume
ChatWidget.initialize(config)
```

### 2. Handle Configuration Updates
```kotlin
// Update configuration when user data changes
fun updateUserEmail(newEmail: String) {
    val updatedConfig = ChatWidget.getCurrentConfiguration()
        .withProperty("mail", newEmail)
    ChatWidget.update(updatedConfig)
}
```

### 3. Proper Lifecycle Management
```kotlin
// Clean up when appropriate
override fun onDestroy() {
    super.onDestroy()
    if (isFinishing) {
        ChatWidget.destroy()
    }
}
```

### 4. Fragment State Management
```kotlin
// Save fragment state properly
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Only initialize once
    if (savedInstanceState == null) {
        ChatWidget.initialize(config)
        // Add fragment
    }
}
```

## Migration Guide

### From Map-based Configuration

If you're migrating from an older version that used `Map<String, Any>`:

```kotlin
// Old approach
val configMap = mapOf(
    "widgetToken" to "your-token",
    "mail" to "user@example.com"
)

// New approach
val config = HelloConfig.fromMap(configMap)
// or
val config = HelloConfig("your-token")
    .withProperty("mail", "user@example.com")
```

## Troubleshooting

### File Upload Not Working

File uploads are handled automatically. If you encounter issues:

1. ✅ Ensure minimum SDK 24
2. ✅ Check permissions in AndroidManifest.xml are not blocking file access
3. ✅ Test on a physical device (file picker may not work properly in some emulators)

### Widget Not Loading

1. ✅ Verify widgetToken is correct
2. ✅ Check internet connectivity
3. ✅ Ensure proper initialization order
4. ✅ Check logs for error messages

### Fragment Issues

1. ✅ Use `supportFragmentManager` for Activities
2. ✅ Use `childFragmentManager` for nested Fragments
3. ✅ Handle configuration changes properly
4. ✅ Don't re-initialize on orientation changes

## Support

For technical support or questions:

- 📧 Email: support@msg91.com
- 📚 Documentation: [Link to full documentation]
- 🐛 Bug Reports: [Link to issue tracker]

## License

```
Copyright (c) MSG91
Licensed under [License Type]
```

---

**Ready to integrate? Start with the [Quick Start](#quick-start) guide above!** 🚀
