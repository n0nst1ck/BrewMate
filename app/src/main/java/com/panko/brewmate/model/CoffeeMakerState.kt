package com.panko.brewmate.model

data class CoffeeMakerState(
    val isPoweredOn: Boolean,
    val status: String, // e.g., "Off", "Ready", "Brewing...", "ERROR_WATER_LOW"
    val primaryMessage: String,
    val detailedMessage: String,
    val canBrewDrink: Boolean,
    val canStopBrew: Boolean,
    val hasMaintenanceAlert: Boolean
)