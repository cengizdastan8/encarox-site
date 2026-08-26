package com.encarox.kodo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encarox.kodo.data.CodeNotifier
import com.encarox.kodo.data.model.CodeSource
import com.encarox.kodo.data.prefs.SettingsStore
import com.encarox.kodo.data.repository.CodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles one-off SMS messages captured through the User Consent flow.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: CodeRepository,
    private val notifier: CodeNotifier,
    private val settings: SettingsStore
) : ViewModel() {

    fun ingestSmsMessage(message: String?, sender: String = "SMS") {
        if (message.isNullOrBlank()) return
        viewModelScope.launch {
            val stored = repository.ingest(
                rawMessage = message,
                source = CodeSource.SMS,
                sender = sender
            )
            if (stored != null && settings.notificationsEnabled.first()) {
                notifier.notifyCode(stored)
            }
        }
    }
}
