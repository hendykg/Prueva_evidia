package com.example.eventia.data.mapper

import com.example.eventia.data.remote.dto.EventDto
import com.example.eventia.domain.model.Attendance

/**
 * `GET /me/attendances` devuelve una lista de eventos (no objetos de inscripción),
 * así que cada evento se envuelve en una [Attendance] cuyo `eventId` es el id del evento.
 */
fun EventDto.toAttendance(): Attendance = Attendance(
    id = id.orEmpty(),
    eventId = id.orEmpty(),
    event = toDomain()
)
