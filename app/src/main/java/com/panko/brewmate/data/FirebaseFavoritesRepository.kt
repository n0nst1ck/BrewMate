package com.panko.brewmate.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.panko.brewmate.model.FavoriteDrink
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlin.Result // Using standard Kotlin Result

class FirebaseFavoritesRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : FavoritesRepository {

    // Helper to reference the correct Firestore collection
    private fun getFavoritesCollection(userId: String) =
        firestore.collection("users")
            .document(userId)
            .collection("favorites")

    override fun getFavorites(userId: String): Flow<List<FavoriteDrink>> = callbackFlow {
        val subscription = getFavoritesCollection(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val drinks = snapshot.documents.mapNotNull { document ->
                        document.toObject(FavoriteDrink::class.java)
                    }
                    trySend(drinks)
                }
            }

        awaitClose { subscription.remove() }
    }

    override suspend fun saveFavorite(drink: FavoriteDrink): Result<Unit> = runCatching {
        // Use the drink's ID as the Firestore document ID
        getFavoritesCollection(drink.userId).document(drink.id).set(drink).await()
    }

    override suspend fun deleteFavorite(drinkId: String): Result<Unit> = runCatching {
        // Requires the user ID, which we get from Auth
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in.")
        getFavoritesCollection(userId).document(drinkId).delete().await()
    }
}