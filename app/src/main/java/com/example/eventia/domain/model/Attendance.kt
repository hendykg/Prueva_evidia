package com.example.eventia.domain.model

/** Inscripción de usuario a un evento (RF-12): envuelve el evento inscrito. */
data class Attendance(
    val id: String,
    val eventId: String,
    val event: Event?
)
