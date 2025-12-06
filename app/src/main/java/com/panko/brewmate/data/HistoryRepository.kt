package com.panko.brewmate.data

import com.panko.brewmate.model.BrewHistoryItem
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    // 1. Get all history for a user (ordered by newest first)
    fun getHistory(userId: String): Flow<List<BrewHistoryItem>>

    // 2. Add a new item to history (Called when you start a brew)
    suspend fun addHistoryItem(item: BrewHistoryItem): Result<Unit>

    // 3. Clear history (Optional, but good for testing)
    suspend fun clearHistory(userId: String): Result<Unit>
}