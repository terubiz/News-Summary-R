package com.newssummary.application.keyword.dto

data class KeywordResponse(
    val id: Long,
    val userId: Long,
    val word: String,
    val isActive: Boolean
)
