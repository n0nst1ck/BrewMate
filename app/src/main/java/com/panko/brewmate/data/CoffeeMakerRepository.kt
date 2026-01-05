package com.panko.brewmate.data

import com.panko.brewmate.model.DrinkType
import kotlinx.coroutines.flow.StateFlow
import com.panko.brewmate.model.CoffeeMakerState
import com.panko.brewmate.model.CoffeeShotSize
import com.panko.brewmate.model.MilkStyle
import com.panko.brewmate.model.Temperature
import com.panko.brewmate.model.BrewSettings



// Interface for interacting with the coffee maker
interface CoffeeMakerRepository {
    // --- Device Status and General Info ---
    val coffeeMakerState: StateFlow<CoffeeMakerState> // Combines status, message, power, canBrew/Stop
    val selectedDrinkType: StateFlow<DrinkType> // The currently selected preset type
    val customBrewSettings: StateFlow<BrewSettings> // The detailed custom settings

    // --- Levels ---
    val beansLevel: StateFlow<Int>
    val waterLevel: StateFlow<Int>
    val milkLevel: StateFlow<Int>
    val groundsBinLevel: StateFlow<Int>

    // --- Commands ---
    fun togglePower()
    fun startBrew(drinkType: DrinkType, customSettings: BrewSettings?)
    fun stopBrew()
    fun setSelectedCoffeeType(type: DrinkType) // To set the main coffee type (e.g., Espresso, Latte)
    fun setCustomStrength(strength: String)
    fun setCustomCoffeeShotSize(size: CoffeeShotSize)
    fun setCustomMilkType(milkStyle: MilkStyle)
    fun setCustomTemperature(temperature: Temperature)
    fun addBeans()
    fun addWater()
    fun addMilk()
    fun emptyGroundsBin()
    fun clearMaintenanceAlert()
    fun setBrewSettingsFromFavorite(settings: BrewSettings, drinkName: String)
    fun updateBrewSettings(settings: BrewSettings)
}