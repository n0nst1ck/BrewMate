package com.panko.brewmate.data

import com.panko.brewmate.model.ScheduledBrew
import kotlinx.coroutines.flow.Flow

interface SchedulingRepository {
    suspend fun scheduleBrew(scheduledBrew: ScheduledBrew): Result<Unit>

    fun getScheduledBrews(userId: String): Flow<List<ScheduledBrew>>

    suspend fun cancelBrew(brewId: String): Result<Unit>

}