package com.panko.brewmate.ui.levels

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.panko.brewmate.viewmodel.CoffeeMakerViewModel

@Composable
fun InventoryScreen(viewModel: CoffeeMakerViewModel) {
    // 1. Collect ALL the data
    val beansLevel by viewModel.beansLevel.collectAsState()
    val waterLevel by viewModel.waterLevel.collectAsState()
    val groundsLevel by viewModel.groundsBinLevel.collectAsState()

    // Collect the new Maps
    val milks by viewModel.milkLevels.collectAsState()
    val syrups by viewModel.syrupLevels.collectAsState()
    val teas by viewModel.teaLevels.collectAsState()
    val chocolates by viewModel.chocolateLevels.collectAsState()
    val sugars by viewModel.sugarLevels.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text("Inventory Management", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        // --- SECTION 1: ESSENTIALS ---
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Machine Essentials", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    LevelIndicator(label = "Coffee Beans", level = beansLevel, onRefill = viewModel::addBeans)
                    LevelIndicator(label = "Water Tank", level = waterLevel, onRefill = viewModel::addWater)
                    LevelIndicator(label = "Grounds Bin (Used)", level = groundsLevel, isWaste = true, onRefill = viewModel::emptyGroundsBin)
                }
            }
        }

        // --- SECTION 2: TEAS ---
        item {
            InventorySection(title = "Tea Selection", items = teas, onRefill = { viewModel.refillTea(it) })
        }

        // --- SECTION 3: MILKS ---
        item {
            InventorySection(title = "Milk Fridge", items = milks, onRefill = { viewModel.refillMilk(it) })
        }

        // --- SECTION 4: CHOCOLATES ---
        item {
            InventorySection(title = "Chocolates", items = chocolates, onRefill = { viewModel.refillChocolate(it) })
        }

        // --- SECTION 5: SYRUPS ---
        item {
            InventorySection(title = "Syrups & Flavors", items = syrups, onRefill = { viewModel.refillSyrup(it) })
        }

        // --- SECTION 6: SUGARS ---
        item {
            InventorySection(title = "Sugars & Sweeteners", items = sugars, onRefill = { viewModel.refillSugar(it) })
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

// --- HELPER 1: Generic Section for Maps ---
@Composable
fun <T> InventorySection(
    title: String,
    items: Map<T, Int>,
    onRefill: (T) -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            // Loop through the map and draw a row for each item
            items.forEach { (type, level) ->
                // Clean up the enum name (e.g., PUMPKIN_SPICE -> Pumpkin Spice)
                val label = (type as? Enum<*>)?.name?.replace("_", " ")?.lowercase()?.capitalizeWords() ?: "Unknown"

                LevelIndicator(label = label, level = level, onRefill = { onRefill(type) })
            }
        }
    }
}

// --- HELPER 2: Single Row Indicator ---
@Composable
fun LevelIndicator(
    label: String,
    level: Int,
    isWaste: Boolean = false,
    onRefill: () -> Unit
) {
    val animatedProgress by animateFloatAsState(targetValue = level / 100f, label = "progress")
    val color = if (isWaste) {
        if (level > 80) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
    } else {
        if (level < 20) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Percentage Text
                Text(text = "$level%", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))

                // Small Refill Button (+)
                FilledTonalIconButton(
                    onClick = onRefill,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Refill", modifier = Modifier.size(16.dp))
                }
            }
        }

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().height(6.dp).padding(top = 4.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

// Helper for capitalization
fun String.capitalizeWords(): String =
    split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }