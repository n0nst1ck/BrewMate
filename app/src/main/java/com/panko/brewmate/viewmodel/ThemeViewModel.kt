package com.panko.brewmate.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class ThemeViewModel : ViewModel() {
    // True = Dark, False = Light
    // Defaulting to false for now, or you could pass system default in constructor
    var isDarkTheme = mutableStateOf(false)
        private set

    fun toggleTheme() {
        isDarkTheme.value = !isDarkTheme.value
    }

    fun setTheme(isDark: Boolean) {
        isDarkTheme.value = isDark
    }
}