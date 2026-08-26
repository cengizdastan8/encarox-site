package com.encarox.kodo.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encarox.kodo.data.model.CodeItem
import com.encarox.kodo.data.repository.CodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val repository: CodeRepository
) : ViewModel() {

    val codes: StateFlow<List<CodeItem>> = repository.activeCodes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unreadCount: StateFlow<Int> = repository.unreadCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun onOpened(item: CodeItem) = viewModelScope.launch {
        if (!item.isRead) repository.markRead(item.id)
    }

    fun onCopied(item: CodeItem) = viewModelScope.launch {
        if (!item.isRead) repository.markRead(item.id)
    }

    fun archive(item: CodeItem) = viewModelScope.launch { repository.archive(item.id) }
    fun delete(item: CodeItem) = viewModelScope.launch { repository.delete(item.id) }
    fun markAllRead() = viewModelScope.launch { repository.markAllRead() }
}
