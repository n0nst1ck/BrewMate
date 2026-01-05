package com.panko.brewmate.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.panko.brewmate.ui.brew.ConfigureBrewTypeScreen
import com.panko.brewmate.ui.home.HomeScreen
import com.panko.brewmate.ui.levels.LevelsTabContent
import com.panko.brewmate.navigation.BrewMateDestinations
import com.panko.brewmate.navigation.NavItem
import com.panko.brewmate.ui.brew.CustomizeBrewScreen
import com.panko.brewmate.ui.schedule.SchedulingScreen
import com.panko.brewmate.viewmodel.CoffeeMakerViewModel
import com.panko.brewmate.viewmodel.FavoritesViewModel
import com.panko.brewmate.viewmodel.SchedulingViewModel
import com.panko.brewmate.ui.favorites.FavoritesScreen
import com.panko.brewmate.ui.history.HistoryScreen
import com.panko.brewmate.ui.settings.SettingsScreen
import com.panko.brewmate.viewmodel.AuthViewModel
import com.panko.brewmate.viewmodel.HistoryViewModel
import com.panko.brewmate.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    coffeeMakerViewModel: CoffeeMakerViewModel,
    schedulingViewModel: SchedulingViewModel,
    favoritesViewModel: FavoritesViewModel,
    themeViewModel: ThemeViewModel,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    historyViewModel: HistoryViewModel
) {
    val navController = rememberNavController()

    val navItems = listOf(
        NavItem(
            route = BrewMateDestinations.FAVORITES_ROUTE,
            icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorites") },
            label = "Favorites"
        ),
        NavItem(
            route = BrewMateDestinations.SCHEDULING_ROUTE,
            icon = { Icon(Icons.Filled.DateRange, contentDescription = "Schedule") },
            label = "Schedule"
        ),
        NavItem(
            route = BrewMateDestinations.HOME_ROUTE,
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = "Home"
        ),
        NavItem(
            route = BrewMateDestinations.HISTORY_ROUTE,
            icon = { Icon(Icons.Filled.History, contentDescription = "History") },
            label = "History"
        ),
        NavItem(
            route = BrewMateDestinations.INVENTORY_ROUTE,
            icon = { Icon(Icons.Filled.Inventory2, contentDescription = "Inventory") },
            label = "Inventory"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BrewMate") },
                actions = {
                    IconButton(
                        onClick = { navController.navigate(BrewMateDestinations.SETTINGS_ROUTE) }
                    ) {
                        // Import androidx.compose.material.icons.filled.Settings
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val currentRoute = navController.currentBackStackEntryFlow.collectAsState(initial = null)
                    .value?.destination?.route

                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = item.icon,
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            // The start destination can remain HOME_ROUTE
            startDestination = BrewMateDestinations.HOME_ROUTE,
            modifier = Modifier.padding(paddingValues)
        ) {
            // --- 1. BOTTOM BAR TAB DESTINATIONS ---

            composable(BrewMateDestinations.HOME_ROUTE) {
                HomeScreen(viewModel = coffeeMakerViewModel, navController = navController)
            }

            composable(BrewMateDestinations.INVENTORY_ROUTE) {
                LevelsTabContent(viewModel = coffeeMakerViewModel)
            }

            composable(BrewMateDestinations.SCHEDULING_ROUTE) {
                SchedulingScreen(
                    // Note: You should decide which VM handles the initial drink selection logic.
                    // For now, keep them all.
                    coffeeMakerViewModel = coffeeMakerViewModel,
                    schedulingViewModel = schedulingViewModel,
                    favoritesViewModel = favoritesViewModel,
                    navController = navController
                )
            }

            composable(BrewMateDestinations.SETTINGS_ROUTE) {
                SettingsScreen(
                    navController = navController,
                    themeViewModel = themeViewModel,
                    authViewModel = authViewModel,
                    onLogout = {
                        authViewModel.logout()
                     // Clear Firebase session
                            // Navigate back to Auth screen and clear history
                            navController.navigate(BrewMateDestinations.AUTH_ROUTE) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                    }
                )
            }

            // 2. THE NEW "DRINKS" MANAGEMENT HUB TAB
            // This is the new entry point for Customization, Presets, and Favorites.
            composable(BrewMateDestinations.FAVORITES_ROUTE) {
                FavoritesScreen(
                    navController = navController,
                    viewModel = favoritesViewModel
                )
            }


            // --- 3. INTERNAL FLOW/FORM DESTINATIONS (Accessed only from the Management Hub) ---

            composable(BrewMateDestinations.CONFIGURE_BREW_TYPE_ROUTE) {
                ConfigureBrewTypeScreen(
                    viewModel = coffeeMakerViewModel,
                    favoritesViewModel = favoritesViewModel,
                    navController = navController
                )
            }

            composable(BrewMateDestinations.CUSTOMIZE_BREW_ROUTE) {
                CustomizeBrewScreen(
                    viewModel = coffeeMakerViewModel,
                    favoritesViewModel = favoritesViewModel,
                    navController = navController
                )
            }

            composable(BrewMateDestinations.HISTORY_ROUTE) {
                HistoryScreen(
                    historyViewModel = historyViewModel,
                    favoritesViewModel = favoritesViewModel
                )
            }
        }
    }
}