package com.panko.brewmate.model

enum class CoffeeShotSize {
    SINGLE_SHOT,
    DOUBLE_SHOT
}
enum class MilkType {
    NONE, FOAMED, STEAMED, WARM
}
enum class Temperature {
    HOT, COLD
}

enum class SyrupType(val displayName: String) {
    NONE("None"),
    VANILLA("Vanilla"),
    CARAMEL("Caramel"),
    CHOCOLATE("Chocolate"),
    HAZELNUT("Hazelnut"),
    PUMPKIN_SPICE("Pumpkin Spice")
}