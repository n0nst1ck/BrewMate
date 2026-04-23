package com.panko.brewmate.data

import com.panko.brewmate.model.DrinkType
import kotlinx.coroutines.flow.StateFlow
import com.panko.brewmate.model.CoffeeMakerState
import com.panko.brewmate.model.CoffeeShotSize
import com.panko.brewmate.model.MilkStyle
import com.panko.brewmate.model.Temperature
import com.panko.brewmate.model.BrewSettings
import com.panko.brewmate.model.ChocolateType
import com.panko.brewmate.model.MilkBase
import com.panko.brewmate.model.SugarType
import com.panko.brewmate.model.SyrupType
import com.panko.brewmate.model.TeaType


// Interface for interacting with the coffee maker
interface CoffeeMakerRepository {
    // Device Status and General Info
    val coffeeMakerState: StateFlow<CoffeeMakerState> // Combines status, message, power, canBrew/Stop
    val selectedDrinkType: StateFlow<DrinkType> // The currently selected preset type
    val customBrewSettings: StateFlow<BrewSettings> // The detailed custom settings

    // Levels
    val beansLevel: StateFlow<Int>
    val waterLevel: StateFlow<Int>
    val groundsBinLevel: StateFlow<Int>

    val milkLevels: StateFlow<Map<MilkBase, Int>>
    val syrupLevels: StateFlow<Map<SyrupType, Int>>
    val sugarLevels: StateFlow<Map<SugarType, Int>>
    val teaLevels: StateFlow<Map<TeaType, Int>>
    val chocolateLevels: StateFlow<Map<ChocolateType, Int>>

    // Commands
    fun togglePower()
    fun startBrew(drinkType: DrinkType, customSettings: BrewSettings?, drinkName: String? = null): Boolean
    fun stopBrew()
    fun setSelectedCoffeeType(type: DrinkType)
    fun setCustomStrength(strength: String)
    fun setCustomCoffeeShotSize(size: CoffeeShotSize)
    fun setCustomMilkType(milkStyle: MilkStyle)
    fun setCustomTemperature(temperature: Temperature)
    fun addBeans()
    fun addWater()
    fun refillMilk(type: MilkBase)
    fun refillSyrup(type: SyrupType)
    fun refillSugar(type: SugarType)
    fun refillTea(type: TeaType)
    fun refillChocolate(type: ChocolateType)
    fun emptyGroundsBin()
    fun clearMaintenanceAlert()
    fun setBrewSettingsFromFavorite(settings: BrewSettings, drinkName: String)
    fun updateBrewSettings(settings: BrewSettings)

    suspend fun getMissingIngredients(settings: BrewSettings): List<String>
}