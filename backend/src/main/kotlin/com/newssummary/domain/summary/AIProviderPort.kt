package com.newssummary.domain.summary

import com.newssummary.domain.summaryconfig.SummaryConfig

interface AIProviderPort {
    fun generateSummary(keywords: List<String>, config: SummaryConfig): AIProviderResult
}

data class AIProviderResult(val summaryText: String)
