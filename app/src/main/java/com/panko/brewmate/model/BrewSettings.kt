package com.panko.brewmate.model

data class BrewSettings(
    val strength: String = "Μedium",
    val coffeeShotSize: CoffeeShotSize = CoffeeShotSize.SINGLE_SHOT,
    val milkType: MilkType = MilkType.NONE,
    val temperature: Temperature = Temperature.HOT,
    val isTea: Boolean = false,
    val steepTime: Long = 0, // time in ms for tea
    val isChocolate: Boolean = false,
    val syrupType: SyrupType = SyrupType.NONE,
    val syrupPumps: Int = 0,
    val sugarAmount: Int = 0
) {
    companion object {
        val DEFAULT = BrewSettings(
            strength = "Medium",
            coffeeShotSize = CoffeeShotSize.SINGLE_SHOT,
            milkType = MilkType.NONE,
            temperature = Temperature.HOT,
            isTea = false,
            steepTime = 0,
            isChocolate = false,
            syrupType = SyrupType.NONE,
            syrupPumps = 0,
            sugarAmount = 0
        )
    }
}