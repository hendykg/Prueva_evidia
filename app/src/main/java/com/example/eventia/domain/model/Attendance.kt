package com.example.eventia.domain.model

/** Inscripción de usuario a un evento (RF-12). */
data class Attendance(
    val id: String,
    val eventId: String,
    val event: Event?,
    val status: String?
)
