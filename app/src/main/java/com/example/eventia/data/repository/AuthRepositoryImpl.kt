package com.example.eventia.data.repository

import com.example.eventia.data.datastore.SessionDataStore
import com.example.eventia.data.mapper.toDomain
import com.example.eventia.data.remote.ApiException
import com.example.eventia.data.remote.EventiaApi
import com.example.eventia.data.remote.serverErrorMessage
import com.example.eventia.data.remote.dto.AuthRequestDto
import com.example.eventia.domain.model.User
import com.example.eventia.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RF-11: guarda el token en DataStore (persistente) y lo limpia ante 401.
 * El token lo agrega automáticamente el AuthInterceptor en cada petición.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: EventiaApi,
    private val sessionDataStore: SessionDataStore
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val response = api.login(AuthRequestDto(email = email, password = password))
            if (response.isSuccessful) {
                val body = response.body()
                val token = body?.token
                if (token.isNullOrBlank()) {
                    return Result.failure(ApiException(response.code(), "Respuesta inválida del servidor"))
                }
                sessionDataStore.saveToken(token)
                Result.success(
                    body.user?.toDomain() ?: User(id = "", name = null, email = email)
                )
            } else {
                Result.failure(
                    ApiException(
                        response.code(),
                        response.serverErrorMessage("No se pudo iniciar sesión")
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(name: String?, email: String, password: String): Result<User> {
        return try {
            val response = api.register(
                AuthRequestDto(email = email, password = password, name = name)
            )
            if (response.isSuccessful) {
                val body = response.body()
                val token = body?.token
                if (token.isNullOrBlank()) {
                    return Result.failure(ApiException(response.code(), "Respuesta inválida del servidor"))
                }
                sessionDataStore.saveToken(token)
                Result.success(
                    body.user?.toDomain() ?: User(id = "", name = name, email = email)
                )
            } else {
                Result.failure(
                    ApiException(
                        response.code(),
                        response.serverErrorMessage("No se pudo registrar la cuenta")
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        sessionDataStore.clearToken()
    }

    override suspend fun me(): Result<User> {
        return try {
            val response = api.me()
            if (response.isSuccessful) {
                val user = response.body()
                if (user != null) Result.success(user.toDomain())
                else Result.failure(ApiException(response.code(), "No se pudo obtener el perfil"))
            } else {
                if (response.code() == 401) {
                    // Sesión inválida o expirada: limpiar token (regla 50)
                    sessionDataStore.clearToken()
                }
                Result.failure(
                    ApiException(response.code(), response.serverErrorMessage("No se pudo obtener el perfil"))
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isLoggedIn(): Boolean =
        !sessionDataStore.getToken().isNullOrBlank()

    override fun observeIsLoggedIn(): Flow<Boolean> =
        sessionDataStore.tokenFlow.map { !it.isNullOrBlank() }
}
