package com.panko.brewmate.model

import java.io.Serializable

data class BrewSettings(
    // Base Identity
    val baseType: BaseDrinkType = BaseDrinkType.COFFEE,

    // Coffee Specifics
    val strength: String = "Medium",
    val coffeeShotSize: CoffeeShotSize = CoffeeShotSize.SINGLE_SHOT,
    val isDecaf: Boolean = false,

    // Tea Specifics
    val teaType: TeaType = TeaType.BLACK,
    val steepTime: Long = 180, // Default 3 mins (in seconds)

    // Chocolate Specifics
    val chocolateType: ChocolateType = ChocolateType.MILK,
    val chocolateTsp: Int = 1,

    // Common Additions
    val milkStyle: MilkStyle = MilkStyle.NONE,
    val milkBase: MilkBase = MilkBase.NONE,
    val temperature: Temperature = Temperature.HOT,

    // Syrups
    val syrupType: SyrupType = SyrupType.NONE,
    val syrupPumps: Int = 0,

    // Sugars
    val sugarType: SugarType = SugarType.NONE, // NEW: Type of sugar
    val sugarAmount: Int = 0 // Teaspoons/Packets
) : Serializable {
    companion object {
        val DEFAULT = BrewSettings()
    }
}