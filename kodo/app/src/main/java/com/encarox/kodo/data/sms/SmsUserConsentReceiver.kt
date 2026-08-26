package com.encarox.kodo.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.os.bundleOf
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives the SMS User Consent broadcast and forwards the system consent
 * Intent to the UI via [SmsConsentBus]. Protected by the GMS SEND permission
 * declared in the manifest, so only Google Play services can trigger it.
 */
class SmsUserConsentReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SmsRetriever.SMS_RETRIEVED_ACTION) return
        val extras = intent.extras ?: return

        @Suppress("DEPRECATION")
        val status = extras.get(SmsRetriever.EXTRA_STATUS) as? Status ?: return

        if (status.statusCode == CommonStatusCodes.SUCCESS) {
            @Suppress("DEPRECATION")
            val consentIntent = extras.get(SmsRetriever.EXTRA_CONSENT_INTENT) as? Intent ?: return
            val pending = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    SmsConsentBus.emit(consentIntent)
                } finally {
                    pending.finish()
                }
            }
        }
    }
}
