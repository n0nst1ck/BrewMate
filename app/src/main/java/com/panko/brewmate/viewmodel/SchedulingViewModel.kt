package com.panko.brewmate.viewmodel

import androidx.lifecycle.ViewModel
import com.google.rpc.context.AttributeContext
import com.panko.brewmate.data.SchedulingRepository
import com.panko.brewmate.data.AuthRepository
import com.panko.brewmate.model.BrewSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.emptyFlow


class SchedulingViewModel(
    private val schedulingRepository: SchedulingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val userId: String = authRepository.getCurrentUserId() ?: ""
    val scheduledBrews = if (userId.isBlank()){
            emptyFlow()
        } else {
            schedulingRepository.getScheduledBrews(userId)
        }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun saveSchedule(settings: BrewSettings, hour: Int, minute: Int, isRecurrent: Boolean) {

    }


}