package com.panko.brewmate.viewmodel

import androidx.lifecycle.ViewModel
import com.panko.brewmate.data.CoffeeMakerRepository
import com.panko.brewmate.model.BrewSettings
import com.panko.brewmate.model.CoffeeMakerState
import com.panko.brewmate.model.CoffeeShotSize
import com.panko.brewmate.model.DrinkType
import com.panko.brewmate.model.MilkType
import com.panko.brewmate.model.Temperature
import kotlinx.coroutines.flow.StateFlow

class CoffeeMakerViewModel(
    private val repository: CoffeeMakerRepository // ViewModel now receives the repository
) : ViewModel() {

    // --- Expose flows directly from the repository ---
    val coffeeMakerState: StateFlow<CoffeeMakerState> = repository.coffeeMakerState
    val beansLevel: StateFlow<Int> = repository.beansLevel
    val waterLevel: StateFlow<Int> = repository.waterLevel
    val milkLevel: StateFlow<Int> = repository.milkLevel
    val groundsBinLevel: StateFlow<Int> = repository.groundsBinLevel
    val selectedDrinkType: StateFlow<DrinkType> = repository.selectedDrinkType
    val customBrewSettings: StateFlow<BrewSettings> = repository.customBrewSettings

    // --- Pass actions directly to the repository ---
    fun togglePower() { repository.togglePower() }
    fun startBrew(drinkType: DrinkType, customSettings: BrewSettings?) { repository.startBrew(drinkType, customSettings) }
    fun stopBrew() { repository.stopBrew() }
    fun setSelectedCoffeeType(type: DrinkType) { repository.setSelectedCoffeeType(type) }
    fun setCustomStrength(strength: String) { repository.setCustomStrength(strength) }
    fun setCustomCoffeeShotSize(size: CoffeeShotSize) { repository.setCustomCoffeeShotSize(size) }
    fun setCustomMilkType(milkType: MilkType) { repository.setCustomMilkType(milkType) }
    fun setCustomTemperature(temperature: Temperature) { repository.setCustomTemperature(temperature) }
    fun addBeans() { repository.addBeans() }
    fun addWater() { repository.addWater() }
    fun addMilk() { repository.addMilk() }
    fun emptyGroundsBin() { repository.emptyGroundsBin() }
    fun clearAlert() { repository.clearMaintenanceAlert() }
    fun setBrewSettingsFromFavorite(settings: BrewSettings, drinkName: String) { repository.setBrewSettingsFromFavorite(settings, drinkName) }
}