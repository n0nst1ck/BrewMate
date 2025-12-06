package com.panko.brewmate.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.panko.brewmate.model.BrewHistoryItem
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseHistoryRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : HistoryRepository {

    private fun getHistoryCollection(userId: String) =
        firestore.collection("users")
            .document(userId)
            .collection("history")

    override fun getHistory(userId: String): Flow<List<BrewHistoryItem>> = callbackFlow {
        if (userId.isBlank()) {
            close() // Safety check
            return@callbackFlow
        }

        // Order by timestamp descending (newest on top)
        val subscription = getHistoryCollection(userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { it.toObject(BrewHistoryItem::class.java) }
                    trySend(items)
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun addHistoryItem(item: BrewHistoryItem): Result<Unit> = runCatching {
        getHistoryCollection(item.userId).document(item.id).set(item).await()
    }

    override suspend fun clearHistory(userId: String): Result<Unit> = runCatching {
        val collection = getHistoryCollection(userId).get().await()
        for (document in collection) {
            document.reference.delete()
        }
    }
}