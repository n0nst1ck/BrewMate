package com.panko.brewmate.model

enum class DrinkType(
    val displayName: String,
    val defaultStrength: String,
    val defaultCoffeeShotSize: CoffeeShotSize,
    val defaultMilkStyle: MilkStyle,
    val defaultTemperature: Temperature,
    val isCoffee: Boolean = true,
    val isTea: Boolean = false,
    val isChocolate: Boolean = false,
    val defaultSyrupType: SyrupType = SyrupType.NONE,
    val syrupPumps: Int = 0,
    val sugarAmount: Int = 0
) {
    ESPRESSO(
        isCoffee = true,
        isTea = false,
        isChocolate = false,
        displayName = "Espresso",
        defaultStrength = "Bold",
        defaultCoffeeShotSize = CoffeeShotSize.SINGLE_SHOT,
        defaultMilkStyle = MilkStyle.NONE,
        defaultTemperature = Temperature.HOT,
        defaultSyrupType = SyrupType.NONE,
        syrupPumps = 0,
        sugarAmount = 0
    ),
    AMERICANO(
        isCoffee = true,
        isTea = false,
        isChocolate = false,
        displayName = "Americano",
        defaultStrength = "Medium",
        defaultCoffeeShotSize = CoffeeShotSize.DOUBLE_SHOT,
        defaultMilkStyle = MilkStyle.NONE,
        defaultTemperature = Temperature.HOT,
        defaultSyrupType = SyrupType.NONE,
        syrupPumps = 0,
        sugarAmount = 0
    ),
    LATTE(
        isCoffee = true,
        isTea = false,
        isChocolate = false,
        displayName = "Latte",
        defaultStrength = "Medium",
        defaultCoffeeShotSize = CoffeeShotSize.SINGLE_SHOT,
        defaultMilkStyle = MilkStyle.STEAMED,
        defaultTemperature = Temperature.HOT,
        defaultSyrupType = SyrupType.NONE,
        syrupPumps = 0,
        sugarAmount = 0
    ),
    CAPPUCCINO(
        isCoffee = true,
        isTea = false,
        isChocolate = false,
        displayName = "Cappuccino",
        defaultStrength = "Bold",
        defaultCoffeeShotSize = CoffeeShotSize.SINGLE_SHOT,
        defaultMilkStyle = MilkStyle.FOAMED,
        defaultTemperature = Temperature.HOT,
        defaultSyrupType = SyrupType.NONE,
        syrupPumps = 0,
        sugarAmount = 0
    ),
    MOCHA(
        isCoffee = true,
        isTea = false,
        isChocolate = false,
        displayName = "Mocha",
        defaultStrength = "Medium",
        defaultCoffeeShotSize = CoffeeShotSize.SINGLE_SHOT,
        defaultMilkStyle = MilkStyle.STEAMED,
        defaultTemperature = Temperature.HOT,
        defaultSyrupType = SyrupType.CHOCOLATE,
        syrupPumps = 2,
        sugarAmount = 0
    ),
    CUSTOM(
        isCoffee = true,
        isTea = false,
        isChocolate = false,
        displayName = "Custom Coffee",
        defaultStrength = "Medium", // These defaults will be overwritten by actual custom selections
        defaultCoffeeShotSize = CoffeeShotSize.SINGLE_SHOT,
        defaultMilkStyle = MilkStyle.NONE,
        defaultTemperature = Temperature.HOT,
        syrupPumps = 0,
        sugarAmount = 0
    );
}