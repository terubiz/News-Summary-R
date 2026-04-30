package com.newssummary.infrastructure.security

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*
import javax.crypto.SecretKey

data class TokenData(val token: String, val jti: String, val expiresAt: Instant)

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.expiration-ms:86400000}") private val expirationMs: Long
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    fun generateToken(userId: Long, email: String): TokenData {
        val jti = UUID.randomUUID().toString()
        val now = Instant.now()
        val expiresAt = now.plusMillis(expirationMs)
        val token = Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .id(jti)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(key)
            .compact()
        return TokenData(token, jti, expiresAt)
    }

    fun validateToken(token: String): Boolean = runCatching {
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
        true
    }.getOrElse { false }

    fun getJtiFromToken(token: String): String =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload.id

    fun getUserIdFromToken(token: String): Long =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload.subject.toLong()

    fun getRemainingTtl(token: String): Long {
        val expiration = Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).payload.expiration
        return maxOf(0L, expiration.time - System.currentTimeMillis())
    }
}
