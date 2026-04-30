package com.newssummary.presentation

import com.newssummary.application.auth.AuthService
import com.newssummary.application.auth.dto.LoginRequest
import com.newssummary.application.auth.dto.LoginResponse
import com.newssummary.application.auth.dto.RegisterRequest
import com.newssummary.application.auth.dto.UserResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/login")
    fun login(@RequestBody @Valid request: LoginRequest): LoginResponse =
        authService.login(request)

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody @Valid request: RegisterRequest): UserResponse =
        authService.register(request)

    @PostMapping("/logout")
    fun logout(@RequestHeader("Authorization") authorization: String) {
        val token = authorization.removePrefix("Bearer ")
        authService.logout(token)
    }
}
