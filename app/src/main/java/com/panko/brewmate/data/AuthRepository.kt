package com.panko.brewmate.data

import kotlin.Result

interface AuthRepository {
    suspend fun createUser(email: String, password: String): kotlin.Result<Unit>
    suspend fun signIn(email: String, password: String): Result<Unit>
    fun getCurrentUserId(): String?
}