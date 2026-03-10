package com.panko.brewmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.panko.brewmate.data.AuthRepository
import com.panko.brewmate.data.FavoritesRepository
import com.panko.brewmate.model.BrewSettings
import com.panko.brewmate.model.FavoriteDrink
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val favoritesRepository: FavoritesRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val favoriteDrinks = authRepository.currentUserIdFlow.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            favoritesRepository.getFavorites(uid).catch { emit(emptyList()) }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun saveFavorite(name: String, settings: BrewSettings): Boolean {
        val uid = authRepository.getCurrentUserId()
        if (uid.isNullOrBlank()) return false

        val currentFavorites = favoriteDrinks.value
        val isDuplicate = currentFavorites.any { it.settings == settings }

        if (isDuplicate) return false

        val newFavorite = FavoriteDrink(userId = uid, name = name, settings = settings)

        viewModelScope.launch {
            favoritesRepository.saveFavorite(newFavorite)
        }
        return true
    }

    fun updateFavorite(drinkId: String, newName: String, newSettings: BrewSettings): Boolean {
        val uid = authRepository.getCurrentUserId()
        if (uid.isNullOrBlank()) return false

        val currentFavorites = favoriteDrinks.value
        val isDuplicate = currentFavorites.any { it.id != drinkId && it.settings == newSettings }

        if (isDuplicate) return false

        val updatedFavorite = FavoriteDrink(
            id = drinkId,
            userId = uid,
            name = newName,
            settings = newSettings
        )

        viewModelScope.launch {
            favoritesRepository.updateFavorite(updatedFavorite)
        }
        return true
    }

    fun deleteFavorite(drinkId: String) {
        viewModelScope.launch {
            favoritesRepository.deleteFavorite(drinkId)
        }
    }
}