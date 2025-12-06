package com.panko.brewmate.model

import java.util.UUID

data class BrewHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val timestamp: Long = System.currentTimeMillis(), // Auto-sets to "now"
    val drinkName: String = "", // e.g., "Latte", "My Custom Brew"
    val settings: BrewSettings = BrewSettings.DEFAULT // Stores the full recipe
)