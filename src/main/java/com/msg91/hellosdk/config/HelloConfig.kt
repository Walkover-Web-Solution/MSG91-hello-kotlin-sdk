package com.msg91.hellosdk.config

/**
 * Configuration class for HelloChatWidget SDK.
 * 
 * This class follows the Open/Closed Principle by allowing extension
 * through additional optional properties while maintaining the core
 * required configuration (widgetToken).
 */
data class HelloConfig(
    /**
     * Mandatory widget token for authentication with the chat service.
     * This field is required and must not be null or blank.
     */
    val widgetToken: String,
    
    /**
     * Additional optional configuration properties.
     * This map allows flexible extension of configuration without
     * modifying the class structure, following Open/Closed Principle.
     */
    private val additionalProperties: Map<String, Any> = emptyMap()
) {
    
    init {
        require(widgetToken.isNotBlank()) {
            "widgetToken cannot be null or blank"
        }
    }
    
    /**
     * Get all configuration properties as a map, including the mandatory
     * widgetToken and any additional properties.
     */
    fun toMap(): Map<String, Any> {
        return mapOf("widgetToken" to widgetToken) + additionalProperties
    }
    
    /**
     * Get a specific additional property by key.
     * Returns null if the property doesn't exist.
     */
    fun getProperty(key: String): Any? {
        return if (key == "widgetToken") widgetToken else additionalProperties[key]
    }
    
    /**
     * Check if a specific property exists in the configuration.
     */
    fun hasProperty(key: String): Boolean {
        return key == "widgetToken" || additionalProperties.containsKey(key)
    }
    
    /**
     * Create a new HelloConfig with additional properties.
     * This follows the Open/Closed Principle by allowing extension
     * without modification.
     */
    fun withAdditionalProperties(properties: Map<String, Any>): HelloConfig {
        return copy(additionalProperties = additionalProperties + properties)
    }
    
    /**
     * Create a new HelloConfig with a single additional property.
     */
    fun withProperty(key: String, value: Any): HelloConfig {
        return withAdditionalProperties(mapOf(key to value))
    }
    
    companion object {
        /**
         * Builder pattern for creating HelloConfig with additional properties.
         * This provides a fluent API for configuration building.
         */
        class Builder(private val widgetToken: String) {
            private val properties = mutableMapOf<String, Any>()
            
            fun addProperty(key: String, value: Any): Builder {
                properties[key] = value
                return this
            }
            
            fun addProperties(additionalProperties: Map<String, Any>): Builder {
                properties.putAll(additionalProperties)
                return this
            }
            
            fun build(): HelloConfig {
                return HelloConfig(widgetToken, properties.toMap())
            }
        }
        
        /**
         * Create a builder for constructing HelloConfig with additional properties.
         */
        fun builder(widgetToken: String): Builder {
            return Builder(widgetToken)
        }
        
        /**
         * Create HelloConfig from an existing map.
         * Useful for migration from the old Map<String, Any> approach.
         */
        fun fromMap(configMap: Map<String, Any>): HelloConfig {
            val widgetToken = configMap["widgetToken"] as? String
                ?: throw IllegalArgumentException("Missing required 'widgetToken' in configuration map")
            
            val additionalProperties = configMap.filterKeys { it != "widgetToken" }
            return HelloConfig(widgetToken, additionalProperties)
        }
    }
}
