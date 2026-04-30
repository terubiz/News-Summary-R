package com.newssummary.application.summaryconfig

import com.newssummary.application.summaryconfig.dto.SummaryConfigRequest
import com.newssummary.application.summaryconfig.dto.SummaryConfigResponse
import com.newssummary.domain.summaryconfig.SummaryConfig
import com.newssummary.domain.summaryconfig.SummaryConfigRepository
import org.springframework.stereotype.Service

@Service
class SummaryConfigService(private val repository: SummaryConfigRepository) {

    fun getOrCreateConfig(userId: Long): SummaryConfigResponse {
        val config = repository.findByUserId(userId)
            ?: repository.save(SummaryConfig(userId = userId))
        return config.toResponse()
    }

    fun updateConfig(userId: Long, request: SummaryConfigRequest): SummaryConfigResponse {
        val config = repository.findByUserId(userId)
            ?: repository.save(SummaryConfig(userId = userId))
        config.executionTime = request.executionTime
        config.lookbackDays = request.lookbackDays
        config.fetchCount = request.fetchCount
        config.aiProviderName = request.aiProviderName
        return repository.save(config).toResponse()
    }

    private fun SummaryConfig.toResponse() = SummaryConfigResponse(userId, executionTime, lookbackDays, fetchCount, aiProviderName)
}
