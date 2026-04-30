package com.newssummary.presentation

import com.newssummary.application.summary.GenerateSummaryUseCase
import com.newssummary.application.summary.SummaryService
import com.newssummary.application.summary.dto.SummaryResponse
import com.newssummary.application.summary.dto.SummaryResult
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/summaries")
class SummaryController(
    private val generateSummaryUseCase: GenerateSummaryUseCase,
    private val summaryService: SummaryService
) {

    @GetMapping
    fun getSummaries(auth: Authentication): List<SummaryResponse> =
        summaryService.getSummaries(auth.principal as Long)

    @PostMapping("/generate")
    fun generate(auth: Authentication): SummaryResponse {
        val result: SummaryResult = generateSummaryUseCase.execute(auth.principal as Long)
        return SummaryResponse(result.id, result.content, result.keywords, result.generatedAt)
    }
}
