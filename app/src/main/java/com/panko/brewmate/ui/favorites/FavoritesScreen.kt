package com.panko.brewmate.ui.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.panko.brewmate.model.DrinkType
import com.panko.brewmate.model.FavoriteDrink
import com.panko.brewmate.navigation.BrewMateDestinations
import com.panko.brewmate.viewmodel.CoffeeMakerViewModel
import com.panko.brewmate.viewmodel.FavoritesViewModel

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    coffeeMakerViewModel: CoffeeMakerViewModel,
    navController: NavController
) {
    val favoriteDrinks by viewModel.favoriteDrinks.collectAsState()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    navController.navigate(BrewMateDestinations.CREATE_FAVORITE_ROUTE)
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "Create New") },
                text = { Text("New Recipe") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "My Favorite Drinks",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (favoriteDrinks.isEmpty()) {
                // --- Empty State ---
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Outlined.Coffee,
                        contentDescription = "Empty Favorites",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Recipes Saved Yet!",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap the button below to invent one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            } else {
                // --- Non-Empty List ---
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(favoriteDrinks, key = { it.id }) { drink ->
                        FavoriteDrinkCard(
                            drink = drink,
                            onDelete = { viewModel.deleteFavorite(drink.id) },
                            onBrew = {
                                coffeeMakerViewModel.startBrew(DrinkType.CUSTOM, drink.settings, specificName = drink.name)
                                navController.navigate(BrewMateDestinations.HOME_ROUTE) {
                                    popUpTo(BrewMateDestinations.HOME_ROUTE) { inclusive = true }
                                }
                            },
                            // 👇 NEW: Pass the edit action down to the card
                            onEdit = {
                                // Assuming your NavGraph is set up to accept an ID for editing.
                                // If you handle passing data via a ViewModel instead, you'd call
                                // viewModel.setDrinkToEdit(drink) here before navigating.
                                navController.navigate("${BrewMateDestinations.CREATE_FAVORITE_ROUTE}/${drink.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}

// 👇 NEW: Added 'onClick' behavior to the Card and 'onEdit' to the parameters
@OptIn(ExperimentalMaterial3Api::class) // Required for clickable Card in some Material3 versions
@Composable
fun FavoriteDrinkCard(
    drink: FavoriteDrink,
    onDelete: () -> Unit,
    onBrew: () -> Unit,
    onEdit: () -> Unit // 👈 Added parameter
) {
    Card(
        onClick = onEdit, // 👈 This makes the entire card clickable with a nice ripple effect!
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Text Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = drink.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                val type = drink.settings.baseType.name.lowercase().capitalize()
                val details = if(drink.settings.syrupType.name != "NONE")
                    "with ${drink.settings.syrupType.displayName}" else "Classic"

                Text(
                    text = "$type • $details",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Right: Actions (Brew & Delete)
            Row(verticalAlignment = Alignment.CenterVertically) {

                // 1. Brew Button (Play)
                FilledTonalButton(
                    onClick = { onBrew() },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Brew", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Brew")
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 2. Delete Button
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete Favorite",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

private fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }