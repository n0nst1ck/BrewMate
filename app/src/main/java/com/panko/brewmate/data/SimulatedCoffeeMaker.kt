package com.panko.brewmate.data

import com.panko.brewmate.model.BrewSettings
import com.panko.brewmate.model.CoffeeMakerState
import com.panko.brewmate.model.DrinkType
import com.panko.brewmate.model.CoffeeShotSize
import com.panko.brewmate.model.MilkStyle
import com.panko.brewmate.model.Temperature
import com.panko.brewmate.util.Scheduler // Import the Scheduler interface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class SimulatedCoffeeMaker(
    private val scheduler: Scheduler // Scheduler is injected here
) : CoffeeMakerRepository {

    // --- Internal MutableStateFlows for all states and settings ---
    private val _coffeeMakerState = MutableStateFlow(CoffeeMakerState(
        isPoweredOn = false,
        status = "Off",
        primaryMessage = "Press power button.",
        detailedMessage = "",
        canBrewDrink = false,
        canStopBrew = false,
        hasMaintenanceAlert = false // Add this to the state
    ))
    override val coffeeMakerState: StateFlow<CoffeeMakerState> = _coffeeMakerState.asStateFlow()

    private val _beansLevel = MutableStateFlow(100)
    override val beansLevel: StateFlow<Int> = _beansLevel.asStateFlow()

    private val _waterLevel = MutableStateFlow(100)
    override val waterLevel: StateFlow<Int> = _waterLevel.asStateFlow()

    private val _milkLevel = MutableStateFlow(100)
    override val milkLevel: StateFlow<Int> = _milkLevel.asStateFlow()

    private val _groundsBinLevel = MutableStateFlow(0)
    override val groundsBinLevel: StateFlow<Int> = _groundsBinLevel.asStateFlow()

    private val _selectedDrinkType = MutableStateFlow(DrinkType.ESPRESSO)
    override val selectedDrinkType: StateFlow<DrinkType> = _selectedDrinkType.asStateFlow()

    private val _customBrewSettings = MutableStateFlow(BrewSettings(
        strength = DrinkType.ESPRESSO.defaultStrength,
        coffeeShotSize = DrinkType.ESPRESSO.defaultCoffeeShotSize,
        milkStyle = DrinkType.ESPRESSO.defaultMilkStyle,
        temperature = DrinkType.ESPRESSO.defaultTemperature
    ))
    override val customBrewSettings: StateFlow<BrewSettings> = _customBrewSettings.asStateFlow()

    private var brewJob: Job? = null // Manages the brewing coroutine

    // --- Implementations of CoffeeMakerRepository functions ---

    override fun togglePower() {
        _coffeeMakerState.update { currentState ->
            val newIsPoweredOn = !currentState.isPoweredOn
            currentState.copy(
                isPoweredOn = newIsPoweredOn,
                status = if (newIsPoweredOn) "Ready" else "Off",
                detailedMessage = if (newIsPoweredOn) "Ready to brew" else "Press power button",
                canBrewDrink = newIsPoweredOn // Can start if turning on
            )
        }
        if (!_coffeeMakerState.value.isPoweredOn) {
            brewJob?.cancel() // Cancel any ongoing brew if turning off
            _coffeeMakerState.update { it.copy(canStopBrew = false) }
        }
    }

    override fun startBrew(drinkType: DrinkType, customSettings: BrewSettings?) {
        // Use a CoroutineScope to launch the brewing process asynchronously
        CoroutineScope(Dispatchers.Default).launch {
            // Pre-brew checks (simplified for now, can be expanded)
            if (!_coffeeMakerState.value.isPoweredOn) {
                _coffeeMakerState.update { it.copy(detailedMessage = "Error: Coffee maker is off.") }
                return@launch
            }
            if (_coffeeMakerState.value.canStopBrew) { // Already brewing
                _coffeeMakerState.update { it.copy(detailedMessage = "Error: Already brewing.") }
                return@launch
            }
            if (_coffeeMakerState.value.hasMaintenanceAlert) {
                _coffeeMakerState.update { it.copy(detailedMessage = "Error: Clear maintenance alert first.") }
                return@launch
            }

            // Determine effective settings (either preset or custom)
            val effectiveSettings = customSettings ?: BrewSettings(
                strength = drinkType.defaultStrength,
                coffeeShotSize = drinkType.defaultCoffeeShotSize,
                milkStyle = drinkType.defaultMilkStyle,
                temperature = drinkType.defaultTemperature
            )

            // Simulate resource checks and consumption
            val beansToConsume = 10 // Simplified consumption
            val waterToConsume = 15
            val milkToConsume = if (effectiveSettings.milkStyle != MilkStyle.NONE) 10 else 0

            if (_beansLevel.value < beansToConsume) {
                _coffeeMakerState.update { it.copy(status = "ERROR_BEANS_LOW", detailedMessage = "ALERT: Add beans!", hasMaintenanceAlert = true) }
                return@launch
            }
            if (_waterLevel.value < waterToConsume) {
                _coffeeMakerState.update { it.copy(status = "ERROR_WATER_LOW", detailedMessage = "ALERT: Add water!", hasMaintenanceAlert = true) }
                return@launch
            }
            if (_milkLevel.value < milkToConsume) {
                _coffeeMakerState.update { it.copy(status = "ERROR_MILK_LOW", detailedMessage = "ALERT: Add milk!", hasMaintenanceAlert = true) }
                return@launch
            }
            if (_groundsBinLevel.value >= 90) { // Near full
                _coffeeMakerState.update { it.copy(status = "ERROR_GROUNDS_FULL", detailedMessage = "ALERT: Empty grounds!", hasMaintenanceAlert = true) }
                return@launch
            }


            // Start the brewing process
            val displayName = drinkType.displayName

            // --- START BREW: Set the primary message for the whole process ---
            _coffeeMakerState.update {
                it.copy(
                    status = "Brewing...",
                    canBrewDrink = false,
                    canStopBrew = true,
                    primaryMessage = "Brewing your $displayName!", // Primary Message set here
                    detailedMessage = "Starting process..." // Initial detailed message
                )
            }
            _beansLevel.update { (it - beansToConsume).coerceAtLeast(0) }
            _waterLevel.update { (it - waterToConsume).coerceAtLeast(0) }
            _milkLevel.update { (it - milkToConsume).coerceAtLeast(0) }

            brewJob = launch {
                try {

                    // Simulate grinding
                    _coffeeMakerState.update {
                        it.copy(status = "Grinding beans...", detailedMessage = "Grinding beans...") }
                    delay(2000.milliseconds) // Non-blocking delay

                    // Simulate heating water
                    _coffeeMakerState.update { it.copy(status = "Heating water...", detailedMessage = "Heating water...") }
                    delay(2000.milliseconds)

                    // Simulate brewing
                    _coffeeMakerState.update { it.copy(status = "Brewing coffee...", detailedMessage = "Extracting coffee...") }
                    delay(3000.milliseconds)

                    // Simulate milk (if applicable)
                    if (effectiveSettings.milkStyle != MilkStyle.NONE) {
                        _coffeeMakerState.update { it.copy(status = "Preparing milk...", detailedMessage = "Preparing milk...") }
                        delay(2000.milliseconds)
                    }

                    // Simulate chilling (if applicable)
                    if (effectiveSettings.temperature == Temperature.COLD) { // Assuming COLD for iced
                        _coffeeMakerState.update { it.copy(status = "Chilling drink...", detailedMessage = "Adding ice...") }
                        delay(2000.milliseconds)
                    }

                    // Done
                    _coffeeMakerState.update { it.copy(status = "Done", detailedMessage = "${drinkType.displayName} is ready! Enjoy.") }
                    _groundsBinLevel.update { (it + 10).coerceAtMost(100) } // Increase grounds

                    scheduler.schedule(5000) { // Display "Done" for a few seconds
                        _coffeeMakerState.update { it.copy(status = "Ready", canBrewDrink = true, detailedMessage = "Ready to brew.") }
                    }
                } catch (e: Exception) {
                    _coffeeMakerState.update { it.copy(status = "Error", detailedMessage = "Brew failed: ${e.localizedMessage}", canBrewDrink = true, canStopBrew = false) }
                } finally {
                    _coffeeMakerState.update { it.copy(canStopBrew = false) } // Ensure stop button is disabled
                }
            }
        }
    }

    override fun stopBrew() {
        brewJob?.cancel() // Cancel the brewing job
        _coffeeMakerState.update { it.copy(status = "Stopped", canStopBrew = false, canBrewDrink = true, detailedMessage = "Brew stopped.") }
    }

    override fun setSelectedCoffeeType(type: DrinkType) {
        _selectedDrinkType.value = type
        if (type != DrinkType.CUSTOM) {
            // When a preset is selected, update custom settings to match
            _customBrewSettings.update {
                it.copy(
                    strength = type.defaultStrength,
                    coffeeShotSize = type.defaultCoffeeShotSize,
                    milkStyle = type.defaultMilkStyle,
                    temperature = type.defaultTemperature
                )
            }
        }
    }

    override fun setCustomStrength(strength: String) {
        _customBrewSettings.update { it.copy(strength = strength) }
        _selectedDrinkType.value = DrinkType.CUSTOM // Any manual change sets type to CUSTOM
    }

    override fun setCustomCoffeeShotSize(size: CoffeeShotSize) {
        _customBrewSettings.update { it.copy(coffeeShotSize = size) }
        _selectedDrinkType.value = DrinkType.CUSTOM
    }

    override fun setCustomMilkType(milkStyle: MilkStyle) {
        _customBrewSettings.update { it.copy(milkStyle = milkStyle) }
        _selectedDrinkType.value = DrinkType.CUSTOM
    }

    override fun setCustomTemperature(temperature: Temperature) {
        _customBrewSettings.update { it.copy(temperature = temperature) }
        _selectedDrinkType.value = DrinkType.CUSTOM
    }

    override fun addBeans() {
        _beansLevel.update { 100 }
        _coffeeMakerState.update { currentState ->
            if (currentState.status == "ERROR_BEANS_LOW") {
                currentState.copy(status = "Ready", detailedMessage = "Beans refilled.", hasMaintenanceAlert = false)
            } else currentState.copy(detailedMessage = "Beans refilled.")
        }
    }

    override fun addWater() {
        _waterLevel.update { 100 }
        _coffeeMakerState.update { currentState ->
            if (currentState.status == "ERROR_WATER_LOW") {
                currentState.copy(status = "Ready", detailedMessage = "Water refilled.", hasMaintenanceAlert = false)
            } else currentState.copy(detailedMessage = "Water refilled.")
        }
    }

    override fun addMilk() {
        _milkLevel.update { 100 }
        _coffeeMakerState.update { currentState ->
            if (currentState.status == "ERROR_MILK_LOW") {
                currentState.copy(status = "Ready", detailedMessage = "Milk refilled.", hasMaintenanceAlert = false)
            } else currentState.copy(detailedMessage = "Milk refilled.")
        }
    }

    override fun emptyGroundsBin() {
        _groundsBinLevel.update { 0 }
        _coffeeMakerState.update { currentState ->
            if (currentState.status == "ERROR_GROUNDS_FULL") {
                currentState.copy(status = "Ready", detailedMessage = "Grounds bin emptied.", hasMaintenanceAlert = false)
            } else currentState.copy(detailedMessage = "Grounds bin emptied.")
        }
    }

    override fun clearMaintenanceAlert() {
        _coffeeMakerState.update { currentState ->
            currentState.copy(
                status = "Ready",
                detailedMessage = "Alert cleared. Ready to brew.",
                hasMaintenanceAlert = false
            )
        }
    }

    override fun setBrewSettingsFromFavorite(settings: BrewSettings, drinkName: String) {

        // Accessing the private state flows is now possible:
        _customBrewSettings.update { settings }
        _selectedDrinkType.update { DrinkType.CUSTOM }

        _coffeeMakerState.update {
            it.copy(
                status = "Ready",
                detailedMessage = "Ready to brew your saved drink: $drinkName!",
                canBrewDrink = true
            )
        }
    }

    override fun updateBrewSettings(settings: BrewSettings) {
        _customBrewSettings.value = settings
    }
}