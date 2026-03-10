package com.panko.brewmate.ui.brew

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.panko.brewmate.model.BaseDrinkType
import com.panko.brewmate.model.BrewSettings
import com.panko.brewmate.model.CoffeeShotSize
import com.panko.brewmate.model.DrinkType
import com.panko.brewmate.model.MilkBase
import com.panko.brewmate.model.MilkStyle
import com.panko.brewmate.model.TeaType
import com.panko.brewmate.model.Temperature
import com.panko.brewmate.model.SyrupType
import com.panko.brewmate.model.SugarType
import com.panko.brewmate.model.ChocolateType
import com.panko.brewmate.navigation.BrewMateDestinations
import com.panko.brewmate.viewmodel.CoffeeMakerViewModel
import com.panko.brewmate.viewmodel.FavoritesViewModel
import com.panko.brewmate.model.BuilderMode
import com.panko.brewmate.model.FavoriteDrink // 👈 Need this for the dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrinkBuilderScreen(
    viewModel: CoffeeMakerViewModel,
    favoritesViewModel: FavoritesViewModel,
    navController: NavController,
    mode: BuilderMode = BuilderMode.BREW_NOW,
    drinkIdToEdit: String? = null // 👈 NEW: Added parameter for Edit Mode
) {
    val customSettings by viewModel.customBrewSettings.collectAsState()
    val showSaveDialog = remember { mutableStateOf(false) }

    // 👇 NEW: State to hold the drink we are editing (if any)
    var favoriteToEdit by remember { mutableStateOf<FavoriteDrink?>(null) }

    // 👇 NEW: When the screen loads, check if we have an ID to edit
    LaunchedEffect(drinkIdToEdit) {
        if (drinkIdToEdit != null) {
            val favoritesList = favoritesViewModel.favoriteDrinks.value
            val drink = favoritesList.find { it.id == drinkIdToEdit }
            if (drink != null) {
                favoriteToEdit = drink
                // Populate the UI sliders/dropdowns with the saved settings
                viewModel.updateBrewSettings(drink.settings)
            }
        } else if (mode == BuilderMode.RECIPE_DESIGNER) {
            // Optional: If creating a NEW favorite, you might want to reset to defaults
            // so they don't accidentally start with the last coffee they brewed.
            viewModel.updateBrewSettings(BrewSettings.DEFAULT)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Change title based on mode
        val titleText = if (drinkIdToEdit != null) "Edit Recipe" else "Design Your Drink"
        Text(titleText, style = MaterialTheme.typography.headlineMedium)

        // --- Base Drink Selection ---
        Row(modifier = Modifier.fillMaxWidth()) {
            BaseDrinkType.entries.forEach { type ->
                val isSelected = customSettings.baseType == type
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.updateBrewSettings(customSettings.copy(baseType = type)) },
                    label = { Text(type.name.replace("_", " ")) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        Divider()

        // --- Dynamic Sections (Coffee, Tea, Chocolate) ---
        when (customSettings.baseType) {
            BaseDrinkType.COFFEE -> {
                Text("Coffee Settings", style = MaterialTheme.typography.titleMedium)

                SimpleDropdown(
                    label = "Strength",
                    currentValue = customSettings.strength,
                    options = listOf("Mild", "Medium", "Bold"),
                    onOptionSelected = { viewModel.setCustomStrength(it) }
                )

                SimpleDropdown(
                    label = "Shot Size",
                    currentValue = customSettings.coffeeShotSize.displayName,
                    options = CoffeeShotSize.entries.map { it.displayName },
                    onOptionSelected = { selectedName ->
                        val size = CoffeeShotSize.entries.find {it.displayName == selectedName} ?: CoffeeShotSize.SINGLE_SHOT
                        viewModel.setCustomCoffeeShotSize(size)
                    }
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = customSettings.isDecaf,
                        onCheckedChange = { viewModel.updateBrewSettings(customSettings.copy(isDecaf = it)) }
                    )
                    Text("Decaf")
                }
            }

            BaseDrinkType.TEA -> {
                Text("Tea Settings", style = MaterialTheme.typography.titleMedium)

                SimpleDropdown(
                    label = "Tea Type",
                    currentValue = customSettings.teaType.displayName,
                    options = TeaType.entries.map { it.displayName },
                    onOptionSelected = { name ->
                        val type = TeaType.entries.find { it.displayName == name } ?: TeaType.BLACK
                        viewModel.updateBrewSettings(customSettings.copy(teaType = type))
                    }
                )

                Text("Steep Time: ${customSettings.steepTime} seconds")
                Slider(
                    value = customSettings.steepTime.toFloat(),
                    onValueChange = { viewModel.updateBrewSettings(customSettings.copy(steepTime = it.toLong())) },
                    valueRange = 0f..600f,
                    steps = 18
                )
            }

            BaseDrinkType.CHOCOLATE -> {
                Text("Chocolate Settings", style = MaterialTheme.typography.titleMedium)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        SimpleDropdown(
                            label = "Chocolate Type",
                            currentValue = customSettings.chocolateType.displayName,
                            options = ChocolateType.entries.map { it.displayName },
                            onOptionSelected = { selectedName ->
                                val type = ChocolateType.entries.find { it.displayName == selectedName } ?: ChocolateType.MILK
                                viewModel.updateBrewSettings(customSettings.copy(chocolateType = type))
                            }
                        )
                    }

                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                if (customSettings.chocolateTsp > 1)
                                    viewModel.updateBrewSettings(customSettings.copy(chocolateTsp = customSettings.chocolateTsp - 1))
                            }) { Text("-") }

                            Text("${customSettings.chocolateTsp} tsp", fontWeight = FontWeight.Bold)

                            IconButton(onClick = {
                                if (customSettings.chocolateTsp < 6)
                                    viewModel.updateBrewSettings(customSettings.copy(chocolateTsp = customSettings.chocolateTsp + 1))
                            }) { Text("+") }
                        }
                    }
                }
            }
        }

        Divider()

        // --- Milk Section ---
        Text("Milk Customization", style = MaterialTheme.typography.titleMedium)

        SimpleDropdown(
            label = "Milk Type",
            currentValue = customSettings.milkBase.displayName,
            options = MilkBase.entries.map { it.displayName },
            onOptionSelected = { name ->
                val base = MilkBase.entries.find { it.displayName == name } ?: MilkBase.WHOLE
                var newStyle = customSettings.milkStyle

                if (base != MilkBase.NONE && newStyle == MilkStyle.NONE) {
                    newStyle = MilkStyle.FOAMED
                } else if (base == MilkBase.NONE) {
                    newStyle = MilkStyle.NONE
                }
                viewModel.updateBrewSettings(customSettings.copy(milkBase = base, milkStyle = newStyle))
            }
        )

        if (customSettings.milkBase != MilkBase.NONE) {
            Spacer(modifier = Modifier.height(8.dp))
            SimpleDropdown(
                label = "Preparation Style",
                currentValue = customSettings.milkStyle.displayName,
                options = MilkStyle.entries.filter { it != MilkStyle.NONE }.map { it.displayName },
                onOptionSelected = { name ->
                    val style = MilkStyle.entries.find { it.displayName == name } ?: MilkStyle.STEAMED
                    viewModel.updateBrewSettings(customSettings.copy(milkStyle = style))
                }
            )
        }

        Divider()

        // --- Syrup Section ---
        Text("Syrups", style = MaterialTheme.typography.titleMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                SimpleDropdown(
                    label = "Flavor",
                    currentValue = customSettings.syrupType.displayName,
                    options = SyrupType.entries.map { it.displayName },
                    onOptionSelected = { name ->
                        val type = SyrupType.entries.find { it.displayName == name } ?: SyrupType.NONE
                        val newPumps = if (type == SyrupType.NONE) 0 else 1
                        viewModel.updateBrewSettings(customSettings.copy(syrupType = type, syrupPumps = newPumps))
                    }
                )
            }

            if (customSettings.syrupType != SyrupType.NONE) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            if (customSettings.syrupPumps > 1)
                                viewModel.updateBrewSettings(customSettings.copy(syrupPumps = customSettings.syrupPumps - 1))
                        }) { Text("-") }
                        Text("${customSettings.syrupPumps} Pumps", fontWeight = FontWeight.Bold)
                        IconButton(onClick = {
                            viewModel.updateBrewSettings(customSettings.copy(syrupPumps = customSettings.syrupPumps + 1))
                        }) { Text("+") }
                    }
                }
            }
        }

        Divider()

        // --- Sugar Section ---
        Text("Sugar", style = MaterialTheme.typography.titleMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                SimpleDropdown(
                    label = "Sugar Type",
                    currentValue = customSettings.sugarType.displayName,
                    options = SugarType.entries.map { it.displayName },
                    onOptionSelected = { name ->
                        val type = SugarType.entries.find { it.displayName == name } ?: SugarType.NONE
                        val newSugarAmount = if (type == SugarType.NONE) 0 else 1
                        viewModel.updateBrewSettings(customSettings.copy(sugarType = type, sugarAmount = newSugarAmount))
                    }
                )
            }

            if (customSettings.sugarType != SugarType.NONE) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            if (customSettings.sugarAmount > 1)
                                viewModel.updateBrewSettings(customSettings.copy(sugarAmount = customSettings.sugarAmount - 1))
                        }) { Text("-")}
                        Text("${customSettings.sugarAmount} tsp", fontWeight = FontWeight.Bold)
                        IconButton(onClick = {
                            viewModel.updateBrewSettings(customSettings.copy(sugarAmount = customSettings.sugarAmount + 1))
                        }) { Text("+")}
                    }
                }
            }
        }

        Divider()

        // --- Temperature Section ---
        Text("Drink Temperature", style = MaterialTheme.typography.titleMedium)
        SimpleDropdown(
            label = "Temperature",
            currentValue = customSettings.temperature.displayName,
            options = Temperature.entries.map { it.displayName },
            onOptionSelected = { selectedName ->
                val temperature = Temperature.entries.find {it.displayName == selectedName} ?: Temperature.HOT
                viewModel.setCustomTemperature(temperature)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- Action Buttons ---
        if (mode == BuilderMode.BREW_NOW) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val smartName = "Custom ${customSettings.baseType.name.lowercase().capitalizeWords()}"
                        viewModel.startBrew(DrinkType.CUSTOM, customSettings, specificName = smartName)
                        navController.popBackStack(BrewMateDestinations.HOME_ROUTE, inclusive = false)
                    }
                ) { Text("Brew Once") }

                Button(modifier = Modifier.weight(1f), onClick = { showSaveDialog.value = true }) {
                    Text("Save & Brew")
                }
            }
        } else {
            // Recipe Designer Mode (Create OR Edit)
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showSaveDialog.value = true }
            ) {
                Text(if (drinkIdToEdit != null) "Update Recipe" else "Save to Favorites")
            }
        }
    }

    // --- Save/Update Dialog ---
    if (showSaveDialog.value) {
        SaveFavoriteDialog(
            settings = customSettings,
            existingDrink = favoriteToEdit, // 👈 Pass the existing drink to pre-fill the name
            favoritesViewModel = favoritesViewModel,
            mode = mode,
            onDismiss = { showSaveDialog.value = false },
            onSaveComplete = { savedName ->
                if (mode == BuilderMode.BREW_NOW) {
                    viewModel.startBrew(DrinkType.CUSTOM, customSettings, specificName = savedName)
                    navController.popBackStack(BrewMateDestinations.HOME_ROUTE, inclusive = false)
                } else {
                    navController.popBackStack()
                }
            }
        )
    }
}

