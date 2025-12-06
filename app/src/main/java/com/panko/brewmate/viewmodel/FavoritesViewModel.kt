package com.panko.brewmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.panko.brewmate.data.AuthRepository
import com.panko.brewmate.data.FavoritesRepository
import com.panko.brewmate.model.BrewSettings
import com.panko.brewmate.model.FavoriteDrink
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val favoritesRepository: FavoritesRepository,
    authRepository: AuthRepository // Needed to get the userId
) : ViewModel() {

    private val userId = authRepository.getCurrentUserId() ?: ""

    // Live stream of all favorite drinks
    val favoriteDrinks = if (userId.isBlank()) {
        emptyFlow()
    } else {
        favoritesRepository.getFavorites(userId)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun saveFavorite(name: String, settings: BrewSettings) {
        viewModelScope.launch {
            // Create the new drink model
            val newDrink = FavoriteDrink(
                userId = userId,
                name = name,
                settings = settings
            )
            favoritesRepository.saveFavorite(newDrink)
            // Error handling logic (omitted for brevity)
        }
    }

    fun deleteFavorite(drinkId: String) {
        viewModelScope.launch {
            favoritesRepository.deleteFavorite(drinkId)
        }
    }
}