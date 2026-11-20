package com.example.eventia.data.mapper

import com.example.eventia.data.local.entities.VenueEntity
import com.example.eventia.data.remote.dto.VenueDto
import com.example.eventia.domain.model.Venue

fun VenueDto.toDomain(): Venue = Venue(
    id = id.orEmpty(),
    name = name.orEmpty(),
    address = address,
    description = description,
    schedule = openingHours ?: schedule,
    latitude = lat,
    longitude = lng,
    imageUrl = imageUrl,
    upcomingEventsCount = upcomingEventsCount
)

fun VenueDto.toEntity(): VenueEntity = toDomain().toEntity()

/** Lista de DTO → entidades descartando los lugares sin id. */
fun List<VenueDto>.toVenueEntities(): List<VenueEntity> =
    mapNotNull { dto -> dto.id?.let { id -> dto.copy(id = id).toEntity() } }

fun Venue.toEntity(): VenueEntity = VenueEntity(
    id = id,
    name = name,
    address = address,
    description = description,
    schedule = schedule,
    latitude = latitude,
    longitude = longitude,
    imageUrl = imageUrl,
    upcomingEventsCount = upcomingEventsCount
)

fun VenueEntity.toDomain(): Venue = Venue(
    id = id,
    name = name,
    address = address,
    description = description,
    schedule = schedule,
    latitude = latitude,
    longitude = longitude,
    imageUrl = imageUrl,
    upcomingEventsCount = upcomingEventsCount
)