// ... SimpleDropdown stays exactly the same ...

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDropdown(
    label: String,
    currentValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = currentValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.lowercase().capitalizeWords()) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

// 👇 ADDED: mode parameter
@Composable
fun SaveFavoriteDialog(
    settings: BrewSettings,
    existingDrink: FavoriteDrink?,
    favoritesViewModel: FavoritesViewModel,
    mode: BuilderMode, // 👈 NEW Parameter
    onDismiss: () -> Unit,
    onSaveComplete: (String) -> Unit
) {
    var name by remember(existingDrink) { mutableStateOf(existingDrink?.name ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val isEditing = existingDrink != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Update Favorite" else "Name Your Favorite") },
        text = {
            Column {
                Text(if (isEditing) "Update the name or settings for this recipe:" else "Enter a name to save this recipe for later:")
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMsg = null
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        if (errorMsg != null) Text(errorMsg!!, color = MaterialTheme.colorScheme.error)
                    }
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    if (isEditing) {
                        // UPDATE mode
                        val success = favoritesViewModel.updateFavorite(existingDrink!!.id, name, settings)
                        if (success) {
                            onSaveComplete(name)
                            onDismiss()
                        } else {
                            errorMsg = "Another recipe with these settings already exists!"
                        }
                    } else {
                        // CREATE mode
                        val success = favoritesViewModel.saveFavorite(name, settings)
                        if (success){
                            onSaveComplete(name)
                            onDismiss()
                        } else {
                            errorMsg = "This recipe already exists!"
                        }
                    }
                }
            ) {
                // 👇 NEW: Dynamic Button Text!
                Text(
                    if (isEditing) "Update Recipe"
                    else if (mode == BuilderMode.BREW_NOW) "Save & Brew"
                    else "Save Drink"
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }