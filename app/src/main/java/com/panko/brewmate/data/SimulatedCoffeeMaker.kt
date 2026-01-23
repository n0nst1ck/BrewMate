package com.panko.brewmate.data

import com.panko.brewmate.model.BaseDrinkType
import com.panko.brewmate.model.BrewSettings
import com.panko.brewmate.model.ChocolateType
import com.panko.brewmate.model.CoffeeMakerState
import com.panko.brewmate.model.DrinkType
import com.panko.brewmate.model.CoffeeShotSize
import com.panko.brewmate.model.MilkBase
import com.panko.brewmate.model.MilkStyle
import com.panko.brewmate.model.SugarType
import com.panko.brewmate.model.SyrupType
import com.panko.brewmate.model.TeaType
import com.panko.brewmate.model.Temperature
import com.panko.brewmate.util.Scheduler
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
    private val scheduler: Scheduler
) : CoffeeMakerRepository {

    // --- State & Inventory Flows ---
    private val _coffeeMakerState = MutableStateFlow(CoffeeMakerState(
        isPoweredOn = false,
        status = "Off",
        primaryMessage = "Press power button.",
        detailedMessage = "",
        canBrewDrink = false,
        canStopBrew = false,
        hasMaintenanceAlert = false
    ))
    override val coffeeMakerState: StateFlow<CoffeeMakerState> = _coffeeMakerState.asStateFlow()

    private val _beansLevel = MutableStateFlow(100)
    override val beansLevel: StateFlow<Int> = _beansLevel.asStateFlow()

    private val _waterLevel = MutableStateFlow(100)
    override val waterLevel: StateFlow<Int> = _waterLevel.asStateFlow()

    // Maps
    private val _milkLevels = MutableStateFlow(
        MilkBase.entries.filter { it != MilkBase.NONE }.associateWith { 100 }
    )
    override val milkLevels = _milkLevels.asStateFlow()

    private val _syrupLevels = MutableStateFlow(
        SyrupType.entries.filter { it != SyrupType.NONE }.associateWith { 100 }
    )
    override val syrupLevels = _syrupLevels.asStateFlow()

    private val _sugarLevels = MutableStateFlow(
        SugarType.entries.filter { it != SugarType.NONE }.associateWith { 100 }
    )
    override val sugarLevels = _sugarLevels.asStateFlow()

    private val _teaLevels = MutableStateFlow(
        TeaType.entries.associateWith { 100 }
    )
    override val teaLevels = _teaLevels.asStateFlow()

    private val _chocolateLevels = MutableStateFlow(
        ChocolateType.entries.associateWith { 100 }
    )
    override val chocolateLevels = _chocolateLevels.asStateFlow()

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

    private var brewJob: Job? = null

    // --- Actions ---

    override fun togglePower() {
        _coffeeMakerState.update { currentState ->
            val newIsPoweredOn = !currentState.isPoweredOn
            currentState.copy(
                isPoweredOn = newIsPoweredOn,
                status = if (newIsPoweredOn) "Ready" else "Off",
                detailedMessage = if (newIsPoweredOn) "Ready to brew" else "Press power button",
                canBrewDrink = newIsPoweredOn
            )
        }
        if (!_coffeeMakerState.value.isPoweredOn) {
            brewJob?.cancel()
            _coffeeMakerState.update { it.copy(canStopBrew = false) }
        }
    }

    override fun startBrew(drinkType: DrinkType, customSettings: BrewSettings?, drinkName: String?): Boolean {
        // --- 1. GENERAL CHECKS ---
        if (!_coffeeMakerState.value.isPoweredOn) {
            _coffeeMakerState.update { it.copy(detailedMessage = "Error: Coffee maker is off.") }
            return false
        }
        if (_coffeeMakerState.value.canStopBrew) {
            _coffeeMakerState.update { it.copy(detailedMessage = "Error: Already brewing.") }
            return false
        }
        if (_coffeeMakerState.value.hasMaintenanceAlert) {
            _coffeeMakerState.update { it.copy(detailedMessage = "Error: Clear maintenance alert first.") }
            return false
        }

        // --- 2. DEFINE REQUIREMENTS ---
        val effectiveSettings = if (customSettings != null) {
            customSettings
        } else if (drinkType == DrinkType.CUSTOM) {
            _customBrewSettings.value
        } else {
            BrewSettings(
                baseType = BaseDrinkType.COFFEE,
                strength = drinkType.defaultStrength,
                coffeeShotSize = drinkType.defaultCoffeeShotSize,
                milkStyle = drinkType.defaultMilkStyle,
                temperature = drinkType.defaultTemperature,

                // clearing out any old custom junk
                milkBase = if (drinkType.defaultMilkStyle != MilkStyle.NONE) MilkBase.WHOLE else MilkBase.NONE,
                syrupType = SyrupType.NONE,
                syrupPumps = 0,
                sugarType = SugarType.NONE,
                sugarAmount = 0,
                teaType = TeaType.BLACK,
                chocolateType = ChocolateType.MILK
            )
        }

        val waterNeeded = 15
        val beansNeeded = if (effectiveSettings.baseType == BaseDrinkType.COFFEE) 10 else 0
        val groundsSpaceNeeded = if (effectiveSettings.baseType == BaseDrinkType.COFFEE) 5 else 0
        val milkNeeded = if (effectiveSettings.milkBase != MilkBase.NONE) 10 else 0
        val teaNeeded = if (effectiveSettings.baseType == BaseDrinkType.TEA) 10 else 0
        val chocolateNeeded = if (effectiveSettings.baseType == BaseDrinkType.CHOCOLATE) effectiveSettings.chocolateTsp * 5 else 0
        val syrupNeeded = if (effectiveSettings.syrupType != SyrupType.NONE) effectiveSettings.syrupPumps * 5 else 0
        val sugarNeeded = if (effectiveSettings.sugarType != SugarType.NONE) effectiveSettings.sugarAmount * 5 else 0

        // --- 3. CHECK INVENTORY ---

        // A. Water
        if (_waterLevel.value < waterNeeded) {
            _coffeeMakerState.update { it.copy(status = "ERROR_WATER_LOW", detailedMessage = "ALERT: Add water!", hasMaintenanceAlert = true) }
            return false
        }

        // B. Beans
        if (beansNeeded > 0 && _beansLevel.value < beansNeeded) {
            _coffeeMakerState.update { it.copy(status = "ERROR_BEANS_LOW", detailedMessage = "ALERT: Add Coffee Beans!", hasMaintenanceAlert = true) }
            return false
        }

        // C. Grounds Bin
        if (groundsSpaceNeeded > 0 && _groundsBinLevel.value >= (100 - groundsSpaceNeeded)) {
            _coffeeMakerState.update { it.copy(status = "ERROR_GROUNDS_FULL", detailedMessage = "ALERT: Empty grounds bin!", hasMaintenanceAlert = true) }
            return false
        }

        // D. Milk
        if (milkNeeded > 0) {
            val currentMilk = _milkLevels.value[effectiveSettings.milkBase] ?: 0
            if (currentMilk < milkNeeded) {
                _coffeeMakerState.update { it.copy(
                    status = "ERROR_MILK_LOW",
                    detailedMessage = "ALERT: Low on ${effectiveSettings.milkBase.displayName}!",
                    hasMaintenanceAlert = true
                )}
                return false
            }
        }

        // E. Tea
        if (teaNeeded > 0) {
            val currentTea = _teaLevels.value[effectiveSettings.teaType] ?: 0
            if (currentTea < teaNeeded) {
                _coffeeMakerState.update { it.copy(
                    status = "ERROR_TEA_LOW",
                    detailedMessage = "ALERT: Refill ${effectiveSettings.teaType.displayName}!",
                    hasMaintenanceAlert = true
                )}
                return false
            }
        }

        // F. Chocolate
        if (chocolateNeeded > 0) {
            val currentChoco = _chocolateLevels.value[effectiveSettings.chocolateType] ?: 0
            if (currentChoco < chocolateNeeded) {
                _coffeeMakerState.update { it.copy(
                    status = "ERROR_CHOCO_LOW",
                    detailedMessage = "ALERT: Refill ${effectiveSettings.chocolateType.displayName}!",
                    hasMaintenanceAlert = true
                )}
                return false
            }
        }

        // G. Syrup
        if (syrupNeeded > 0) {
            val currentSyrup = _syrupLevels.value[effectiveSettings.syrupType] ?: 0
            if (currentSyrup < syrupNeeded) {
                _coffeeMakerState.update { it.copy(
                    status = "ERROR_SYRUP_LOW",
                    detailedMessage = "ALERT: Refill ${effectiveSettings.syrupType.displayName}!",
                    hasMaintenanceAlert = true
                )}
                return false
            }
        }

        // H. Sugar
        if (sugarNeeded > 0) {
            val currentSugar = _sugarLevels.value[effectiveSettings.sugarType] ?: 0
            if (currentSugar < sugarNeeded) {
                _coffeeMakerState.update { it.copy(
                    status = "ERROR_SUGAR_LOW",
                    detailedMessage = "ALERT: Refill ${effectiveSettings.sugarType.displayName}!",
                    hasMaintenanceAlert = true
                )}
                return false
            }
        }


        // --- 4. CONSUME RESOURCES (Update the Maps) ---

        // Basics
        _waterLevel.update { (it - waterNeeded).coerceAtLeast(0) }
        if (beansNeeded > 0) _beansLevel.update { (it - beansNeeded).coerceAtLeast(0) }

        // Milk
        if (milkNeeded > 0) {
            _milkLevels.update { map ->
                val current = map[effectiveSettings.milkBase] ?: 0
                map.toMutableMap().apply { this[effectiveSettings.milkBase] = (current - milkNeeded).coerceAtLeast(0) }
            }
        }

        // Tea
        if (teaNeeded > 0) {
            _teaLevels.update { map ->
                val current = map[effectiveSettings.teaType] ?: 0
                map.toMutableMap().apply { this[effectiveSettings.teaType] = (current - teaNeeded).coerceAtLeast(0) }
            }
        }

        // Chocolate
        if (chocolateNeeded > 0) {
            _chocolateLevels.update { map ->
                val current = map[effectiveSettings.chocolateType] ?: 0
                map.toMutableMap().apply { this[effectiveSettings.chocolateType] = (current - chocolateNeeded).coerceAtLeast(0) }
            }
        }

        // Syrup
        if (syrupNeeded > 0) {
            _syrupLevels.update { map ->
                val current = map[effectiveSettings.syrupType] ?: 0
                map.toMutableMap().apply { this[effectiveSettings.syrupType] = (current - syrupNeeded).coerceAtLeast(0) }
            }
        }

        // Sugar
        if (sugarNeeded > 0) {
            _sugarLevels.update { map ->
                val current = map[effectiveSettings.sugarType] ?: 0
                map.toMutableMap().apply { this[effectiveSettings.sugarType] = (current - sugarNeeded).coerceAtLeast(0) }
            }
        }

        // --- 5. START THE PROCESS ---
        val displayName = drinkName ?: if (drinkType == DrinkType.CUSTOM) {
            "Custom ${effectiveSettings.baseType.name.lowercase().capitalize()}"
        } else {
            drinkType.displayName
        }

        _coffeeMakerState.update {
            it.copy(
                status = "Starting...",
                canBrewDrink = false,
                canStopBrew = true,
                primaryMessage = "Making your $displayName!",
                detailedMessage = "Initializing..."
            )
        }

        brewJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                val brewSteps = getBrewSteps(effectiveSettings)

                for (step in brewSteps) {
                    _coffeeMakerState.update {
                        it.copy(status = step.message, detailedMessage = step.message)
                    }
                    delay(step.durationMs.milliseconds)
                }

                _coffeeMakerState.update {
                    it.copy(
                        status = "Done",
                        primaryMessage = "Enjoy!",
                        detailedMessage = "Your $displayName is ready."
                    )
                }

                _groundsBinLevel.update { (it + groundsSpaceNeeded).coerceAtMost(100) }

                delay(4000)
                _coffeeMakerState.update {
                    it.copy(status = "Ready", canBrewDrink = true, detailedMessage = "Ready to brew.", primaryMessage = "Ready")
                }

            } catch (e: Exception) {
                _coffeeMakerState.update {
                    it.copy(status = "Error", detailedMessage = "Brew failed: ${e.localizedMessage}", canBrewDrink = true)
                }
            } finally {
                _coffeeMakerState.update { it.copy(canStopBrew = false) }
            }
        }
        return true
    }


    override fun stopBrew() {
        brewJob?.cancel()
        _coffeeMakerState.update { it.copy(status = "Stopped", canStopBrew = false, canBrewDrink = true, detailedMessage = "Brew stopped.") }
    }

    override fun setSelectedCoffeeType(type: DrinkType) {
        _selectedDrinkType.value = type
        if (type != DrinkType.CUSTOM) {
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
        _selectedDrinkType.value = DrinkType.CUSTOM
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

    // --- Refill Functions ---
    override fun addBeans() {
        _beansLevel.update { 100 }
        if (_coffeeMakerState.value.status == "ERROR_BEANS_LOW") clearMaintenanceAlert()
    }

    override fun addWater() {
        _waterLevel.update { 100 }
        if (_coffeeMakerState.value.status == "ERROR_WATER_LOW") clearMaintenanceAlert()
    }

    override fun refillMilk(type: MilkBase) {
        _milkLevels.update { map -> map.toMutableMap().apply { this[type] = 100 } }
        if (_coffeeMakerState.value.status == "ERROR_MILK_LOW") clearMaintenanceAlert()
    }

    override fun refillSyrup(type: SyrupType) {
        _syrupLevels.update { map -> map.toMutableMap().apply { this[type] = 100 } }
        if (_coffeeMakerState.value.status == "ERROR_SYRUP_LOW") clearMaintenanceAlert()
    }

    override fun refillSugar(type: SugarType) {
        _sugarLevels.update { map -> map.toMutableMap().apply { this[type] = 100 } }
        if (_coffeeMakerState.value.status == "ERROR_SUGAR_LOW") clearMaintenanceAlert()
    }

    override fun refillTea(type: TeaType) {
        _teaLevels.update { map -> map.toMutableMap().apply { this[type] = 100 } }
        if (_coffeeMakerState.value.status == "ERROR_TEA_LOW") clearMaintenanceAlert()
    }

    override fun refillChocolate(type: ChocolateType) {
        _chocolateLevels.update { map -> map.toMutableMap().apply { this[type] = 100 } }
        if (_coffeeMakerState.value.status == "ERROR_CHOCO_LOW") clearMaintenanceAlert()
    }

    override fun emptyGroundsBin() {
        _groundsBinLevel.update { 0 }
        if (_coffeeMakerState.value.status == "ERROR_GROUNDS_FULL") clearMaintenanceAlert()
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

    // Helper
    private fun getBrewSteps(settings: BrewSettings): List<BrewStep> {
        val steps = mutableListOf<BrewStep>()

        // 1. PREPARATION
        when (settings.baseType) {
            BaseDrinkType.COFFEE -> {
                steps.add(BrewStep("Grinding beans (${settings.coffeeShotSize.displayName})...", 2500))
                steps.add(BrewStep("Tamping grounds...", 1000))
            }
            BaseDrinkType.TEA -> {
                steps.add(BrewStep("Heating water to optimal temp...", 2000))
                val steepTime = if (settings.steepTime > 0) settings.steepTime * 1000 else 3000
                steps.add(BrewStep("Steeping ${settings.teaType.displayName}...", steepTime))
            }
            BaseDrinkType.CHOCOLATE -> {
                val liquidName = if (settings.milkBase == MilkBase.NONE) "water" else settings.milkBase.displayName
                steps.add(BrewStep("Heating $liquidName to 80°C...", 4000))
                // Note: Using chocolateTsp here!
                steps.add(BrewStep("Mixing ${settings.chocolateTsp} tsp of ${settings.chocolateType.displayName}...", 3000))
                steps.add(BrewStep("Pouring creamy hot chocolate...", 3000))
            }
        }

        // 2. BREWING / POURING
        if (settings.baseType == BaseDrinkType.COFFEE) {
            steps.add(BrewStep("Extracting espresso...", 3000))
        } else if (settings.baseType == BaseDrinkType.TEA) {
            steps.add(BrewStep("Pouring tea...", 1500))
        }

        // 3. MILK
        if (settings.milkBase != MilkBase.NONE && settings.baseType != BaseDrinkType.CHOCOLATE) {
            val action = if (settings.milkStyle == MilkStyle.FOAMED) "Foaming" else "Steaming"
            steps.add(BrewStep("$action ${settings.milkBase.displayName}...", 2500))
            steps.add(BrewStep("Pouring milk...", 1500))
        }

        // 4. FLAVORINGS
        if (settings.syrupType != SyrupType.NONE) {
            steps.add(BrewStep("Adding ${settings.syrupPumps} pumps of ${settings.syrupType.displayName}...", 1500))
        }
        if (settings.sugarType != SugarType.NONE) {
            steps.add(BrewStep("Adding ${settings.sugarAmount} tsp of ${settings.sugarType.displayName}...", 1500))
        }

        return steps
    }
}

private data class BrewStep(val message: String, val durationMs: Long)