package com.panko.brewmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.panko.brewmate.data.AuthRepository
import com.panko.brewmate.data.FavoritesRepository
import com.panko.brewmate.model.BrewSettings
import com.panko.brewmate.model.FavoriteDrink
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
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
            .catch { emit(emptyList()) }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun saveFavorite(name: String, settings: BrewSettings): Boolean {
        val currentFavorites = favoriteDrinks.value

        // Check if any favorite has the exact same settings
        val isDuplicate = currentFavorites.any { it.settings == settings }

        if (isDuplicate) {
            return false
        }

        // Save
        val newFavorite = FavoriteDrink(userId = userId, name = name, settings = settings)

        viewModelScope.launch {
            favoritesRepository.saveFavorite(newFavorite)
        }
        return true
    }

    fun deleteFavorite(drinkId: String) {
        viewModelScope.launch {
            favoritesRepository.deleteFavorite(drinkId)
        }
    }
}