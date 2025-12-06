package com.panko.brewmate.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
//import com.panko.brewmate.ui.brew.BrewDashboardContent
import com.panko.brewmate.navigation.BrewMateDestinations
import com.panko.brewmate.viewmodel.CoffeeMakerViewModel

@Composable
fun HomeScreen(viewModel: CoffeeMakerViewModel, navController: NavController) {
    val coffeeMakerState by viewModel.coffeeMakerState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BrewDashboardContent(
            coffeeMakerStatus = coffeeMakerState.status,
            coffeeMakerMessage = coffeeMakerState.detailedMessage,
            isPoweredOn = coffeeMakerState.isPoweredOn,
            canStartBrew = coffeeMakerState.canStartBrew,
            canStopBrew = coffeeMakerState.canStopBrew,
            onTogglePower = viewModel::togglePower,
            onStartBrewClicked = { navController.navigate(BrewMateDestinations.CONFIGURE_BREW_TYPE_ROUTE) },
            onStopBrew = viewModel::stopBrew
        )
    }
}

@Composable
fun BrewDashboardContent(
    coffeeMakerStatus: String,
    coffeeMakerMessage: String,
    isPoweredOn: Boolean,
    canStartBrew: Boolean,
    canStopBrew: Boolean,
    onTogglePower: () -> Unit,
    onStartBrewClicked: () -> Unit,
    onStopBrew: () -> Unit
) {
    val powerButtonText = if (isPoweredOn) "Turn Off" else "Turn On"
    val powerButtonColor = if (isPoweredOn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Status: $coffeeMakerStatus",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = coffeeMakerMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = onTogglePower,
            colors = ButtonDefaults.buttonColors(containerColor = powerButtonColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(powerButtonText)
        }
        Button(
            onClick = onStartBrewClicked,
            enabled = isPoweredOn && canStartBrew,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Brew")
        }
        Button(
            onClick = onStopBrew,
            enabled = isPoweredOn && canStopBrew,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Stop Brew")
        }
    }
}