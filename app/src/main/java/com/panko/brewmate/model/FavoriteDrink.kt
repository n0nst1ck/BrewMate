// In model/FavoriteDrink.kt
package com.panko.brewmate.model

import java.util.UUID

data class FavoriteDrink(
    val id: String = UUID.randomUUID().toString(), // Firestore document ID
    val userId: String = "",
    val name: String = "", // e.g., "My Extra Shot Latte"
    val settings: BrewSettings = BrewSettings.DEFAULT // The full details of the drink
)