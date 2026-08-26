package com.encarox.kodo.data.model

/** Where a captured code came from. */
enum class CodeSource {
    SMS,
    GMAIL;

    companion object {
        fun fromName(value: String?): CodeSource =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SMS
    }
}
