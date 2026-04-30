package com.newssummary.application.summary.dto

import java.time.Instant

data class SummaryResponse(
    val id: Long,
    val content: String,
    val keywords: String,
    val generatedAt: Instant
)
