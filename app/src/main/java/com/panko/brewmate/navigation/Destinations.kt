package com.panko.brewmate.navigation

import androidx.compose.runtime.Composable

object BrewMateDestinations {
    const val AUTH_ROUTE = "auth"
    const val MAIN_APP_GRAPH = "main_app_graph"

    const val HOME_ROUTE = "home"
    const val CONFIGURE_BREW_TYPE_ROUTE = "configure_brew_type"
    const val CUSTOMIZE_BREW_ROUTE = "customize_brew"
    const val LEVELS_ROUTE = "levels"
    const val SCHEDULING_ROUTE = "scheduling"
    const val FAVORITES_ROUTE = "favorites_list"
    const val MANAGEMENT_HUB_ROUTE = "management_hub"
}

// Data class for navigation items (unchanged)
data class NavItem(
    val route: String,
    val icon: @Composable () -> Unit,
    val label: String
)