package com.example.eventia.domain.repository

import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import kotlinx.coroutines.flow.Flow

interface EventsRepository {
    fun observeEvents(): Flow<List<Event>>
    fun observeCategories(): Flow<List<Category>>
    suspend fun refreshEvents(query: String? = null, categoryId: String? = null): Result<Unit>
    suspend fun refreshCategories(): Result<Unit>
    suspend fun getEvent(id: String): Result<Event>
    suspend fun getLastSyncMillis(): Long?
}
