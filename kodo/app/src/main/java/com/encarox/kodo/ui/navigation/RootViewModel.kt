package com.encarox.kodo.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encarox.kodo.data.prefs.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    private val settings: SettingsStore
) : ViewModel() {
    val onboarded: StateFlow<Boolean> = settings.onboarded
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun completeOnboarding() = viewModelScope.launch { settings.setOnboarded(true) }
}
