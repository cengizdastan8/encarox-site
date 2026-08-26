package com.encarox.kodo.data.model

/**
 * A single verification / OTP code that Kodo captured, together with enough
 * context to show the user where it came from.
 */
data class CodeItem(
    val id: Long = 0,
    val code: String,
    val source: CodeSource,
    /** Human label: sender phone, "Google", the service name, or the Gmail address. */
    val sender: String,
    /** For Gmail: which account received it (e.g. me@gmail.com). Null for SMS. */
    val account: String? = null,
    /** Best-effort service/brand the code is for, e.g. "Instagram", "PayPal". */
    val service: String? = null,
    /** Full original message text (kept locally only). */
    val rawMessage: String,
    /** Epoch millis the message was received. */
    val receivedAt: Long,
    val isRead: Boolean = false,
    val isArchived: Boolean = false
)
