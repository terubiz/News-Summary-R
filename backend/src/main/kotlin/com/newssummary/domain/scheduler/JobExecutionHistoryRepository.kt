package com.newssummary.domain.scheduler

interface JobExecutionHistoryRepository {
    fun save(history: JobExecutionHistory): JobExecutionHistory
}
