package com.newssummary.application.auth.dto

import java.time.Instant

data class UserResponse(val id: Long, val email: String, val createdAt: Instant)
