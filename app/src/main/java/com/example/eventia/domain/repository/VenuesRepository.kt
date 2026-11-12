package com.example.eventia.domain.repository

import com.example.eventia.domain.model.Event
import com.example.eventia.domain.model.Venue
import kotlinx.coroutines.flow.Flow

interface VenuesRepository {
    fun observeVenues(): Flow<List<Venue>>
    suspend fun refreshVenues(): Result<Unit>
    suspend fun getVenue(id: String): Result<Venue>
    suspend fun getVenueEvents(venueId: String): Result<List<Event>>
}
