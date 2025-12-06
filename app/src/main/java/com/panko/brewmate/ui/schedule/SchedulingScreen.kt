package com.panko.brewmate.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import java.time.DayOfWeek
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Divider
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import com.panko.brewmate.viewmodel.CoffeeMakerViewModel
import com.panko.brewmate.viewmodel.FavoritesViewModel
import com.panko.brewmate.viewmodel.SchedulingViewModel

@Composable
fun RecurrenceSelector(
    selectedDays: Set<java.time.DayOfWeek>,
    onDaySelected: (java.time.DayOfWeek) -> Unit
){
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        java.time.DayOfWeek.entries.forEach { day ->
            val isSelected = selectedDays.contains(day)
            Button(
                onClick = { onDaySelected(day) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            ) {
                // Display just the first letter (M, T, W, Th, F, S, S)
                Text(text = day.name.first().toString())
            }
        }
    }
}

@Composable
fun SchedulingScreen(
    coffeeMakerViewModel: CoffeeMakerViewModel,
    schedulingViewModel: SchedulingViewModel,
    favoritesViewModel: FavoritesViewModel,
    navController: NavController) {

    // Placeholder for the list of currently scheduled brews
    val schedules by schedulingViewModel.scheduledBrews.collectAsState()

    // Placeholder for the form input state (Hour, Minute, Recurrence)
    val hour = remember { mutableStateOf(7) }
    val minute = remember { mutableStateOf(0) }
    val recurrenceDays = remember { mutableStateOf(setOf<DayOfWeek>()) }
    val isRecurrent = remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Scheduled Brews", style = MaterialTheme.typography.headlineMedium)

        // --- 1. Display Existing Schedules ---
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // items(schedules) { brew -> /* Display a card for each scheduled brew */ }
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        // --- 2. Schedule Form ---
        Text("New Schedule", style = MaterialTheme.typography.titleLarge)

        // A. Time Input (simplified text for now)
        Row {
            Text("Time: ${hour.value}:${minute.value.toString().padStart(2, '0')}")
            // Placeholder: Use a TimePicker Composable here later
        }

        // B. Recurrence Toggle
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Repeat Weekly?")
            Switch(checked = isRecurrent.value, onCheckedChange = { isRecurrent.value = it })
        }

        // C. Recurrence Day Selection (Only visible if isRecurrent is true)
        if (isRecurrent.value) {
            RecurrenceSelector(
                selectedDays = recurrenceDays.value,
                onDaySelected = { day ->
                    recurrenceDays.value = if (recurrenceDays.value.contains(day)) {
                        recurrenceDays.value - day
                    } else {
                        recurrenceDays.value + day
                    }
                }
            )
        }

        // D. Drink Selection (Simplified: just a button to select preset)
        Button(onClick = { /* TODO: Navigate to ConfigureBrewTypeScreen to select drink */ }) {
            Text("Select Drink Preset (e.g., Mocha)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // E. Save Button
        Button(
            onClick = {
                // TODO: Implement actual save logic
                // viewModel.saveSchedule(...)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Schedule Brew")
        }
    }
}