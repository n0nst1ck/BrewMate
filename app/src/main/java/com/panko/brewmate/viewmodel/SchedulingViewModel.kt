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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SchedulingViewModel(
    private val schedulingRepository: SchedulingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // --- 1. DEFINE THE MISSING FLOW HERE ---
    // This holds the current User ID and notifies the list when it changes.
    private val currentUserIdFlow = MutableStateFlow(authRepository.getCurrentUserId())

    // --- 2. REACTIVE LIST ---
    // Whenever currentUserIdFlow changes, this automatically switches to the correct user's list.
    val scheduledBrews = currentUserIdFlow.flatMapLatest { userId ->
        if (userId.isNullOrBlank()) {
            flowOf(emptyList()) // If no user, show empty list
        } else {
            schedulingRepository.getScheduledBrews(userId)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Helper to refresh ID (call this from UI LaunchedEffect)
    fun refreshUser() {
        val userId = authRepository.getCurrentUserId()
        currentUserIdFlow.value = userId
        Log.d("DEBUG_BREW", "ViewModel refreshed. Current User: '$userId'")
    }

    // --- 3. ADD SCHEDULE WITH LOGS ---
    fun addSchedule(schedule: ScheduledBrew) {
        viewModelScope.launch {
            Log.d("DEBUG_BREW", "1. Button Clicked! Starting addSchedule...")

            val userId = authRepository.getCurrentUserId()
            Log.d("DEBUG_BREW", "2. User ID found: '$userId'")

            // Update the flow so the list knows we are active
            currentUserIdFlow.value = userId

            if (userId.isNullOrBlank()) {
                Log.e("DEBUG_BREW", "❌ ERROR: User ID is blank! Cannot save.")
                return@launch
            }

            try {
                // Attach the real user ID to the schedule
                val finalSchedule = schedule.copy(userID = userId)

                Log.d("DEBUG_BREW", "3. Saving to Repo: ${finalSchedule.id} at ${finalSchedule.hour}:${finalSchedule.minute}")

                // Call the repository
                val result = schedulingRepository.scheduleBrew(finalSchedule)

                result.onSuccess {
                    Log.d("DEBUG_BREW", "✅ SUCCESS: Schedule saved & Alarm set!")
                }.onFailure { e ->
                    Log.e("DEBUG_BREW", "❌ FAIL: Repository threw an error", e)
                }

            } catch (e: Exception) {
                Log.e("DEBUG_BREW", "❌ CRITICAL ERROR: ViewModel crashed", e)
            }
        }
    }

    // --- 4. DELETE SCHEDULE ---
    fun deleteSchedule(schedule: ScheduledBrew) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            if (!userId.isNullOrBlank()) {
                schedulingRepository.cancelBrew(schedule.id)
            }
        }
    }
}