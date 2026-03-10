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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class SimulatedCoffeeMaker(
    private val scheduler: Scheduler,
    private val storage: InventoryStorage
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

    override suspend fun getMissingIngredients(settings: BrewSettings): List<String> {
        val missing = mutableListOf<String>()

        // 1. Calculate Needs (Exactly the same as startBrew)
        val waterNeeded = 15
        val beansNeeded = if (settings.baseType == BaseDrinkType.COFFEE) 10 else 0
        val groundsSpaceNeeded = if (settings.baseType == BaseDrinkType.COFFEE) 5 else 0
        val milkNeeded = if (settings.milkBase != MilkBase.NONE) 10 else 0
        val teaNeeded = if (settings.baseType == BaseDrinkType.TEA) 10 else 0
        val chocolateNeeded = if (settings.baseType == BaseDrinkType.CHOCOLATE) settings.chocolateTsp * 5 else 0
        val syrupNeeded = if (settings.syrupType != SyrupType.NONE) settings.syrupPumps * 5 else 0
        val sugarNeeded = if (settings.sugarType != SugarType.NONE) settings.sugarAmount * 5 else 0

        // 2. Check actual storage safely (using .first() waits for the database to load!)
        if (storage.waterLevel.first() < waterNeeded) missing.add("Water")

        if (beansNeeded > 0 && storage.beansLevel.first() < beansNeeded) missing.add("Coffee Beans")

        if (groundsSpaceNeeded > 0 && storage.groundsLevel.first() >= (100 - groundsSpaceNeeded)) missing.add("Empty Grounds Bin") // Remind them to empty it!

        if (milkNeeded > 0) {
            val currentMilk = storage.getLevel(storage.getMilkKey(settings.milkBase)).first()
            if (currentMilk < milkNeeded) missing.add(settings.milkBase.displayName)
        }

        if (teaNeeded > 0) {
            val currentTea = storage.getLevel(storage.getTeaKey(settings.teaType)).first()
            if (currentTea < teaNeeded) missing.add(settings.teaType.displayName)
        }

        if (chocolateNeeded > 0) {
            val currentChoco = storage.getLevel(storage.getChocolateKey(settings.chocolateType)).first()
            if (currentChoco < chocolateNeeded) missing.add(settings.chocolateType.displayName)
        }

        if (syrupNeeded > 0) {
            val currentSyrup = storage.getLevel(storage.getSyrupKey(settings.syrupType)).first()
            if (currentSyrup < syrupNeeded) missing.add(settings.syrupType.displayName)
        }

        if (sugarNeeded > 0) {
            val currentSugar = storage.getLevel(storage.getSugarKey(settings.sugarType)).first()
            if (currentSugar < sugarNeeded) missing.add(settings.sugarType.displayName)
        }

        return missing
    }

    init {
        // Load saved data on start up
        CoroutineScope(Dispatchers.IO).launch {
            // Load Essentials
            launch { storage.beansLevel.collect { _beansLevel.value = it } }
            launch { storage.waterLevel.collect { _waterLevel.value = it } }
            launch { storage.groundsLevel.collect { _groundsBinLevel.value = it } }

            // Load Maps (Loop through Enums and collect each one)

            // Milk
            MilkBase.entries.filter { it != MilkBase.NONE }.forEach { type ->
                launch {
                    storage.getLevel(storage.getMilkKey(type)).collect { savedLevel ->
                        _milkLevels.update { map -> map.toMutableMap().apply { this[type] = savedLevel } }
                    }
                }
            }

            // Syrup
            SyrupType.entries.filter { it != SyrupType.NONE }.forEach { type ->
                launch {
                    storage.getLevel(storage.getSyrupKey(type)).collect { savedLevel ->
                        _syrupLevels.update { map -> map.toMutableMap().apply { this[type] = savedLevel } }
                    }
                }
            }

            // Sugar
            SugarType.entries.filter { it != SugarType.NONE }.forEach { type ->
                launch {
                    storage.getLevel(storage.getSugarKey(type)).collect { lvl ->
                        _sugarLevels.update { map -> map.toMutableMap().apply { this[type] = lvl } }
                    }
                }
            }

            // Tea
            TeaType.entries.forEach { type ->
                launch {
                    storage.getLevel(storage.getTeaKey(type)).collect { lvl ->
                        _teaLevels.update { map -> map.toMutableMap().apply { this[type] = lvl } }
                    }
                }
            }

            // Chocolate
            ChocolateType.entries.forEach { type ->
                launch {
                    storage.getLevel(storage.getChocolateKey(type)).collect { lvl ->
                        _chocolateLevels.update { map -> map.toMutableMap().apply { this[type] = lvl } }
                    }
                }
            }
        }
    }

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
        val chocolateNeeded =
            if (effectiveSettings.baseType == BaseDrinkType.CHOCOLATE) effectiveSettings.chocolateTsp * 5 else 0
        val syrupNeeded =
            if (effectiveSettings.syrupType != SyrupType.NONE) effectiveSettings.syrupPumps * 5 else 0
        val sugarNeeded =
            if (effectiveSettings.sugarType != SugarType.NONE) effectiveSettings.sugarAmount * 5 else 0

        // --- 3. CHECK INVENTORY ---

        // A. Water
        if (_waterLevel.value < waterNeeded) {
            _coffeeMakerState.update {
                it.copy(
                    status = "ERROR_WATER_LOW",
                    detailedMessage = "ALERT: Add water!",
                    hasMaintenanceAlert = true
                )
            }
            return false
        }

        // B. Beans
        if (beansNeeded > 0 && _beansLevel.value < beansNeeded) {
            _coffeeMakerState.update {
                it.copy(
                    status = "ERROR_BEANS_LOW",
                    detailedMessage = "ALERT: Add Coffee Beans!",
                    hasMaintenanceAlert = true
                )
            }
            return false
        }

        // C. Grounds Bin
        if (groundsSpaceNeeded > 0 && _groundsBinLevel.value >= (100 - groundsSpaceNeeded)) {
            _coffeeMakerState.update {
                it.copy(
                    status = "ERROR_GROUNDS_FULL",
                    detailedMessage = "ALERT: Empty grounds bin!",
                    hasMaintenanceAlert = true
                )
            }
            return false
        }

        // D. Milk
        if (milkNeeded > 0) {
            val currentMilk = _milkLevels.value[effectiveSettings.milkBase] ?: 0
            if (currentMilk < milkNeeded) {
                _coffeeMakerState.update {
                    it.copy(
                        status = "ERROR_MILK_LOW",
                        detailedMessage = "ALERT: Low on ${effectiveSettings.milkBase.displayName}!",
                        hasMaintenanceAlert = true
                    )
                }
                return false
            }
        }

        // E. Tea
        if (teaNeeded > 0) {
            val currentTea = _teaLevels.value[effectiveSettings.teaType] ?: 0
            if (currentTea < teaNeeded) {
                _coffeeMakerState.update {
                    it.copy(
                        status = "ERROR_TEA_LOW",
                        detailedMessage = "ALERT: Refill ${effectiveSettings.teaType.displayName}!",
                        hasMaintenanceAlert = true
                    )
                }
                return false
            }
        }

        // F. Chocolate
        if (chocolateNeeded > 0) {
            val currentChoco = _chocolateLevels.value[effectiveSettings.chocolateType] ?: 0
            if (currentChoco < chocolateNeeded) {
                _coffeeMakerState.update {
                    it.copy(
                        status = "ERROR_CHOCO_LOW",
                        detailedMessage = "ALERT: Refill ${effectiveSettings.chocolateType.displayName}!",
                        hasMaintenanceAlert = true
                    )
                }
                return false
            }
        }

        // G. Syrup
        if (syrupNeeded > 0) {
            val currentSyrup = _syrupLevels.value[effectiveSettings.syrupType] ?: 0
            if (currentSyrup < syrupNeeded) {
                _coffeeMakerState.update {
                    it.copy(
                        status = "ERROR_SYRUP_LOW",
                        detailedMessage = "ALERT: Refill ${effectiveSettings.syrupType.displayName}!",
                        hasMaintenanceAlert = true
                    )
                }
                return false
            }
        }

        // H. Sugar
        if (sugarNeeded > 0) {
            val currentSugar = _sugarLevels.value[effectiveSettings.sugarType] ?: 0
            if (currentSugar < sugarNeeded) {
                _coffeeMakerState.update {
                    it.copy(
                        status = "ERROR_SUGAR_LOW",
                        detailedMessage = "ALERT: Refill ${effectiveSettings.sugarType.displayName}!",
                        hasMaintenanceAlert = true
                    )
                }
                return false
            }
        }


        // Consume Resources: update maps + storage

        // Basics

        CoroutineScope(Dispatchers.IO).launch {

            val newVal = (_waterLevel.value - waterNeeded).coerceAtLeast(0)
            storage.saveWater(newVal)

            if (beansNeeded > 0) {
                val newVal = (_beansLevel.value - beansNeeded).coerceAtLeast(0)
                storage.saveBeans(newVal)
            }

            // Milk
            if (milkNeeded > 0) {
                val current = _milkLevels.value[effectiveSettings.milkBase] ?: 0
                val newVal = (current - milkNeeded).coerceAtLeast(0)
                storage.saveItemLevel(storage.getMilkKey(effectiveSettings.milkBase), newVal)
            }

            // Tea
            if (teaNeeded > 0) {
                val current = _teaLevels.value[effectiveSettings.teaType] ?: 0
                val newVal = (current - teaNeeded).coerceAtLeast(0)
                storage.saveItemLevel(storage.getTeaKey(effectiveSettings.teaType), newVal)
            }

            // Chocolate
            if (chocolateNeeded > 0) {
                val current = _chocolateLevels.value[effectiveSettings.chocolateType] ?: 0
                val newVal = (current - chocolateNeeded).coerceAtLeast(0)
                storage.saveItemLevel(storage.getChocolateKey(effectiveSettings.chocolateType), newVal)
            }

            // Syrup
            if (syrupNeeded > 0) {
                val current = _syrupLevels.value[effectiveSettings.syrupType] ?: 0
                val newVal = (current - syrupNeeded).coerceAtLeast(0)
                storage.saveItemLevel(storage.getSyrupKey(effectiveSettings.syrupType), newVal)
            }

            // Sugar
            if (sugarNeeded > 0) {
                val current = _sugarLevels.value[effectiveSettings.sugarType] ?: 0
                val newVal = (current - sugarNeeded).coerceAtLeast(0)
                storage.saveItemLevel(storage.getSugarKey(effectiveSettings.sugarType), newVal)
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
        CoroutineScope(Dispatchers.IO).launch { storage.saveBeans(100) }
        clearErrorIfMatches("ERROR_BEANS_LOW")
    }

    override fun addWater() {
        CoroutineScope(Dispatchers.IO).launch { storage.saveWater(100) }
        clearErrorIfMatches("ERROR_WATER_LOW")
    }

    override fun emptyGroundsBin() {
        CoroutineScope(Dispatchers.IO).launch { storage.saveGrounds(0) }
        clearErrorIfMatches("ERROR_GROUNDS_FULL")
    }

    override fun refillMilk(type: MilkBase) {
        CoroutineScope(Dispatchers.IO).launch { storage.saveItemLevel(storage.getMilkKey(type), 100) }
        clearErrorIfMatches("ERROR_MILK_LOW")
    }

    // ... Implement refillSyrup, refillTea, etc. similarly ...
    override fun refillSyrup(type: SyrupType) {
        CoroutineScope(Dispatchers.IO).launch { storage.saveItemLevel(storage.getSyrupKey(type), 100) }
        clearErrorIfMatches("ERROR_SYRUP_LOW")
    }
    override fun refillSugar(type: SugarType) {
        CoroutineScope(Dispatchers.IO).launch {storage.saveItemLevel(storage.getSugarKey(type), 100) }
        clearErrorIfMatches("ERROR_SUGAR_LOW")
    }

    override fun refillTea(type: TeaType) {
        CoroutineScope(Dispatchers.IO).launch {storage.saveItemLevel(storage.getTeaKey(type), 100) }
        clearErrorIfMatches("ERROR_TEA_LOW")
    }

    override fun refillChocolate(type: ChocolateType) {
        CoroutineScope(Dispatchers.IO).launch {storage.saveItemLevel(storage.getChocolateKey(type), 100) }
        clearErrorIfMatches("ERROR_CHOCOLATE_LOW")
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
                if (settings.teaType == TeaType.MATCHA) {
                    steps.add(BrewStep("Sifting Matcha...", 1500))
                    steps.add(BrewStep("Whisking Matcha...", 2500))
                } else {
                    val steepTime = if (settings.steepTime > 0) settings.steepTime * 1000 else 3000
                    steps.add(BrewStep("Steeping ${settings.teaType.displayName}...", steepTime))
                }
            }
            BaseDrinkType.CHOCOLATE -> {
                val liquidName = if (settings.milkBase == MilkBase.NONE) "water" else settings.milkBase.displayName
                steps.add(BrewStep("Heating $liquidName to 80°C...", 4000))
                // Note: Using chocolateTsp here!
                steps.add(BrewStep("Mixing ${settings.chocolateTsp} tsp of ${settings.chocolateType.displayName}...", 3000))
                steps.add(BrewStep("Pouring creamy chocolate...", 3000))
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
            val milkName = settings.milkBase.displayName
            // We execute specific steps based on the type.
            // Cold has 1 step. Others have 2 steps (Prep + Pour).
            when {
                // Scenario A: Cold (Single Step - Just Pour)
                settings.milkStyle == MilkStyle.COLD -> {
                    steps.add(BrewStep("Pouring Cold $milkName...", 2000))
                }

                // Scenario B: Foamed (Froth -> Spoon Pour)
                settings.milkStyle == MilkStyle.FOAMED -> {
                    steps.add(BrewStep("Frothing $milkName...", 2500))
                    steps.add(BrewStep("Pouring $milkName Foam...", 1500))
                }

                // Scenario C: Steamed (Steam -> Pour)
                // This covers the standard "Hot Latte" case
                settings.milkStyle == MilkStyle.STEAMED && settings.temperature == Temperature.HOT -> {
                    steps.add(BrewStep("Steaming $milkName...", 2500))
                    steps.add(BrewStep("Pouring Steamed $milkName...", 1500))
                }

                // Scenario D: Warm (Warm -> Pour)
                // This covers Warm Milk or Kids' Temp drinks
                settings.milkStyle == MilkStyle.WARM -> {
                    steps.add(BrewStep("Gently Warming $milkName...", 2500))
                    steps.add(BrewStep("Pouring Warm $milkName...", 1500))
                }
            }
        }

        // 4. FLAVORINGS
        if (settings.syrupType != SyrupType.NONE) {
            steps.add(BrewStep("Adding ${settings.syrupPumps} pumps of ${settings.syrupType.displayName}...", 1500))
        }
        if (settings.sugarType != SugarType.NONE) {
            steps.add(BrewStep("Adding ${settings.sugarAmount} tsp of ${settings.sugarType.displayName}...", 1500))
        }

        // If the user requested a COLD drink, we finish by cooling it down.
        if (settings.temperature == Temperature.COLD) {
            steps.add(BrewStep("Adding Ice...", 1500))
            steps.add(BrewStep("Chilling Drink...", 1000))
        }

        return steps
    }

    private fun clearErrorIfMatches(targetStatus: String) {
        if (_coffeeMakerState.value.status == targetStatus) clearMaintenanceAlert()
    }
}

private data class BrewStep(val message: String, val durationMs: Long)