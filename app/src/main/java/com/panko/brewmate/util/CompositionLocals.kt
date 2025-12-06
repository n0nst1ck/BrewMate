package com.panko.brewmate.util

import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.ViewModelProvider

/**
 * Custom CompositionLocal to provide the ViewModelProvider.Factory
 * down the Compose tree for ViewModel injection in screens.
 */
val LocalViewModelFactory = compositionLocalOf<ViewModelProvider.Factory> {
    error("No ViewModelProvider.Factory provided. Ensure BrewMateApp is called with the factory.")
}