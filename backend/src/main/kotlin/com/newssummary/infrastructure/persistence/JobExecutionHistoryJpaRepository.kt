package com.newssummary.infrastructure.persistence

import com.newssummary.domain.scheduler.JobExecutionHistory
import com.newssummary.domain.scheduler.JobExecutionHistoryRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface SpringDataJobExecutionHistoryJpaRepository : JpaRepository<JobExecutionHistory, Long>

@Repository
class JobExecutionHistoryJpaRepository(
    private val delegate: SpringDataJobExecutionHistoryJpaRepository
) : JobExecutionHistoryRepository {
    override fun save(history: JobExecutionHistory): JobExecutionHistory = delegate.save(history)
}
