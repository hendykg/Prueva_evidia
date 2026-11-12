package com.example.eventia.data.mapper

import com.example.eventia.data.remote.dto.AttendanceDto
import com.example.eventia.domain.model.Attendance

fun AttendanceDto.toDomain(): Attendance = Attendance(
    id = id.orEmpty(),
    eventId = eventId ?: event?.id.orEmpty(),
    event = event?.toDomain(),
    status = status
)
