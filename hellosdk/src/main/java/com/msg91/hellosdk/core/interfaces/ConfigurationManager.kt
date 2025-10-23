package com.msg91.hellosdk.core.interfaces

import com.msg91.hellosdk.config.HelloConfig

/**
 * Interface for managing chat widget configuration.
 * Follows Interface Segregation Principle by focusing only on configuration management.
 */
interface ConfigurationManager {
    /**
     * Set the current configuration.
     */
    fun setConfiguration(config: HelloConfig)
    
    /**
     * Get the current configuration.
     */
    fun getCurrentConfiguration(): HelloConfig?
    
    /**
     * Update the configuration with new values.
     */
    fun updateConfiguration(config: HelloConfig)
    
    /**
     * Validate that the configuration is complete and valid.
     */
    fun validateConfiguration(config: HelloConfig): Boolean
}
