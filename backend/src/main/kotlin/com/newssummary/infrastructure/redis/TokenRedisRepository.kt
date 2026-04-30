package com.newssummary.infrastructure.redis

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.util.concurrent.TimeUnit

@Repository
class TokenRedisRepository(private val redisTemplate: StringRedisTemplate) {

    fun saveToken(jti: String, ttlMs: Long) {
        redisTemplate.opsForValue().set("token:$jti", "valid", ttlMs, TimeUnit.MILLISECONDS)
    }

    fun isBlacklisted(jti: String): Boolean =
        redisTemplate.hasKey("blacklist:$jti") == true

    fun addToBlacklist(jti: String, remainingTtlMs: Long) {
        redisTemplate.opsForValue().set("blacklist:$jti", "revoked", remainingTtlMs, TimeUnit.MILLISECONDS)
    }
}
