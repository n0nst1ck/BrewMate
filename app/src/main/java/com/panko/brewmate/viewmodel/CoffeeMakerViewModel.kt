package com.panko.brewmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.panko.brewmate.data.CoffeeMakerRepository
import com.panko.brewmate.data.HistoryRepository
import com.panko.brewmate.data.AuthRepository
import com.panko.brewmate.model.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CoffeeMakerViewModel(
    private val coffeeMakerRepository: CoffeeMakerRepository,
    private val historyRepository: HistoryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // --- State & Essentials ---
    val coffeeMakerState: StateFlow<CoffeeMakerState> = coffeeMakerRepository.coffeeMakerState
    val beansLevel: StateFlow<Int> = coffeeMakerRepository.beansLevel
    val waterLevel: StateFlow<Int> = coffeeMakerRepository.waterLevel
    val groundsBinLevel: StateFlow<Int> = coffeeMakerRepository.groundsBinLevel

    // --- 🆕 INVENTORY MAPS (Fixed Access) ---
    // We access 'coffeeMakerRepository' (the instance), NOT 'CoffeeMakerRepository' (the class)
    val milkLevels: StateFlow<Map<MilkBase, Int>> = coffeeMakerRepository.milkLevels
    val syrupLevels: StateFlow<Map<SyrupType, Int>> = coffeeMakerRepository.syrupLevels
    val sugarLevels: StateFlow<Map<SugarType, Int>> = coffeeMakerRepository.sugarLevels
    val teaLevels: StateFlow<Map<TeaType, Int>> = coffeeMakerRepository.teaLevels
    val chocolateLevels: StateFlow<Map<ChocolateType, Int>> = coffeeMakerRepository.chocolateLevels

    // --- Settings ---
    val selectedDrinkType: StateFlow<DrinkType> = coffeeMakerRepository.selectedDrinkType
    val customBrewSettings: StateFlow<BrewSettings> = coffeeMakerRepository.customBrewSettings

    // --- Actions ---
    fun togglePower() { coffeeMakerRepository.togglePower() }

    fun startBrew(drinkType: DrinkType, customSettings: BrewSettings?) {
        coffeeMakerRepository.startBrew(drinkType, customSettings)

        // Add to history logic
        val currentUserId = authRepository.getCurrentUserId()
        if (currentUserId != null) {
            viewModelScope.launch {
                val settingsToSave = customSettings ?: BrewSettings(
                    strength = drinkType.defaultStrength,
                    coffeeShotSize = drinkType.defaultCoffeeShotSize,
                    milkStyle = drinkType.defaultMilkStyle,
                    temperature = drinkType.defaultTemperature,
                    // Ensure defaults are safe
                    syrupType = SyrupType.NONE,
                    syrupPumps = 0,
                    sugarAmount = 0
                )

                val historyItem = BrewHistoryItem(
                    userId = currentUserId,
                    drinkName = drinkType.displayName,
                    settings = settingsToSave,
                    timestamp = System.currentTimeMillis()
                )
                historyRepository.addHistoryItem(historyItem)
            }
        }
    }

    fun stopBrew() { coffeeMakerRepository.stopBrew() }

    // --- Configuration ---
    fun setSelectedCoffeeType(type: DrinkType) { coffeeMakerRepository.setSelectedCoffeeType(type) }
    fun setCustomStrength(strength: String) { coffeeMakerRepository.setCustomStrength(strength) }
    fun setCustomCoffeeShotSize(size: CoffeeShotSize) { coffeeMakerRepository.setCustomCoffeeShotSize(size) }
    fun setCustomMilkType(milkStyle: MilkStyle) { coffeeMakerRepository.setCustomMilkType(milkStyle) }
    fun setCustomTemperature(temperature: Temperature) { coffeeMakerRepository.setCustomTemperature(temperature) }

    fun updateBrewSettings(newSettings: BrewSettings) { coffeeMakerRepository.updateBrewSettings(newSettings) }
    fun setSelectedRecipe(drinkType: DrinkType, settings: BrewSettings?) {
        coffeeMakerRepository.setSelectedCoffeeType(drinkType)
        if (settings != null) {
            coffeeMakerRepository.updateBrewSettings(settings)
        }
    }
    fun setBrewSettingsFromFavorite(settings: BrewSettings, drinkName: String) {
        coffeeMakerRepository.setBrewSettingsFromFavorite(settings, drinkName)
    }

    // --- Maintenance / Refills ---
    fun addBeans() { coffeeMakerRepository.addBeans() }
    fun addWater() { coffeeMakerRepository.addWater() }
    fun emptyGroundsBin() { coffeeMakerRepository.emptyGroundsBin() }
    fun clearAlert() { coffeeMakerRepository.clearMaintenanceAlert() }

    // --- 🆕 SPECIFIC REFILL ACTIONS ---
    fun refillMilk(type: MilkBase) = coffeeMakerRepository.refillMilk(type)
    fun refillSyrup(type: SyrupType) = coffeeMakerRepository.refillSyrup(type)
    fun refillSugar(type: SugarType) = coffeeMakerRepository.refillSugar(type)
    fun refillTea(type: TeaType) = coffeeMakerRepository.refillTea(type)
    fun refillChocolate(type: ChocolateType) = coffeeMakerRepository.refillChocolate(type)
}