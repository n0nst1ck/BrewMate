package com.panko.brewmate.ui.management

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.panko.brewmate.navigation.BrewMateDestinations
import com.panko.brewmate.viewmodel.FavoritesViewModel

@Composable
fun ManagementHubScreen(
    navController: NavController,
    favoritesViewModel: FavoritesViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Drink Management",
            style = MaterialTheme.typography.headlineMedium
        )

        // 1. Existing Presets/Selection Screen
        Button(
            onClick = { navController.navigate(BrewMateDestinations.CONFIGURE_BREW_TYPE_ROUTE) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select Preset Drink")
        }

        // 2. Existing Customization Form
        Button(
            onClick = { navController.navigate(BrewMateDestinations.CUSTOMIZE_BREW_ROUTE) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Edit Current Custom Brew")
        }

        // 3. NEW: Favorites Management Screen
        Button(
            onClick = { navController.navigate(BrewMateDestinations.FAVORITES_ROUTE) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Manage Saved Favorites")
        }
    }
}