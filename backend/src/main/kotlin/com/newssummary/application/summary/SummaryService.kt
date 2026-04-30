package com.newssummary.application.summary

import com.newssummary.application.summary.dto.SummaryResponse
import com.newssummary.domain.summary.SummaryRepository
import org.springframework.stereotype.Service

@Service
class SummaryService(private val summaryRepository: SummaryRepository) {

    fun getSummaries(userId: Long): List<SummaryResponse> =
        summaryRepository.findAllByUserIdOrderByGeneratedAtDesc(userId)
            .map { SummaryResponse(it.id, it.content, it.keywords, it.generatedAt) }
}
