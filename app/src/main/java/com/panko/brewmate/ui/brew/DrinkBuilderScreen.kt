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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrinkBuilderScreen(
    viewModel: CoffeeMakerViewModel,
    favoritesViewModel: FavoritesViewModel,
    navController: NavController,
    mode: BuilderMode = BuilderMode.BREW_NOW // Default to Brew Now
) {
    val customSettings by viewModel.customBrewSettings.collectAsState()
    val showSaveDialog = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()), // Makes it scrollable
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Design Your Drink", style = MaterialTheme.typography.headlineMedium)

        // Base Drink Selection
        Row(modifier = Modifier.fillMaxWidth()) {
            BaseDrinkType.entries.forEach { type ->
                val isSelected = customSettings.baseType == type
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        // Update the base type in ViewModel
                        viewModel.updateBrewSettings(customSettings.copy(baseType = type))
                    },
                    label = { Text(type.name.replace("_", " ")) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        Divider()

        // Dynamic Sections (depends on base type, like coffee or tea, or whether it has milk or not)

        when (customSettings.baseType) {
            BaseDrinkType.COFFEE -> {
                Text("Coffee Settings", style = MaterialTheme.typography.titleMedium)

                // Strength
                SimpleDropdown(
                    label = "Strength",
                    currentValue = customSettings.strength,
                    options = listOf("Mild", "Medium", "Bold"),
                    onOptionSelected = { viewModel.setCustomStrength(it) }
                )

                // Shot Size
                SimpleDropdown(
                    label = "Shot Size",
                    currentValue = customSettings.coffeeShotSize.displayName,
                    options = CoffeeShotSize.entries.map { it.displayName },
                    onOptionSelected = { selectedName ->
                        val size = CoffeeShotSize.entries.find {it.displayName == selectedName}
                                ?: CoffeeShotSize.SINGLE_SHOT
                            viewModel.setCustomCoffeeShotSize(size) }
                )

                // Decaf Toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = customSettings.isDecaf,
                        onCheckedChange = {
                            viewModel.updateBrewSettings(customSettings.copy(isDecaf = it))
                        }
                    )
                    Text("Decaf")
                }
            }

            BaseDrinkType.TEA -> {
                Text("Tea Settings", style = MaterialTheme.typography.titleMedium)

                // Tea Type
                SimpleDropdown(
                    label = "Tea Type",
                    currentValue = customSettings.teaType.displayName,
                    options = TeaType.entries.map { it.displayName },
                    onOptionSelected = { name ->
                        val type = TeaType.entries.find { it.displayName == name } ?: TeaType.BLACK
                        viewModel.updateBrewSettings(customSettings.copy(teaType = type))
                    }
                )

                // Steep Time Slider
                Text("Steep Time: ${customSettings.steepTime} seconds")
                Slider(
                    value = customSettings.steepTime.toFloat(),
                    onValueChange = {
                        viewModel.updateBrewSettings(customSettings.copy(steepTime = it.toLong()))
                    },
                    valueRange = 60f..600f, // 1 min to 10 mins
                    steps = 18
                )
            }

            BaseDrinkType.CHOCOLATE -> {
                Text("Chocolate Settings", style = MaterialTheme.typography.titleMedium)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    // Chocolate Type Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        SimpleDropdown(
                            label = "Chocolate Type",
                            currentValue = customSettings.chocolateType.displayName,
                            options = ChocolateType.entries.map { it.displayName },
                            onOptionSelected = { selectedName ->
                                val type = ChocolateType.entries.find { it.displayName == selectedName }
                                    ?: ChocolateType.MILK
                                viewModel.updateBrewSettings(customSettings.copy(chocolateType = type))
                            }
                        )
                    }

                    // Quantity Counter (tsp)
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Minus Button
                            IconButton(onClick = {
                                if (customSettings.chocolateTsp > 1)
                                    viewModel.updateBrewSettings(customSettings.copy(chocolateTsp = customSettings.chocolateTsp - 1))
                            }) { Text("-") }

                            // Bold Value Text
                            Text(
                                text = "${customSettings.chocolateTsp} tsp",
                                fontWeight = FontWeight.Bold
                            )

                            // Plus Button
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

        // Milk Section
        Text("Milk Customization", style = MaterialTheme.typography.titleMedium)

        // Milk base
        SimpleDropdown(
            label = "Milk Type",
            currentValue = customSettings.milkBase.displayName,
            options = MilkBase.entries.map { it.displayName },
            onOptionSelected = { name ->
                val base = MilkBase.entries.find { it.displayName == name } ?: MilkBase.WHOLE
                // Reset style to NONE if user selects NONE for base
                var newStyle = customSettings.milkStyle

                if (base != MilkBase.NONE && newStyle == MilkStyle.NONE) {
                    newStyle = MilkStyle.FOAMED
                } else if (base == MilkBase.NONE) {
                    newStyle = MilkStyle.NONE
                }
                viewModel.updateBrewSettings(customSettings.copy(milkBase = base, milkStyle = newStyle))
            }
        )

        // Milk style (Only show if Milk Base is NOT None)
        if (customSettings.milkBase != MilkBase.NONE) { // Dynamic Visibility
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

        // Syrup section
        Text("Syrups", style = MaterialTheme.typography.titleMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                SimpleDropdown(
                    label = "Flavor",
                    currentValue = customSettings.syrupType.displayName,
                    options = SyrupType.entries.map { it.displayName },
                    onOptionSelected = { name ->
                        val type = SyrupType.entries.find { it.displayName == name } ?: SyrupType.NONE
                        // Reset pumps to 0 if NONE is selected
                        val newPumps = if (type == SyrupType.NONE) 0 else 1
                        viewModel.updateBrewSettings(customSettings.copy(syrupType = type, syrupPumps = newPumps))
                    }
                )
            }

            // Pumps (Only show if Syrup is NOT None)
            if (customSettings.syrupType != SyrupType.NONE) { // Dynamic Visibility
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    // Simple Counter UI
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

        // Sugar section
        Text("Sugar", style = MaterialTheme.typography.titleMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                SimpleDropdown(
                    label = "Sugar Type",
                    currentValue = customSettings.sugarType.displayName,
                    options = SugarType.entries.map { it.displayName },
                    onOptionSelected = { name ->
                        val type =
                            SugarType.entries.find { it.displayName == name } ?: SugarType.NONE
                        val newSugarAmount = if (type == SugarType.NONE) 0 else 1
                        viewModel.updateBrewSettings(
                            customSettings.copy(
                                sugarType = type,
                                sugarAmount = newSugarAmount
                            )
                        )
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

        Text("Drink Temperature", style = MaterialTheme.typography.titleMedium)
        // Temperature
        SimpleDropdown(
            label = "Temperature",
            currentValue = customSettings.temperature.displayName,
            options = Temperature.entries.map { it.displayName },
            onOptionSelected = { selectedName ->
                val temperature = Temperature.entries.find {it.displayName == selectedName}
                    ?: Temperature.HOT
                viewModel.setCustomTemperature(temperature) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons
        if (mode == BuilderMode.BREW_NOW) {
            // From home screen, we want to brew coffee now
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button 1: Just Brew
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val smartName = "Custom ${customSettings.baseType.name.lowercase().capitalizeWords()}"
                        viewModel.startBrew(DrinkType.CUSTOM, customSettings, specificName = smartName)
                        navController.popBackStack(BrewMateDestinations.HOME_ROUTE, inclusive = false)
                    }
                ) {
                    Text("Brew Once")
                }

                // Button 2: Save & Brew
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { showSaveDialog.value = true }
                ) {
                    Text("Save & Brew")
                }
            }
        } else {
            // From favorites screen, we want to save the recipe
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showSaveDialog.value = true } // Opens dialog, but we change logic below
            ) {
                Text("Save to Favorites")
            }
        }
    }

    // Save Favorite Dialog
    if (showSaveDialog.value) {
        SaveFavoriteDialog(
            settings = customSettings,
            favoritesViewModel = favoritesViewModel,
            onDismiss = { showSaveDialog.value = false },
            onSaveComplete = { savedName ->
                if (mode == BuilderMode.BREW_NOW) {
                    // If brewing, start the machine
                    viewModel.startBrew(DrinkType.CUSTOM, customSettings, specificName = savedName)
                    navController.popBackStack(BrewMateDestinations.HOME_ROUTE, inclusive = false)
                } else {
                    // If just designing, go back to Favorites list
                    navController.popBackStack()
                }
            }
        )
    }
}

// Helper composables (dropdowns and such)

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
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
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

@Composable
fun SaveFavoriteDialog(
    settings: BrewSettings,
    favoritesViewModel: FavoritesViewModel,
    onDismiss: () -> Unit,
    onSaveComplete: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Name Your Favorite") },
        text = {
            Column {
                Text("Enter a name to save this recipe for later:")
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    favoritesViewModel.saveFavorite(name, settings)
                    onSaveComplete(name)
                    onDismiss()
                }
            ) {
                Text("Save & Brew")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun String.capitalizeWords(): String =
    split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }