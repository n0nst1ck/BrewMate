package com.panko.brewmate.ui.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.panko.brewmate.model.FavoriteDrink
import com.panko.brewmate.viewmodel.FavoritesViewModel

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    navController: NavController
) {
    // Collect the real-time stream of favorites from Firestore
    val favoriteDrinks by viewModel.favoriteDrinks.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Your Saved Drinks (${favoriteDrinks.size})",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (favoriteDrinks.isEmpty()) {
            // 🚨 FIX: Centered Empty State UI 🚨

            // Use a separate Column that takes up all remaining space
            Column(
                modifier = Modifier.fillMaxSize(), // Take up all available space
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center // Center vertically
            ) {
                Icon(
                    Icons.Outlined.Coffee, // You may need to import this or use Icons.Filled.Restaurant
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Go to the 'Drinks' tab to customize a brew and save it.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            // --- Non-Empty List ---
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(favoriteDrinks, key = { it.id }) { drink ->
                    FavoriteDrinkCard(drink, viewModel)
                }
            }
        }
    }
}

// Sub-composable for displaying each favorite
@Composable
fun FavoriteDrinkCard(drink: FavoriteDrink, viewModel: FavoritesViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = drink.name,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Type: ${drink.settings.syrupType.displayName} / ${drink.settings.milkStyle.name.lowercase()}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Action Button - Delete
            IconButton(onClick = { viewModel.deleteFavorite(drink.id) }) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete Favorite",
                    tint = MaterialTheme.colorScheme.error
                )
            }

            // NOTE: You would add an Edit or Set as Current button here later
        }
    }
}