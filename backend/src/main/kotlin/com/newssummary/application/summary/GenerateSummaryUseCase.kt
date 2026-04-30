package com.newssummary.application.summary

import com.newssummary.application.summary.dto.SummaryResult
import com.newssummary.domain.keyword.KeywordRepository
import com.newssummary.domain.summary.AIProviderPort
import com.newssummary.domain.summary.Summary
import com.newssummary.domain.summary.SummaryRepository
import com.newssummary.domain.summaryconfig.SummaryConfig
import com.newssummary.domain.summaryconfig.SummaryConfigRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class GenerateSummaryUseCase(
    private val keywordRepository: KeywordRepository,
    private val summaryConfigRepository: SummaryConfigRepository,
    private val aiProviderPort: AIProviderPort,
    private val summaryRepository: SummaryRepository
) {
    private val log = LoggerFactory.getLogger(GenerateSummaryUseCase::class.java)

    fun execute(userId: Long): SummaryResult {
        val activeKeywords = keywordRepository.findAllByUserId(userId)
            .filter { it.isActive }
        if (activeKeywords.isEmpty()) throw NoActiveKeywordsException(userId)

        val config = summaryConfigRepository.findByUserId(userId)
            ?: SummaryConfig(userId = userId)

        val keywordWords = activeKeywords.map { it.word }
        val result = aiProviderPort.generateSummary(keywordWords, config)

        val summary = summaryRepository.save(
            Summary(
                userId = userId,
                content = result.summaryText,
                keywords = keywordWords.joinToString(","),
                generatedAt = Instant.now()
            )
        )
        log.info("要約生成完了: userId={}, キーワード数={}, generatedAt={}", userId, keywordWords.size, summary.generatedAt)

        return SummaryResult(summary.id, summary.content, summary.keywords, summary.generatedAt)
    }
}
