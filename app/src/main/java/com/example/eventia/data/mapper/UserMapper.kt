package com.example.eventia.data.mapper

import com.example.eventia.data.remote.dto.UserDto
import com.example.eventia.domain.model.User

fun UserDto.toDomain(): User = User(
    id = id.orEmpty(),
    name = name,
    email = email
)
