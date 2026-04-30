package com.newssummary.infrastructure.ai

import com.newssummary.application.summary.UnsupportedAIProviderException
import com.newssummary.domain.summary.AIProviderPort
import com.newssummary.domain.summary.AIProviderResult
import com.newssummary.domain.summaryconfig.SummaryConfig
import org.springframework.stereotype.Component

@Component
class AIProviderRouter(
    private val geminiAdapter: GeminiApiAdapter
) : AIProviderPort {

    override fun generateSummary(keywords: List<String>, config: SummaryConfig): AIProviderResult =
        when (config.aiProviderName) {
            "gemini" -> geminiAdapter.generateSummary(keywords, config)
            else -> throw UnsupportedAIProviderException(config.aiProviderName)
        }
}
