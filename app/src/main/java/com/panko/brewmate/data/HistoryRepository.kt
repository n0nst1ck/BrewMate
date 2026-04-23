package com.panko.brewmate.data

import com.panko.brewmate.model.BrewHistoryItem
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    // Get all history for a user (ordered by newest first)
    fun getHistory(userId: String): Flow<List<BrewHistoryItem>>

    // Add a new item to history
    suspend fun addHistoryItem(item: BrewHistoryItem): Result<Unit>

    // Clear history for a user
    suspend fun clearHistory(userId: String): Result<Unit>
}