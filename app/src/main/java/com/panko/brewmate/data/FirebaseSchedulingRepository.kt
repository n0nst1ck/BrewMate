// File: data/FirebaseSchedulingRepository.kt

package com.panko.brewmate.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await // Don't forget this!

// Import your custom models
import com.panko.brewmate.model.ScheduledBrew
import com.panko.brewmate.data.SchedulingRepository
import com.panko.brewmate.data.AndroidAlarmScheduler
import com.panko.brewmate.data.SystemSchedulerInterface
import java.time.DayOfWeek
import java.util.UUID

// Assuming SystemSchedulerInterface is defined elsewhere
// Assuming SchedulingRepository is defined as the interface this class implements

class FirebaseSchedulingRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val systemScheduler: SystemSchedulerInterface // The AndroidAlarmScheduler implementation
) : SchedulingRepository {

    // Helper to get the current user's document path
    private fun getScheduleCollection() =
        firestore.collection("users")
            .document(auth.currentUser?.uid ?: "guest_user_if_allowed")
            .collection("schedules")

    // --- Implementation of SchedulingRepository methods ---

    override suspend fun scheduleBrew(brew: ScheduledBrew): Result<Unit> {
        return runCatching {
            // Saves the schedule object to Firestore
            getScheduleCollection().document(brew.id).set(brew).await()
            // Sets the actual system alarm
            systemScheduler.schedule(brew)
            Unit // Return success
        }
    }

    private fun getScheduleCollection(userId: String) =
        firestore.collection("users")
            .document(userId) // The collection path is specific to the user
            .collection("schedules")

    override fun getScheduledBrews(userId: String): Flow<List<ScheduledBrew>> = callbackFlow {
        // 1. Create a listener for Firestore data changes
        val subscription = getScheduleCollection(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // 2. If an error occurs, send it down the Flow and close the channel.
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // 3. Convert the documents to a List of ScheduledBrew objects
                    val brews = snapshot.documents.mapNotNull { document ->
                        // Firestore's toObject function handles data class mapping
                        document.toObject(ScheduledBrew::class.java)
                    }

                    // 4. Send the new list of brews to the Flow consumer (your ViewModel/UI)
                    trySend(brews)
                }
            }

        // 5. This block is executed when the Flow consumer cancels (e.g., when the ViewModel is cleared).
        // It is crucial for preventing memory leaks by removing the Firestore listener.
        awaitClose {
            subscription.remove()
        }
    }

    override suspend fun cancelBrew(brewId: String): Result<Unit> {
        return runCatching {
            // Removes the schedule from Firestore
            getScheduleCollection().document(brewId).delete().await()
            // Cancels the system alarm
            systemScheduler.cancel(brewId)
            Unit
        }
    }
}