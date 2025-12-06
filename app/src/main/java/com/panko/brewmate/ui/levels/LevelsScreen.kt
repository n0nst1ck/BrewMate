package com.panko.brewmate.ui.levels

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.panko.brewmate.viewmodel.CoffeeMakerViewModel

@Composable
fun LevelsTabContent(viewModel: CoffeeMakerViewModel) {
    val beansLevel by viewModel.beansLevel.collectAsState()
    val waterLevel by viewModel.waterLevel.collectAsState()
    val milkLevel by viewModel.milkLevel.collectAsState()
    val groundsBinLevel by viewModel.groundsBinLevel.collectAsState()
    val coffeeMakerState by viewModel.coffeeMakerState.collectAsState() // To check isPoweredOn for buttons

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Coffee Machine Levels", style = MaterialTheme.typography.headlineMedium)
        LevelIndicator(label = "Beans", level = beansLevel)
        LevelIndicator(label = "Water", level = waterLevel)
        LevelIndicator(label = "Milk", level = milkLevel)
        LevelIndicator(label = "Grounds Bin", level = groundsBinLevel)

        Spacer(modifier = Modifier.height(8.dp))

        // Action Buttons for Levels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(
                onClick = viewModel::addBeans,
                enabled = coffeeMakerState.isPoweredOn && !coffeeMakerState.canStopBrew // Only add when powered on and not brewing
            ) {
                Text("Add Beans")
            }
            Button(
                onClick = viewModel::addWater,
                enabled = coffeeMakerState.isPoweredOn && !coffeeMakerState.canStopBrew
            ) {
                Text("Add Water")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(
                onClick = viewModel::addMilk,
                enabled = coffeeMakerState.isPoweredOn && !coffeeMakerState.canStopBrew
            ) {
                Text("Add Milk")
            }
            Button(
                onClick = viewModel::emptyGroundsBin,
                enabled = coffeeMakerState.isPoweredOn && !coffeeMakerState.canStopBrew
            ) {
                Text("Empty Grounds")
            }
        }

        Spacer(modifier = Modifier.weight(1f)) // Pushes content up

        // Maintenance Alert Dialog (if active)
        if (coffeeMakerState.hasMaintenanceAlert) {
            val alertMessage = when (coffeeMakerState.status) {
                "ERROR_WATER_LOW" -> "Water tank is low! Please refill."
                "ERROR_GROUNDS_FULL" -> "Used coffee grounds bin is full! Please empty."
                "ERROR_MILK_LOW" -> "Milk reservoir is low! Please refill."
                "ERROR_BEANS_LOW" -> "Coffee bean hopper is low! Please add beans."
                else -> coffeeMakerState.detailedMessage // Fallback to general message
            }
            AlertDialog(
                onDismissRequest = { /* Cannot dismiss without clearing */ },
                title = { Text("Maintenance Required!") },
                text = { Text(alertMessage) },
                confirmButton = {
                    Button(onClick = viewModel::clearAlert) {
                        Text("Clear Alert / Refill")
                    }
                }
            )
        }
    }
}


@Composable
fun LevelIndicator(label: String, level: Int) {
    val animatedProgress by animateFloatAsState(
        targetValue = level / 100f,
        label = "level_progress_animation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(text = "$level%", style = MaterialTheme.typography.bodyLarge)
        }
        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = when {
                level < 20 -> MaterialTheme.colorScheme.error
                level < 50 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}