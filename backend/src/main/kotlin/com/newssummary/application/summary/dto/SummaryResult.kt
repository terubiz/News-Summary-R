package com.newssummary.application.summary.dto

import java.time.Instant

data class SummaryResult(
    val id: Long,
    val content: String,
    val keywords: String,
    val generatedAt: Instant
)
