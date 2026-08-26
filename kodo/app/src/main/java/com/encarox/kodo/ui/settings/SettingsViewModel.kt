package com.encarox.kodo.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encarox.kodo.data.prefs.SettingsStore
import com.encarox.kodo.data.sms.SmsUserConsentManager
import com.encarox.kodo.work.GmailSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val syncMinutes: Int = SettingsStore.DEFAULT_SYNC_MINUTES,
    val retentionDays: Int = SettingsStore.DEFAULT_RETENTION_DAYS,
    val autoCopy: Boolean = true,
    val notifications: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val settings: SettingsStore,
    private val smsConsent: SmsUserConsentManager
) : ViewModel() {

    val syncMinutes: StateFlow<Int> = settings.syncIntervalMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsStore.DEFAULT_SYNC_MINUTES)
    val retentionDays: StateFlow<Int> = settings.retentionDays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsStore.DEFAULT_RETENTION_DAYS)
    val notifications: StateFlow<Boolean> = settings.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setSyncMinutes(minutes: Int) = viewModelScope.launch {
        settings.setSyncInterval(minutes)
        if (settings.accounts.first().any { it.enabled }) {
            GmailSyncWorker.schedule(appContext, minutes)
        }
    }

    fun setRetentionDays(days: Int) = viewModelScope.launch { settings.setRetentionDays(days) }

    fun setNotifications(enabled: Boolean) = viewModelScope.launch {
        settings.setNotificationsEnabled(enabled)
    }

    /** Arm the SMS User Consent listener for the next incoming code. */
    fun listenForSms() = viewModelScope.launch {
        runCatching { smsConsent.startListening(null) }
    }
}
