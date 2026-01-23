package com.panko.brewmate.ui.history

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.panko.brewmate.model.BrewHistoryItem
import com.panko.brewmate.viewmodel.FavoritesViewModel
import com.panko.brewmate.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    historyViewModel: HistoryViewModel,
    favoritesViewModel: FavoritesViewModel // Needed to save a favorite
) {
    val historyList by historyViewModel.historyList.collectAsState()
    val favorites by favoritesViewModel.favoriteDrinks.collectAsState()

    // State for the "Save Favorite" dialog
    var showDialog by remember { mutableStateOf(false) }
    var selectedHistoryItem by remember { mutableStateOf<BrewHistoryItem?>(null) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Brew History",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (historyList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No brews yet. Go make some coffee!", color = MaterialTheme.colorScheme.secondary)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(historyList, key = { it.id }) { item ->
                    // Check if this specific configuration exists in favorites
                    val isAlreadyFavorite = favorites.any { it.settings == item.settings }

                    HistoryItemCard(
                        item = item,
                        isAlreadyFavorite = isAlreadyFavorite,
                        onFavoriteClicked = {
                            if (isAlreadyFavorite) {
                                // Short toast for better UX
                                Toast.makeText(context, "Already in your favorites!", Toast.LENGTH_SHORT).show()
                            } else {
                                selectedHistoryItem = item
                                showDialog = true
                            }
                        }
                    )
                }
            }
        }
    }

    // Reuse the logic to save a favorite if an item is selected
    if (showDialog && selectedHistoryItem != null) {
        SaveHistoryAsFavoriteDialog(
            item = selectedHistoryItem!!,
            favoritesViewModel = favoritesViewModel,
            onDismiss = {
                showDialog = false
                selectedHistoryItem = null
            },
            onSuccess = {
                Toast.makeText(context, "Saved to favorites!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun HistoryItemCard(
    item: BrewHistoryItem,
    isAlreadyFavorite: Boolean,
    onFavoriteClicked: () -> Unit
) {
    // Format timestamp: "Mon, 10:30 AM"
    val dateString = remember(item.timestamp) {
        SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()).format(Date(item.timestamp))
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.drinkName, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${item.settings.strength} | ${item.settings.milkStyle.name.lowercase()}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // The "Heart" button to save this specific past brew
            IconButton(onClick = onFavoriteClicked) {
                Icon(
                    imageVector = if (isAlreadyFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Save to Favorites",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SaveHistoryAsFavoriteDialog(
    item: BrewHistoryItem,
    favoritesViewModel: FavoritesViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var name by remember { mutableStateOf(item.drinkName) } // Default to the original name
    val context = LocalContext.current // Need context for error toast

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save to Favorites") },
        text = {
            Column {
                Text("Name this recipe to save it for later:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    // Check the boolean return value!
                    val success = favoritesViewModel.saveFavorite(name, item.settings)

                    if (success) {
                        onSuccess()
                        onDismiss()
                    } else {
                        // Show error if it's a duplicate
                        Toast.makeText(context, "Duplicate recipe! You already have this.", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}