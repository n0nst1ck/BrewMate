package com.panko.brewmate.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.panko.brewmate.data.AuthRepository
import com.panko.brewmate.data.SchedulingRepository
import com.panko.brewmate.model.ScheduledBrew
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SchedulingViewModel(
    private val schedulingRepository: SchedulingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val currentUserIdFlow = MutableStateFlow(authRepository.getCurrentUserId())

    // Reactive List
    // Whenever currentUserIdFlow changes, this automatically switches to the correct user's list
    val scheduledBrews = currentUserIdFlow.flatMapLatest { userId ->
        if (userId.isNullOrBlank()) {
            flowOf(emptyList()) // If no user, show empty list
        } else {
            schedulingRepository.getScheduledBrews(userId)
                .catch {
                    emit(emptyList())
                }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Used to add a schedule to database
    fun addSchedule(schedule: ScheduledBrew) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()

            // Update the flow so the list knows we are active
            currentUserIdFlow.value = userId

            if (userId.isNullOrBlank()) {
                return@launch
            }

            try {
                // Attach the real user ID to the schedule
                val finalSchedule = schedule.copy(userID = userId)

                // Call the repository
                schedulingRepository.scheduleBrew(finalSchedule)
            } catch (e: Exception) {
                Log.e("DEBUG_BREW", "❌ CRITICAL ERROR: ViewModel crashed", e)
            }
        }
    }

    // Used to delete a schedule from database
    fun deleteSchedule(schedule: ScheduledBrew) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            if (!userId.isNullOrBlank()) {
                schedulingRepository.cancelBrew(schedule.id)
            }
        }
    }
}