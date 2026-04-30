package com.newssummary.application.auth

import com.newssummary.application.auth.dto.*
import com.newssummary.domain.user.User
import com.newssummary.domain.user.UserRepository
import com.newssummary.infrastructure.redis.TokenRedisRepository
import com.newssummary.infrastructure.security.JwtTokenProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

class UnauthorizedException(message: String) : RuntimeException(message)
class EmailAlreadyExistsException(email: String) : RuntimeException("Email already exists: $email")

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val tokenRedisRepository: TokenRedisRepository
) {

    fun login(request: LoginRequest): LoginResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw UnauthorizedException("Invalid credentials")
        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw UnauthorizedException("Invalid credentials")
        }
        val tokenData = jwtTokenProvider.generateToken(user.id, user.email)
        tokenRedisRepository.saveToken(tokenData.jti, jwtTokenProvider.getRemainingTtl(tokenData.token))
        return LoginResponse(tokenData.token, user.email)
    }

    fun register(request: RegisterRequest): UserResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw EmailAlreadyExistsException(request.email)
        }
        val user = User(
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password)
        )
        val saved = userRepository.save(user)
        return UserResponse(saved.id, saved.email, saved.createdAt)
    }

    fun logout(token: String) {
        val jti = jwtTokenProvider.getJtiFromToken(token)
        val remainingTtl = jwtTokenProvider.getRemainingTtl(token)
        tokenRedisRepository.addToBlacklist(jti, remainingTtl)
    }
}
