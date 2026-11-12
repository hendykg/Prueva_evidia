package com.example.eventia.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore by preferencesDataStore(name = "session")

@Singleton
class SessionDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tokenKey = stringPreferencesKey("auth_token")
    private val lastSyncKey = longPreferencesKey("last_catalog_sync")

    /** Flujo del token para que la UI reaccione a login/logout/401. */
    val tokenFlow: Flow<String?> = context.sessionDataStore.data.map { it[tokenKey] }

    suspend fun saveToken(token: String) {
        context.sessionDataStore.edit { it[tokenKey] = token }
    }

    suspend fun getToken(): String? {
        return context.sessionDataStore.data.map { it[tokenKey] }.first()
    }

    suspend fun clearToken() {
        context.sessionDataStore.edit { it.remove(tokenKey) }
    }

    suspend fun saveLastSync(millis: Long) {
        context.sessionDataStore.edit { it[lastSyncKey] = millis }
    }

    suspend fun getLastSync(): Long? {
        return context.sessionDataStore.data.map { it[lastSyncKey] }.first()
    }
}
