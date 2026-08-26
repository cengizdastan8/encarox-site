package com.encarox.kodo.data.sms

import android.content.Intent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Bridges the [SmsUserConsentReceiver] (a BroadcastReceiver) and the UI layer.
 * The receiver cannot start an Activity for the consent dialog directly, so it
 * emits the consent Intent here and MainActivity launches it.
 */
object SmsConsentBus {
    private val _consentIntents = MutableSharedFlow<Intent>(replay = 1)
    val consentIntents: SharedFlow<Intent> = _consentIntents.asSharedFlow()

    suspend fun emit(intent: Intent) = _consentIntents.emit(intent)
}
