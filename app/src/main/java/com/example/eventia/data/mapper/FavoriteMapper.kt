package com.example.eventia.data.mapper

import com.example.eventia.data.local.entities.FavoriteEntity
import com.example.eventia.domain.model.Event

/** Evento → fila de favorito (snapshot permanente en Room). */
fun Event.toFavoriteEntity(addedAt: Long = System.currentTimeMillis()): FavoriteEntity =
    FavoriteEntity(
        eventId = id,
        addedAt = addedAt,
        title = title,
        summary = summary,
        description = description,
        date = date,
        categoryId = categoryId,
        categoryName = categoryName,
        venueId = venueId,
        venueName = venueName,
        place = place,
        imageUrl = imageUrl,
        capacity = capacity,
        availableSpots = availableSpots,
        latitude = latitude,
        longitude = longitude
    )

/** Fila de favorito → modelo de dominio. */
fun FavoriteEntity.toDomain(): Event = Event(
    id = eventId,
    title = title,
    summary = summary,
    description = description,
    date = date,
    categoryId = categoryId,
    categoryName = categoryName,
    venueId = venueId,
    venueName = venueName,
    place = place,
    imageUrl = imageUrl,
    capacity = capacity,
    availableSpots = availableSpots,
    latitude = latitude,
    longitude = longitude
)
