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
import com.panko.brewmate.navigation.BrewMateDestinations
import com.panko.brewmate.viewmodel.CoffeeMakerViewModel
import com.panko.brewmate.viewmodel.FavoritesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeBrewScreen(
    viewModel: CoffeeMakerViewModel,
    favoritesViewModel: FavoritesViewModel,
    navController: NavController
) {
    val customSettings by viewModel.customBrewSettings.collectAsState()
    val showSaveDialog = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()), // Make it scrollable!
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Design Your Drink", style = MaterialTheme.typography.headlineMedium)

        // --- 1. BASE DRINK SELECTOR ---
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

        // --- 2. DYNAMIC SECTIONS ---

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
                    currentValue = customSettings.coffeeShotSize.name,
                    options = CoffeeShotSize.entries.map { it.name },
                    onOptionSelected = { viewModel.setCustomCoffeeShotSize(CoffeeShotSize.valueOf(it)) }
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
                Text("Richness: Maximum", style = MaterialTheme.typography.bodyMedium)
                // Add specific chocolate settings here if needed
            }
        }

        Divider()

        // --- MILK SECTION ---
        Text("Milk Customization", style = MaterialTheme.typography.titleMedium)

        // 1. Milk Base (The Ingredient)
        SimpleDropdown(
            label = "Milk Type",
            currentValue = customSettings.milkBase.displayName,
            options = MilkBase.entries.map { it.displayName },
            onOptionSelected = { name ->
                val base = MilkBase.entries.find { it.displayName == name } ?: MilkBase.WHOLE
                // Reset style to NONE if user selects NONE for base
                val newStyle = if (base == MilkBase.NONE) MilkStyle.NONE else customSettings.milkStyle
                viewModel.updateBrewSettings(customSettings.copy(milkBase = base, milkStyle = newStyle))
            }
        )

        // 2. Milk Style (Only show if Milk Base is NOT None)
        if (customSettings.milkBase != MilkBase.NONE) { // <--- DYNAMIC VISIBILITY
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

        // --- SYRUP SECTION ---
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
            if (customSettings.syrupType != SyrupType.NONE) { // <--- DYNAMIC VISIBILITY
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

        // --- SUGAR SECTION ---
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
            currentValue = customSettings.temperature.name,
            options = Temperature.entries.map { it.name },
            onOptionSelected = { viewModel.setCustomTemperature(Temperature.valueOf(it)) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- ACTION BUTTONS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // BUTTON 1: BREW ONCE (Don't save to favorites)
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    // 1. Ensure the ViewModel has the latest custom settings (it should already)
                    viewModel.startBrew(DrinkType.CUSTOM, customSettings)
                    // 2. Return Home to start machine
                    navController.popBackStack(BrewMateDestinations.HOME_ROUTE, inclusive = false)
                }
            ) {
                Text("Brew Once")
            }

            // BUTTON 2: SAVE & BREW (Opens Dialog)
            Button(
                modifier = Modifier.weight(1f),
                onClick = { showSaveDialog.value = true }
            ) {
                Text("Save & Brew")
            }
        }
    }

    // --- SAVE DIALOG ---
    if (showSaveDialog.value) {
        SaveFavoriteDialog(
            settings = customSettings,
            favoritesViewModel = favoritesViewModel,
            onDismiss = { showSaveDialog.value = false },
            onSaveComplete = {
                // After saving, we also set it as the current recipe and go home
                viewModel.startBrew(DrinkType.CUSTOM, customSettings)
                navController.popBackStack(BrewMateDestinations.HOME_ROUTE, inclusive = false)
            }
        )
    }
}

// --- HELPER COMPOSABLES ---

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
    onSaveComplete: () -> Unit
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
                    onSaveComplete()
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