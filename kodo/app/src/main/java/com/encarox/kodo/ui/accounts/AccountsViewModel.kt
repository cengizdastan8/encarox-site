package com.encarox.kodo.ui.accounts

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encarox.kodo.data.gmail.GmailAuthManager
import com.encarox.kodo.data.model.GmailAccountInfo
import com.encarox.kodo.data.prefs.SettingsStore
import com.encarox.kodo.work.GmailSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val settings: SettingsStore,
    val authManager: GmailAuthManager
) : ViewModel() {

    val accounts: StateFlow<List<GmailAccountInfo>> = settings.accounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Called after a successful Google Sign-In with the chosen account email. */
    fun onAccountConnected(email: String?) {
        if (email.isNullOrBlank()) return
        viewModelScope.launch {
            settings.addAccount(email)
            reschedule()
        }
    }

    fun setEnabled(email: String, enabled: Boolean) = viewModelScope.launch {
        settings.setAccountEnabled(email, enabled)
        reschedule()
    }

    fun remove(email: String) = viewModelScope.launch {
        settings.removeAccount(email)
        reschedule()
    }

    private suspend fun reschedule() {
        val hasEnabled = settings.accounts.first().any { it.enabled }
        if (hasEnabled) {
            GmailSyncWorker.schedule(appContext, settings.syncIntervalMinutes.first())
        } else {
            GmailSyncWorker.cancel(appContext)
        }
    }
}
