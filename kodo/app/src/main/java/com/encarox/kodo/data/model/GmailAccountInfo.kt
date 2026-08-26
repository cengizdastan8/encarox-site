package com.encarox.kodo.data.model

/** A Gmail account the user connected for code scanning. */
data class GmailAccountInfo(
    val email: String,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val enabled: Boolean = true,
    val lastSyncedAt: Long = 0L
)
