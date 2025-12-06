// File: ui/brew/ConfigureBrewTypeScreen.kt
package com.panko.brewmate.ui.brew

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.panko.brewmate.model.* // Import your models: DrinkType, BrewSettings, etc.
import com.panko.brewmate.navigation.BrewMateDestinations
import com.panko.brewmate.viewmodel.CoffeeMakerViewModel
import com.panko.brewmate.viewmodel.FavoritesViewModel
import com.panko.brewmate.util.capitalizeWords // Assuming you moved this helper function

@Composable
fun ConfigureBrewTypeScreen(
    viewModel: CoffeeMakerViewModel,
    favoritesViewModel: FavoritesViewModel,
    navController: NavController
) {
    val selectedDrink by viewModel.selectedDrinkType.collectAsState()
    val favoriteDrinks by favoritesViewModel.favoriteDrinks.collectAsState()

    // Filter out the CUSTOM type from the presets list
    val presetDrinks = DrinkType.entries.filter { it != DrinkType.CUSTOM }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Select Your Drink",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn {

            // --- SECTION 1: PRESETS (Label) ---
            item {
                Text(
                    "PRESETS",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            // --- Existing Logic: Display PRESET Drinks ---
            items(presetDrinks, key = { it.displayName }) { drinkType ->
                DrinkTypeCard(
                    drinkType = drinkType,
                    isSelected = selectedDrink == drinkType,
                    onClick = {
                        viewModel.setSelectedCoffeeType(drinkType)
                        if (drinkType == DrinkType.CUSTOM) {
                            navController.navigate(BrewMateDestinations.CUSTOMIZE_BREW_ROUTE)
                        } else {
                            // Select preset and navigate back to Home to brew
                            viewModel.startBrew(drinkType, null)
                            navController.popBackStack(BrewMateDestinations.HOME_ROUTE, inclusive = false)
                        }
                    }
                )
            }

            // --- SECTION 2: FAVORITES (Divider & Label) ---
            if (favoriteDrinks.isNotEmpty()) {
                item {
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    Text(
                        "MY FAVORITES",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            // --- NEW Logic: Display FAVORITE Drinks ---
            items(favoriteDrinks, key = { it.id }) { favorite ->
                FavoriteDrinkCard(
                    favorite = favorite,
                    onClick = {
                        // Select the Favorite, update state, and go home to brew
                        viewModel.setBrewSettingsFromFavorite(favorite.settings, favorite.name)
                        navController.popBackStack(BrewMateDestinations.HOME_ROUTE, inclusive = false)
                    }
                )
            }
        }
    }
}

// --- Reusable Composable for Presets (Restored Layout Logic) ---
@Composable
fun DrinkTypeCard(drinkType: DrinkType, isSelected: Boolean, onClick: () -> Unit) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(drinkType.displayName, style = MaterialTheme.typography.titleLarge)
            Text("Strength: ${drinkType.defaultStrength}", style = MaterialTheme.typography.bodyMedium)
            Text("Shot Size: ${drinkType.defaultCoffeeShotSize.name.replace("_", " ").capitalizeWords()}", style = MaterialTheme.typography.bodySmall)
            Text("Milk: ${drinkType.defaultMilkType.name.lowercase().capitalizeWords()}", style = MaterialTheme.typography.bodySmall)
        }
    }
}


// --- Reusable Composable for Favorites (New Layout) ---
@Composable
fun FavoriteDrinkCard(favorite: FavoriteDrink, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = favorite.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary // Highlight the favorite name
            )
            // Display key features of the saved recipe
            Text(
                "Strength: ${favorite.settings.strength} | Syrup: ${favorite.settings.syrupType.displayName}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Milk: ${favorite.settings.milkType.name.lowercase().capitalizeWords()} | Temp: ${favorite.settings.temperature.name}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}