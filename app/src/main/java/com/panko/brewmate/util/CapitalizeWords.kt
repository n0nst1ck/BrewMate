package com.panko.brewmate.util

fun String.capitalizeWords(): String =
    lowercase().split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }
