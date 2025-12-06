package com.panko.brewmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.panko.brewmate.data.CoffeeMakerRepository
import com.panko.brewmate.data.HistoryRepository
import com.panko.brewmate.data.AuthRepository
import com.panko.brewmate.model.BrewHistoryItem
import com.panko.brewmate.model.BrewSettings
import com.panko.brewmate.model.CoffeeMakerState
import com.panko.brewmate.model.CoffeeShotSize
import com.panko.brewmate.model.DrinkType
import com.panko.brewmate.model.MilkType
import com.panko.brewmate.model.SyrupType
import com.panko.brewmate.model.Temperature
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CoffeeMakerViewModel(
    private val coffeeMakerRepository: CoffeeMakerRepository, // ViewModel now receives the repository
    private val historyRepository: HistoryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // --- Expose flows directly from the repository ---
    val coffeeMakerState: StateFlow<CoffeeMakerState> = coffeeMakerRepository.coffeeMakerState
    val beansLevel: StateFlow<Int> = coffeeMakerRepository.beansLevel
    val waterLevel: StateFlow<Int> = coffeeMakerRepository.waterLevel
    val milkLevel: StateFlow<Int> = coffeeMakerRepository.milkLevel
    val groundsBinLevel: StateFlow<Int> = coffeeMakerRepository.groundsBinLevel
    val selectedDrinkType: StateFlow<DrinkType> = coffeeMakerRepository.selectedDrinkType
    val customBrewSettings: StateFlow<BrewSettings> = coffeeMakerRepository.customBrewSettings

    // --- Pass actions directly to the repository ---
    fun togglePower() { coffeeMakerRepository.togglePower() }
    fun startBrew(drinkType: DrinkType, customSettings: BrewSettings?) {
        coffeeMakerRepository.startBrew(drinkType, customSettings)
        // adding brew to user history
        val currentUserId = authRepository.getCurrentUserId()

        if (currentUserId != null) {
            viewModelScope.launch {
                // Determine the settings to save
                // If customSettings is null, use the defaults from the drinkType
                val settingsToSave = customSettings ?: BrewSettings(
                    strength = drinkType.defaultStrength,
                    coffeeShotSize = drinkType.defaultCoffeeShotSize,
                    milkType = drinkType.defaultMilkType,
                    temperature = drinkType.defaultTemperature,
                    syrupType = SyrupType.NONE,
                    syrupPumps = 0,
                    sugarAmount = 0
                )

                // Create the history item
                val historyItem = BrewHistoryItem(
                    userId = currentUserId,
                    drinkName = drinkType.displayName,
                    settings = settingsToSave,
                    timestamp = System.currentTimeMillis()
                )

                // Save to Firebase (Fire and forget)
                historyRepository.addHistoryItem(historyItem)
            }
        }
    }
    fun stopBrew() { coffeeMakerRepository.stopBrew() }
    fun setSelectedCoffeeType(type: DrinkType) { coffeeMakerRepository.setSelectedCoffeeType(type) }
    fun setCustomStrength(strength: String) { coffeeMakerRepository.setCustomStrength(strength) }
    fun setCustomCoffeeShotSize(size: CoffeeShotSize) { coffeeMakerRepository.setCustomCoffeeShotSize(size) }
    fun setCustomMilkType(milkType: MilkType) { coffeeMakerRepository.setCustomMilkType(milkType) }
    fun setCustomTemperature(temperature: Temperature) { coffeeMakerRepository.setCustomTemperature(temperature) }
    fun addBeans() { coffeeMakerRepository.addBeans() }
    fun addWater() { coffeeMakerRepository.addWater() }
    fun addMilk() { coffeeMakerRepository.addMilk() }
    fun emptyGroundsBin() { coffeeMakerRepository.emptyGroundsBin() }
    fun clearAlert() { coffeeMakerRepository.clearMaintenanceAlert() }
    fun setBrewSettingsFromFavorite(settings: BrewSettings, drinkName: String) { coffeeMakerRepository.setBrewSettingsFromFavorite(settings, drinkName) }
}