// File: ui/brew/ConfigureBrewTypeScreen.kt
package com.panko.brewmate.ui.brew

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.runtime.remember
import com.panko.brewmate.model.*
import com.panko.brewmate.navigation.BrewMateDestinations
import com.panko.brewmate.viewmodel.CoffeeMakerViewModel
import com.panko.brewmate.viewmodel.FavoritesViewModel

@Composable
fun ConfigureBrewTypeScreen(
    viewModel: CoffeeMakerViewModel,
    favoritesViewModel: FavoritesViewModel,
    navController: NavController
) {
    val favoriteDrinks by favoritesViewModel.favoriteDrinks.collectAsState()
    // Define presets (excluding Custom)
    val presets = remember { DrinkType.entries.filter { it != DrinkType.CUSTOM } }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Choose Your Brew", style = MaterialTheme.typography.headlineMedium)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // 1. CUSTOM BREW BUTTON
            item {
                Button(
                    onClick = {
                        // Reset to defaults and go to customize
                        viewModel.setSelectedRecipe(DrinkType.CUSTOM, null)
                        navController.navigate(BrewMateDestinations.CUSTOMIZE_BREW_ROUTE)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Create New Custom Brew")
                }
            }

            // 2. FAVORITES LIST
            if (favoriteDrinks.isNotEmpty()) {
                item { Text("Favorites", style = MaterialTheme.typography.titleMedium) }
                items(favoriteDrinks) { favorite ->
                    ExpandableDrinkCard(
                        title = favorite.name,
                        details = "${favorite.settings.strength} | ${favorite.settings.milkStyle.name}",
                        onSelect = {
                            viewModel.setBrewSettingsFromFavorite(favorite.settings, favorite.name)
                            viewModel.startBrew(DrinkType.CUSTOM, favorite.settings)
                            navController.popBackStack(BrewMateDestinations.HOME_ROUTE, inclusive = false)
                        },
                        onEdit = {
                            // Load settings into ViewModel then navigate to edit
                            viewModel.setBrewSettingsFromFavorite(favorite.settings, favorite.name)
                            navController.navigate(BrewMateDestinations.CUSTOMIZE_BREW_ROUTE)
                        }
                    )
                }
            }

            // 3. PRESETS LIST
            item { Text("Presets", style = MaterialTheme.typography.titleMedium) }
            items(presets) { preset ->
                ExpandableDrinkCard(
                    title = preset.displayName,
                    details = "Default Settings",
                    onSelect = {
                        viewModel.startBrew(preset, null)
                        navController.popBackStack(BrewMateDestinations.HOME_ROUTE, inclusive = false)
                    },
                    onEdit = {
                        viewModel.setSelectedRecipe(preset, null)
                        navController.navigate(BrewMateDestinations.CUSTOMIZE_BREW_ROUTE)
                    }
                )
            }
        }
    }
}

// Helper Card Component with Edit Button
@Composable
fun ExpandableDrinkCard(
    title: String,
    details: String,
    onSelect: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelect // Clicking the card selects it immediately
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(details, style = MaterialTheme.typography.bodyMedium)
            }
            // The Edit Button
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit")
            }
        }
    }
}