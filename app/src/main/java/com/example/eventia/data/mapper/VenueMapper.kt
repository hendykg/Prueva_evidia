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
