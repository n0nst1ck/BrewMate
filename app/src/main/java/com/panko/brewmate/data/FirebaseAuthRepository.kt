package com.panko.brewmate.data

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    override suspend fun createUser(email: String, password: String): kotlin.Result<Unit> {
        return runCatching {
            auth.createUserWithEmailAndPassword(email, password).await()
        }
    }

    override suspend fun signIn(email: String, password: String): kotlin.Result<Unit> {
        return runCatching {
            auth.signInWithEmailAndPassword(email, password).await()
        }
    }

    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    override val currentUserIdFlow: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid)
        }

        auth.addAuthStateListener(listener)

        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override fun signOut() {
        auth.signOut()
    }
}