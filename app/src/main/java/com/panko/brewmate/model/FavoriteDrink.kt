package com.panko.brewmate.model

import java.util.UUID

data class FavoriteDrink(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val name: String = "",
    val settings: BrewSettings = BrewSettings.DEFAULT
)