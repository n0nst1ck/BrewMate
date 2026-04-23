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

        // Core utilities
        val scheduler: Scheduler = SystemScheduler()
        val androidAlarmScheduler = AndroidAlarmScheduler(this)

        inventoryStorage = InventoryStorage(this)
        // Firebase Instances
        val authInstance = FirebaseAuth.getInstance()
        val firestoreInstance = FirebaseFirestore.getInstance()

        // Instantiate Repositories
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