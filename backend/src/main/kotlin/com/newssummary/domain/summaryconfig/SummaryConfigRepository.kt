package com.newssummary.domain.summaryconfig

interface SummaryConfigRepository {
    fun save(config: SummaryConfig): SummaryConfig
    fun findByUserId(userId: Long): SummaryConfig?
    fun findAll(): List<SummaryConfig>   // scheduler スペックで使用
}
