package com.panko.brewmate.data

import com.google.firebase.auth.FirebaseAuth
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
}