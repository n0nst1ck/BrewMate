package com.panko.brewmate.model

enum class CoffeeShotSize(val displayName: String) {
    SINGLE_SHOT("Single Shot"),
    DOUBLE_SHOT("Double Shot"),
    TRIPLE_SHOT("Triple Shot"),
    QUADRUPLE_SHOT("Quadruple Shot")
}
enum class MilkStyle(val displayName: String) {
    FOAMED("Foamed"),
    STEAMED("Steamed"),
    WARM("Warm"),
    COLD("Cold"),
    NONE("None")
}

enum class MilkBase(val displayName: String) {
    WHOLE("Whole Milk"),
    SKIM("Skim Milk"),
    OAT("Oat Milk"),
    ALMOND("Almond Milk"),
    SOY("Soy Milk"),
    NONE("None")
}

enum class Temperature(val displayName: String) {
    HOT("Hot"),
    COLD("Cold")
}

enum class SyrupType(val displayName: String) {
    NONE("None"),
    VANILLA("Vanilla"),
    CARAMEL("Caramel"),
    CHOCOLATE("Chocolate"),
    HAZELNUT("Hazelnut"),
    PUMPKIN_SPICE("Pumpkin Spice")
}

enum class SugarType(val displayName: String) {
    NONE("None"),
    WHITE("White Sugar"),
    BROWN("Brown Sugar"),
    CANE("Cane Sugar"),
    STEVIA("Stevia")
}

enum class TeaType(val displayName: String) {
    BLACK("Black Tea"),
    GREEN("Green Tea"),
    EARL_GREY("Earl Grey"),
    CHAMOMILE("Chamomile"),
    PEPPERMINT("Peppermint"),
    MATCHA("Matcha")
}

enum class BaseDrinkType(val displayName: String) {
    COFFEE("Coffee"),
    TEA("Tea"),
    CHOCOLATE("Chocolate")
}

enum class ChocolateType(val displayName: String) {
    MILK("Milk Chocolate"),
    DARK("Dark Chocolate"),
    WHITE("White Chocolate")
}

// Used for the Brew Customization screen, to know which buttons to show on the bottom!
enum class BuilderMode {
    BREW_NOW,       // from home screen, user wants to brew coffee now
    RECIPE_DESIGNER // from favorites, user wants to just save a recipe
}