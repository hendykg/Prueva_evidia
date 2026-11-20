package com.example.eventia.data.repository

import com.example.eventia.data.datastore.SessionDataStore
import com.example.eventia.data.mapper.toDomain
import com.example.eventia.data.remote.ApiException
import com.example.eventia.data.remote.EventiaApi
import com.example.eventia.data.remote.apiCall
import com.example.eventia.data.remote.dto.AuthRequestDto
import com.example.eventia.data.remote.dto.AuthResponseDto
import com.example.eventia.data.remote.toApiException
import com.example.eventia.domain.model.User
import com.example.eventia.domain.repository.AuthRepository
import retrofit2.Response
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

    override suspend fun login(email: String, password: String): Result<User> = apiCall {
        val response = api.login(AuthRequestDto(email = email, password = password))
        if (response.isSuccessful) {
            saveSession(response, fallbackUser = User(id = "", name = null, email = email))
        } else {
            Result.failure(response.toApiException("No se pudo iniciar sesión"))
        }
    }

    override suspend fun register(name: String?, email: String, password: String): Result<User> = apiCall {
        val response = api.register(AuthRequestDto(email = email, password = password, name = name))
        if (response.isSuccessful) {
            saveSession(response, fallbackUser = User(id = "", name = name, email = email))
        } else {
            Result.failure(response.toApiException("No se pudo registrar la cuenta"))
        }
    }

    /** Persiste el token de la respuesta y devuelve el usuario (o uno vacío). */
    private suspend fun saveSession(
        response: Response<AuthResponseDto>,
        fallbackUser: User
    ): Result<User> {
        val body = response.body()
        val token = body?.token
        if (token.isNullOrBlank()) {
            return Result.failure(ApiException(response.code(), "Respuesta inválida del servidor"))
        }
        sessionDataStore.saveToken(token)
        return Result.success(body.user?.toDomain() ?: fallbackUser)
    }

    override suspend fun me(): Result<User> = apiCall {
        val response = api.me()
        // Sesión inválida o expirada: limpiar token para que la UI redirija (regla 50)
        if (response.code() == 401) {
            sessionDataStore.clearToken()
        }
        response.body()?.toDomain()?.let { Result.success(it) }
            ?: Result.failure(response.toApiException("No se pudo obtener el perfil"))
    }

    override suspend fun logout() {
        sessionDataStore.clearToken()
    }

    override suspend fun isLoggedIn(): Boolean =
        !sessionDataStore.getToken().isNullOrBlank()

    override fun observeIsLoggedIn(): Flow<Boolean> =
        sessionDataStore.tokenFlow.map { !it.isNullOrBlank() }
}
