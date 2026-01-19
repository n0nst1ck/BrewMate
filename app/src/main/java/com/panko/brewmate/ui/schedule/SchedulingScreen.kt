package com.panko.brewmate.ui.schedule

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.panko.brewmate.model.BrewSettings
import com.panko.brewmate.model.DrinkType
import com.panko.brewmate.model.ScheduledBrew
import com.panko.brewmate.viewmodel.CoffeeMakerViewModel
import com.panko.brewmate.viewmodel.FavoritesViewModel
import com.panko.brewmate.viewmodel.SchedulingViewModel
import java.time.DayOfWeek
import java.util.Locale

@Composable
fun RecurrenceSelector(
    selectedDays: Set<DayOfWeek>,
    onDaySelected: (DayOfWeek) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        DayOfWeek.values().forEach { day ->
            val isSelected = selectedDays.contains(day)

            // Visual tweak: Use FilterChip style logic for a cleaner look
            OutlinedButton(
                onClick = { onDaySelected(day) },
                shape = MaterialTheme.shapes.extraSmall,
                contentPadding = PaddingValues(0.dp),
                colors = if (isSelected) {
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                },
                modifier = Modifier.size(40.dp)
            ) {
                Text(
                    text = day.name.take(1),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulingScreen(
    schedulingViewModel: SchedulingViewModel,
    favoritesViewModel: FavoritesViewModel,
    coffeeMakerViewModel: CoffeeMakerViewModel,
    navController: NavController
) {
    // 1. Data from ViewModels
    val schedules by schedulingViewModel.scheduledBrews.collectAsState()
    val favorites by favoritesViewModel.favoriteDrinks.collectAsState()

    // 2. Permission Handling (Auto-ask on Android 13+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* We don't need to do anything if granted, it just works */ }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // 3. Form State
    val timeState = rememberTimePickerState(initialHour = 8, initialMinute = 0)
    val recurrenceDays = remember { mutableStateOf(setOf<DayOfWeek>()) }
    val isRecurrent = remember { mutableStateOf(false) }

    // Drink Selection State
    var selectedDrinkName by remember { mutableStateOf("Select Drink") }
    var selectedSettings by remember { mutableStateOf(BrewSettings.DEFAULT) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {

        Text(
            "Scheduled Brews",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // --- LIST OF SCHEDULES ---
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (schedules.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No alarms set. Time to wake up! ☕", color = Color.Gray)
                    }
                }
            }
            items(schedules) { schedule ->
                BrewScheduleCard(
                    schedule = schedule,
                    onDelete = { schedulingViewModel.deleteSchedule(schedule) }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // --- NEW SCHEDULE FORM ---
        Text("New Schedule", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // A. TIME INPUT
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TimeInput(state = timeState)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // B. DRINK SELECTION
        ExposedDropdownMenuBox(
            expanded = isDropdownExpanded,
            onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedDrinkName,
                onValueChange = {},
                readOnly = true,
                label = { Text("What to brew?") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors()
            )
            ExposedDropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false }
            ) {
                // Favorites Section
                if (favorites.isNotEmpty()) {
                    DropdownMenuItem(
                        text = { Text("★ YOUR FAVORITES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) },
                        onClick = {}
                    )
                    favorites.forEach { fav ->
                        DropdownMenuItem(
                            text = { Text(fav.name) },
                            onClick = {
                                selectedDrinkName = fav.name
                                selectedSettings = fav.settings
                                isDropdownExpanded = false
                            }
                        )
                    }
                    HorizontalDivider()
                }

                // Presets Section
                DropdownMenuItem(
                    text = { Text("PRESETS", style = MaterialTheme.typography.labelSmall, color = Color.Gray) },
                    onClick = {}
                )
                DrinkType.entries.filter { it != DrinkType.CUSTOM }.forEach { preset ->
                    DropdownMenuItem(
                        text = { Text(preset.displayName) },
                        onClick = {
                            selectedDrinkName = preset.displayName
                            selectedSettings = BrewSettings.DEFAULT
                            isDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // C. RECURRENCE TOGGLE
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Repeat Weekly?")
            }
            Switch(
                checked = isRecurrent.value,
                onCheckedChange = { isRecurrent.value = it }
            )
        }

        // D. RECURRENCE DAYS
        if (isRecurrent.value) {
            Spacer(modifier = Modifier.height(8.dp))
            RecurrenceSelector(
                selectedDays = recurrenceDays.value,
                onDaySelected = { day ->
                    val current = recurrenceDays.value
                    recurrenceDays.value = if (current.contains(day)) current - day else current + day
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // E. SAVE BUTTON
        Button(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = selectedDrinkName != "Select Drink",
            onClick = {
                val newSchedule = ScheduledBrew(
                    userID = "current_user",
                    brewSettings = selectedSettings,
                    drinkName = selectedDrinkName,
                    hour = timeState.hour,
                    minute = timeState.minute,
                    isRecurrent = isRecurrent.value,
                    recurrenceDays = recurrenceDays.value.toList()
                )

                schedulingViewModel.addSchedule(newSchedule)
                recurrenceDays.value = emptySet()
            }
        ) {
            Text("Set Alarm", fontSize = 16.sp)
        }
    }
}

// --- UPGRADED UI COMPONENT ---
@Composable
fun BrewScheduleCard(schedule: ScheduledBrew, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Time Column
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d", schedule.hour, schedule.minute),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (schedule.isRecurrent) {
                    Text("Weekly", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                } else {
                    Text("One-time", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 2. Info Column
            Column(modifier = Modifier.weight(1f)) {
                Text(schedule.drinkName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                if (schedule.isRecurrent && schedule.recurrenceDays.isNotEmpty()) {
                    val daysText = schedule.recurrenceDays.joinToString(" ") { it.name.take(1) }
                    Text(daysText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("Ready to brew", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }

            // 3. Delete Action
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Schedule",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}