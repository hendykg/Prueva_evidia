package com.example.eventia.domain.repository

import com.example.eventia.domain.model.User
import kotlinx.coroutines.flow.Flow

/** RF-11: registro, inicio/cierre de sesión y restauración de sesión. */
interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(name: String?, email: String, password: String): Result<User>
    suspend fun logout()
    suspend fun me(): Result<User>
    suspend fun isLoggedIn(): Boolean

    /** Cambia automáticamente cuando el token se guarda o se limpia (401 / logout). */
    fun observeIsLoggedIn(): Flow<Boolean>
}
