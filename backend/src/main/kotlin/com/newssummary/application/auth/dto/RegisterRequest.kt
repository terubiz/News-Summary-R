package com.newssummary.application.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:Email @field:Size(max = 255) val email: String,
    @field:Size(min = 8) val password: String
)
