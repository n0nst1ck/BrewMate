package com.panko.brewmate.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.panko.brewmate.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Create the DataStore extension
private val Context.dataStore by preferencesDataStore(name = "inventory_prefs")

class InventoryStorage(private val context: Context) {

    // Keys for the inventory items
    private val BEANS_KEY = intPreferencesKey("beans_level")
    private val WATER_KEY = intPreferencesKey("water_level")
    private val GROUNDS_KEY = intPreferencesKey("grounds_level")

    // Getters (Flows)
    val beansLevel: Flow<Int> = context.dataStore.data.map { it[BEANS_KEY] ?: 100 }
    val waterLevel: Flow<Int> = context.dataStore.data.map { it[WATER_KEY] ?: 100 }
    val groundsLevel: Flow<Int> = context.dataStore.data.map { it[GROUNDS_KEY] ?: 0 }

    // Generic getter for maps
    // Basically turns map into flow since DataStore cannot handle maps
    fun getLevel(keyName: String): Flow<Int> {
        val key = intPreferencesKey(keyName)
        return context.dataStore.data.map { it[key] ?: 100 }
    }

    // Save Functions
    suspend fun saveBeans(level: Int) {
        context.dataStore.edit { it[BEANS_KEY] = level }
    }

    suspend fun saveWater(level: Int) {
        context.dataStore.edit { it[WATER_KEY] = level }
    }

    suspend fun saveGrounds(level: Int) {
        context.dataStore.edit { it[GROUNDS_KEY] = level }
    }

    // Generic Saver for dynamic keys (Milk, Syrup, etc.)
    suspend fun saveItemLevel(keyName: String, level: Int) {
        val key = intPreferencesKey(keyName)
        context.dataStore.edit { it[key] = level }
    }

    // Helpers to generate consistent key names
    fun getMilkKey(type: MilkBase) = "milk_${type.name}"
    fun getSyrupKey(type: SyrupType) = "syrup_${type.name}"
    fun getSugarKey(type: SugarType) = "sugar_${type.name}"
    fun getTeaKey(type: TeaType) = "tea_${type.name}"
    fun getChocolateKey(type: ChocolateType) = "choco_${type.name}"
}