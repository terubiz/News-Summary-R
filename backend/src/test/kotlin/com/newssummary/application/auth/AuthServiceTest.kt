package com.newssummary.application.auth

import com.newssummary.application.auth.dto.LoginRequest
import com.newssummary.application.auth.dto.RegisterRequest
import com.newssummary.domain.user.User
import com.newssummary.domain.user.UserRepository
import com.newssummary.infrastructure.redis.TokenRedisRepository
import com.newssummary.infrastructure.security.JwtTokenProvider
import com.newssummary.infrastructure.security.TokenData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.Instant

@DisplayName("AuthService")
class AuthServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var tokenRedisRepository: TokenRedisRepository
    private lateinit var authService: AuthService
    private val passwordEncoder = BCryptPasswordEncoder()

    @BeforeEach
    fun setUp() {
        userRepository = mock()
        jwtTokenProvider = mock()
        tokenRedisRepository = mock()
        authService = AuthService(userRepository, passwordEncoder, jwtTokenProvider, tokenRedisRepository)
    }

    @Test
    @DisplayName("login: 正しい資格情報で LoginResponse を返す")
    fun loginReturnsLoginResponseWithValidCredentials() {
        val hash = passwordEncoder.encode("password123")
        val user = User(id = 1L, email = "test@example.com", passwordHash = hash)
        val tokenData = TokenData("jwt-token", "jti-123", Instant.now().plusSeconds(3600))
        whenever(userRepository.findByEmail("test@example.com")).thenReturn(user)
        whenever(jwtTokenProvider.generateToken(1L, "test@example.com")).thenReturn(tokenData)
        whenever(jwtTokenProvider.getRemainingTtl("jwt-token")).thenReturn(3600000L)

        val result = authService.login(LoginRequest("test@example.com", "password123"))

        assertThat(result.token).isEqualTo("jwt-token")
        assertThat(result.email).isEqualTo("test@example.com")
        verify(tokenRedisRepository).saveToken("jti-123", 3600000L)
    }

    @Test
    @DisplayName("login: 存在しないメールで UnauthorizedException をスローする")
    fun loginThrowsUnauthorizedExceptionForUnknownEmail() {
        whenever(userRepository.findByEmail(any())).thenReturn(null)
        assertThrows<UnauthorizedException> {
            authService.login(LoginRequest("unknown@example.com", "password"))
        }
    }

    @Test
    @DisplayName("login: 誤パスワードで UnauthorizedException をスローする")
    fun loginThrowsUnauthorizedExceptionForWrongPassword() {
        val user = User(id = 1L, email = "test@example.com", passwordHash = passwordEncoder.encode("correct"))
        whenever(userRepository.findByEmail("test@example.com")).thenReturn(user)
        assertThrows<UnauthorizedException> {
            authService.login(LoginRequest("test@example.com", "wrong"))
        }
    }

    @Test
    @DisplayName("register: 新規メールで UserResponse を返す")
    fun registerReturnsUserResponseForNewEmail() {
        whenever(userRepository.existsByEmail("new@example.com")).thenReturn(false)
        whenever(userRepository.save(any())).thenAnswer { it.arguments[0] as User }

        val result = authService.register(RegisterRequest("new@example.com", "password123"))

        assertThat(result.email).isEqualTo("new@example.com")
        verify(userRepository).save(argThat { email == "new@example.com" })
    }

    @Test
    @DisplayName("register: 既存メールで EmailAlreadyExistsException をスローする")
    fun registerThrowsEmailAlreadyExistsExceptionForDuplicateEmail() {
        whenever(userRepository.existsByEmail("exists@example.com")).thenReturn(true)
        assertThrows<EmailAlreadyExistsException> {
            authService.register(RegisterRequest("exists@example.com", "password123"))
        }
    }

    @Test
    @DisplayName("logout: トークンをブラックリストに登録する")
    fun logoutAddsTokenToBlacklist() {
        whenever(jwtTokenProvider.getJtiFromToken("token")).thenReturn("jti-abc")
        whenever(jwtTokenProvider.getRemainingTtl("token")).thenReturn(1800000L)

        authService.logout("token")

        verify(tokenRedisRepository).addToBlacklist("jti-abc", 1800000L)
    }
}
