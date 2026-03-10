package com.panko.brewmate.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.panko.brewmate.data.AuthRepository
import com.panko.brewmate.data.SchedulingRepository
import com.panko.brewmate.model.ScheduledBrew
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SchedulingViewModel(
    private val schedulingRepository: SchedulingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val scheduledBrews = authRepository.currentUserIdFlow.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            schedulingRepository.getScheduledBrews(uid)
                .catch { emit(emptyList()) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addSchedule(schedule: ScheduledBrew) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()

            if (userId.isNullOrBlank()) return@launch

            try {
                val finalSchedule = schedule.copy(userID = userId)
                schedulingRepository.scheduleBrew(finalSchedule)
            } catch (e: Exception) {
                Log.e("DEBUG_BREW", "❌ CRITICAL ERROR: ViewModel crashed", e)
            }
        }
    }

    fun deleteSchedule(schedule: ScheduledBrew) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            if (!userId.isNullOrBlank()) {
                schedulingRepository.cancelBrew(schedule.id)
            }
        }
    }
}