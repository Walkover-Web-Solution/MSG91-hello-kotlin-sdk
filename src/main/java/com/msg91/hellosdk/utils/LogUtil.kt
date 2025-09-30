package com.msg91.hellosdk.utils

object LogUtil {
    private const val IS_DEBUG = true

    fun log(vararg messages: Any?) {
        if (IS_DEBUG) {
            val output = messages.joinToString(" ") { it.toString() }
            println("[MSG91 HELLO SDK]: $output")
        }
    }
}
