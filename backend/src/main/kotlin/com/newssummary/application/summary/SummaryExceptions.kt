package com.newssummary.application.summary

class AIProviderException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class UnsupportedAIProviderException(providerName: String) : RuntimeException("Unsupported AI provider: $providerName")
class NoActiveKeywordsException(userId: Long) : RuntimeException("No active keywords found for user: $userId")
