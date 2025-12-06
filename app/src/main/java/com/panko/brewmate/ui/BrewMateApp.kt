package com.panko.brewmate.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.panko.brewmate.AuthScreen
import com.panko.brewmate.data.AuthRepository
import com.panko.brewmate.navigation.BrewMateDestinations
import com.panko.brewmate.viewmodel.AuthViewModel
import com.panko.brewmate.viewmodel.CoffeeMakerViewModel
import com.panko.brewmate.util.LocalViewModelFactory
import com.panko.brewmate.viewmodel.FavoritesViewModel
import com.panko.brewmate.viewmodel.SchedulingViewModel
@Composable
fun BrewMateApp(
    coffeeMakerViewModel: CoffeeMakerViewModel,
    schedulingViewModel: SchedulingViewModel,
    favoritesViewModel: FavoritesViewModel,
    authViewModel: AuthViewModel,
    authRepository: AuthRepository,
    viewModelFactory: ViewModelProvider.Factory
) {
    // 1. Initial State Check
    val isLoggedIn = authRepository.getCurrentUserId() != null
    val startDestination = if (isLoggedIn) {
        BrewMateDestinations.MAIN_APP_GRAPH // Logged in, go to machine
    } else {
        BrewMateDestinations.AUTH_ROUTE // Not logged in, go to auth wall
    }

    val navController = rememberNavController()

    // 2. Custom CompositionLocal for ViewModel Factory (simplifies usage in child screens)
    CompositionLocalProvider(LocalViewModelFactory provides viewModelFactory) {

        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            // --- AUTHENTICATION ROUTE ---
            composable(BrewMateDestinations.AUTH_ROUTE) {
                AuthScreen(
                    authViewModel = authViewModel,
                    onAuthSuccess = {
                        // Navigate to the main app graph and clear history
                        navController.navigate(BrewMateDestinations.MAIN_APP_GRAPH) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }

            // --- MAIN APPLICATION GRAPH ---
            composable(BrewMateDestinations.MAIN_APP_GRAPH) {
                MainAppScaffold(
                    coffeeMakerViewModel = coffeeMakerViewModel,
                    schedulingViewModel = schedulingViewModel,
                    favoritesViewModel = favoritesViewModel,
                    onLogout = {
                        authViewModel.logout() // Clear Firebase session
                        // Navigate back to Auth screen and clear history
                        navController.navigate(BrewMateDestinations.AUTH_ROUTE) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}