package com.panko.brewmate

import com.panko.brewmate.data.SystemSchedulerInterface
import com.panko.brewmate.data.FirebaseSchedulingRepository
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

import com.panko.brewmate.data.AndroidAlarmScheduler
import com.panko.brewmate.data.CoffeeMakerRepository
import com.panko.brewmate.data.SimulatedCoffeeMaker
import com.panko.brewmate.ui.theme.BrewMateTheme
import com.panko.brewmate.util.Scheduler
import com.panko.brewmate.util.SystemScheduler
import com.panko.brewmate.data.AuthRepository
import com.panko.brewmate.data.FavoritesRepository
import com.panko.brewmate.data.FirebaseAuthRepository
import com.panko.brewmate.data.FirebaseFavoritesRepository
import com.panko.brewmate.data.FirebaseHistoryRepository
import com.panko.brewmate.data.HistoryRepository
import com.panko.brewmate.data.SchedulingRepository
import com.panko.brewmate.viewmodel.AuthViewModel
import com.panko.brewmate.ui.BrewMateApp
import com.panko.brewmate.viewmodel.CoffeeMakerViewModel
import com.panko.brewmate.viewmodel.FavoritesViewModel
import com.panko.brewmate.viewmodel.HistoryViewModel
import com.panko.brewmate.viewmodel.SchedulingViewModel
import com.panko.brewmate.viewmodel.ThemeViewModel


class MainActivity : ComponentActivity() {

    // You can define dependencies here to make them accessible across the activity
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModelFactory: ViewModelProvider.Factory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Dependency Instantiation
        val scheduler: Scheduler = SystemScheduler()
        val coffeeMakerRepository: CoffeeMakerRepository = SimulatedCoffeeMaker(scheduler)
        authRepository = FirebaseAuthRepository() // Concrete Firebase instance
        val authInstance = FirebaseAuth.getInstance()
        val firestoreInstance: FirebaseFirestore = FirebaseFirestore.getInstance()
        val systemScheduler: SystemSchedulerInterface = AndroidAlarmScheduler(applicationContext)
        val schedulingRepository: SchedulingRepository = FirebaseSchedulingRepository(
            firestore = firestoreInstance,
            auth = authInstance,
            systemScheduler = systemScheduler
        )
        val favoritesRepository: FavoritesRepository = FirebaseFavoritesRepository(
            firestore = firestoreInstance,
            auth = authInstance
        )
        val historyRepository: HistoryRepository = FirebaseHistoryRepository(
            firestore = firestoreInstance,
            auth = authInstance
        )
        viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return when {
                    modelClass.isAssignableFrom(CoffeeMakerViewModel::class.java) ->
                        CoffeeMakerViewModel(
                            coffeeMakerRepository = coffeeMakerRepository,
                            historyRepository = historyRepository,
                            authRepository = authRepository
                        ) as T

                    modelClass.isAssignableFrom(AuthViewModel::class.java) ->
                        AuthViewModel(authRepository) as T

                    modelClass.isAssignableFrom(SchedulingViewModel::class.java) ->
                        SchedulingViewModel(schedulingRepository, authRepository) as T

                    modelClass.isAssignableFrom(FavoritesViewModel::class.java) ->
                        FavoritesViewModel(favoritesRepository, authRepository) as T

                    modelClass.isAssignableFrom(HistoryViewModel::class.java) ->
                        HistoryViewModel(historyRepository, authRepository) as T

                    else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }

        val themeViewModel: ThemeViewModel by viewModels()

        setContent {
            val systemInDark = isSystemInDarkTheme()
            LaunchedEffect(Unit) {
                themeViewModel.setTheme(systemInDark)
            }
            val isDark by remember { themeViewModel.isDarkTheme }

            BrewMateTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Obtain ViewModels using the factory
                    val coffeeMakerViewModel: CoffeeMakerViewModel = viewModel(factory = viewModelFactory)
                    val authViewModel: AuthViewModel = viewModel(factory = viewModelFactory)
                    val schedulingViewModel: SchedulingViewModel = viewModel(factory = viewModelFactory)
                    val favoritesViewModel: FavoritesViewModel = viewModel(factory = viewModelFactory)
                    val historyViewModel: HistoryViewModel = viewModel(factory = viewModelFactory)

                    // 3. Call the Root Navigator
                    BrewMateApp(
                        coffeeMakerViewModel = coffeeMakerViewModel,
                        authViewModel = authViewModel,
                        schedulingViewModel = schedulingViewModel,
                        favoritesViewModel = favoritesViewModel,
                        historyViewModel = historyViewModel,
                        themeViewModel = themeViewModel,
                        authRepository = authRepository, // Passed for the initial login check
                        viewModelFactory = viewModelFactory // Passed to allow screens to get their ViewModels
                    )
                }
            }
        }
    }
}