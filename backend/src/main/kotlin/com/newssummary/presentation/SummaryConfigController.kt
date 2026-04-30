package com.newssummary.presentation

import com.newssummary.application.summaryconfig.SummaryConfigService
import com.newssummary.application.summaryconfig.dto.SummaryConfigRequest
import com.newssummary.application.summaryconfig.dto.SummaryConfigResponse
import jakarta.validation.Valid
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/summary-config")
class SummaryConfigController(private val summaryConfigService: SummaryConfigService) {

    @GetMapping
    fun getConfig(auth: Authentication): SummaryConfigResponse =
        summaryConfigService.getOrCreateConfig(auth.principal as Long)

    @PutMapping
    fun updateConfig(
        auth: Authentication,
        @Valid @RequestBody request: SummaryConfigRequest
    ): SummaryConfigResponse =
        summaryConfigService.updateConfig(auth.principal as Long, request)
}
