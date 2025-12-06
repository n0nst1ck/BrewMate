package com.panko.brewmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.panko.brewmate.data.AuthRepository
import com.panko.brewmate.data.HistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(
    historyRepository: HistoryRepository,
    authRepository: AuthRepository
) : ViewModel() {

    private val userId = authRepository.getCurrentUserId() ?: ""

    val historyList = if (userId.isBlank()) {
        emptyFlow()
    } else {
        historyRepository.getHistory(userId)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
}