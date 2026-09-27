/*
 * File: NameUtils.kt
 * Module: Core (shared)
 * Description: Small helpers for showing user names
 */
package com.solargrid.mobile.core.utils

// "Kajan Siva" -> "KS", "Kajan" -> "K", blank -> "?"
fun initialsOf(fullName: String): String =
    fullName.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }
