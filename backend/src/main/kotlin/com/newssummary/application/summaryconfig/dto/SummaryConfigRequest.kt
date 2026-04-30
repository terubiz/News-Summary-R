package com.newssummary.application.summaryconfig.dto

import jakarta.validation.constraints.*

data class SummaryConfigRequest(
    @field:NotBlank
    @field:Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$")
    val executionTime: String,
    @field:Min(1) @field:Max(365)
    val lookbackDays: Int,
    @field:Min(1) @field:Max(100)
    val fetchCount: Int,
    @field:NotBlank
    val aiProviderName: String
)
