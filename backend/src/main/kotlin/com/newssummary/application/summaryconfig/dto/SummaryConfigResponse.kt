package com.newssummary.application.summaryconfig.dto

data class SummaryConfigResponse(
    val userId: Long,
    val executionTime: String,
    val lookbackDays: Int,
    val fetchCount: Int,
    val aiProviderName: String
)
