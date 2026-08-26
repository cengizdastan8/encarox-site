package com.encarox.kodo.data.sms

import android.content.Context
import com.google.android.gms.auth.api.phone.SmsRetriever
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Starts the SMS User Consent flow. While active (about 5 minutes), the next
 * incoming SMS that contains a numeric code triggers a system consent prompt —
 * no READ_SMS / RECEIVE_SMS permission required, so it stays Play-compliant.
 */
@Singleton
class SmsUserConsentManager @Inject constructor(
    private val appContext: Context
) {
    /**
     * @param senderPhoneNumber optional; pass null to listen to any sender.
     */
    suspend fun startListening(senderPhoneNumber: String? = null) {
        SmsRetriever.getClient(appContext)
            .startSmsUserConsent(senderPhoneNumber)
            .await()
    }
}
