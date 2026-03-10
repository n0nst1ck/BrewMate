package com.panko.brewmate

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.panko.brewmate.data.*
import com.panko.brewmate.util.Scheduler
import com.panko.brewmate.util.SystemScheduler

class BrewMateApplication : Application() {

    lateinit var authRepository: AuthRepository
    lateinit var coffeeMakerRepository: CoffeeMakerRepository
    lateinit var schedulingRepository: SchedulingRepository
    lateinit var favoritesRepository: FavoritesRepository
    lateinit var historyRepository: HistoryRepository
    lateinit var inventoryStorage: InventoryStorage

    override fun onCreate() {
        super.onCreate()

        // 1. Core utilities
        val scheduler: Scheduler = SystemScheduler()
        val androidAlarmScheduler = AndroidAlarmScheduler(this)

        inventoryStorage = InventoryStorage(this)
        // 2. Firebase Instances
        val authInstance = FirebaseAuth.getInstance()
        val firestoreInstance = FirebaseFirestore.getInstance()

        // 3. Instantiate Repositories (Exactly as you had them!)
        coffeeMakerRepository = SimulatedCoffeeMaker(scheduler, inventoryStorage)
        authRepository = FirebaseAuthRepository()

        schedulingRepository = FirebaseSchedulingRepository(
            firestore = firestoreInstance,
            auth = authInstance,
            systemScheduler = androidAlarmScheduler
        )

        favoritesRepository = FirebaseFavoritesRepository(
            firestore = firestoreInstance,
            auth = authInstance
        )

        historyRepository = FirebaseHistoryRepository(
            firestore = firestoreInstance,
            auth = authInstance
        )
    }
}