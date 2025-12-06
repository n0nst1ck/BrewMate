package com.panko.brewmate.data

import com.panko.brewmate.model.FavoriteDrink
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    // Fetches all saved favorites for the user
    fun getFavorites(userId: String): Flow<List<FavoriteDrink>>

    // Saves a new favorite drink
    suspend fun saveFavorite(drink: FavoriteDrink): Result<Unit>

    // Deletes a favorite
    suspend fun deleteFavorite(drinkId: String): Result<Unit>
}