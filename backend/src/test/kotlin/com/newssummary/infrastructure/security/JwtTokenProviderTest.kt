package com.newssummary.infrastructure.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("JwtTokenProvider")
class JwtTokenProviderTest {

    private lateinit var provider: JwtTokenProvider

    @BeforeEach
    fun setUp() {
        provider = JwtTokenProvider(
            secret = "test-secret-key-for-testing-only-32chars",
            expirationMs = 3600000
        )
    }

    @Test
    @DisplayName("generateToken: 有効な JWT を返す")
    fun generateTokenReturnsValidJwt() {
        val tokenData = provider.generateToken(1L, "test@example.com")
        assertThat(tokenData.token).isNotBlank()
        assertThat(tokenData.jti).isNotBlank()
        assertThat(tokenData.expiresAt).isNotNull()
    }

    @Test
    @DisplayName("validateToken: 有効なトークンで true を返す")
    fun validateTokenReturnsTrueForValidToken() {
        val tokenData = provider.generateToken(1L, "test@example.com")
        assertThat(provider.validateToken(tokenData.token)).isTrue()
    }

    @Test
    @DisplayName("validateToken: 不正なトークンで false を返す")
    fun validateTokenReturnsFalseForInvalidToken() {
        assertThat(provider.validateToken("invalid.token.here")).isFalse()
    }

    @Test
    @DisplayName("getUserIdFromToken: 正しい userId を返す")
    fun getUserIdFromTokenReturnsCorrectUserId() {
        val tokenData = provider.generateToken(42L, "test@example.com")
        assertThat(provider.getUserIdFromToken(tokenData.token)).isEqualTo(42L)
    }

    @Test
    @DisplayName("getJtiFromToken: 正しい jti を返す")
    fun getJtiFromTokenReturnsCorrectJti() {
        val tokenData = provider.generateToken(1L, "test@example.com")
        assertThat(provider.getJtiFromToken(tokenData.token)).isEqualTo(tokenData.jti)
    }

    @Test
    @DisplayName("getRemainingTtl: 正の値を返す")
    fun getRemainingTtlReturnsPositiveValue() {
        val tokenData = provider.generateToken(1L, "test@example.com")
        assertThat(provider.getRemainingTtl(tokenData.token)).isGreaterThan(0L)
    }
}
