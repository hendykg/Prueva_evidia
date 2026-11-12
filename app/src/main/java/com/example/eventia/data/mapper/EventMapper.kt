package com.example.eventia.data.mapper

import com.example.eventia.data.local.entities.CategoryEntity
import com.example.eventia.data.local.entities.EventEntity
import com.example.eventia.data.remote.dto.CategoryDto
import com.example.eventia.data.remote.dto.EventDto
import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event

fun EventDto.toDomain(): Event = Event(
    id = id.orEmpty(),
    title = title.orEmpty(),
    summary = summary,
    description = description,
    date = date ?: startDate,
    categoryId = category?.id ?: categoryId,
    categoryName = category?.name,
    venueId = venue?.id ?: venueId,
    venueName = venue?.name,
    place = place ?: venue?.address ?: venue?.name,
    imageUrl = imageUrl,
    capacity = capacity ?: cups,
    availableSpots = availableSpots ?: cups,
    latitude = latitude ?: venue?.latitude,
    longitude = longitude ?: venue?.longitude
)

fun EventDto.toEntity(): EventEntity = toDomain().toEntity()

fun Event.toEntity(): EventEntity = EventEntity(
    id = id,
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

fun EventEntity.toDomain(): Event = Event(
    id = id,
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

fun CategoryDto.toEntity(): CategoryEntity = CategoryEntity(
    id = id.orEmpty(),
    name = name.orEmpty()
)

fun CategoryEntity.toDomain(): Category = Category(id = id, name = name)
