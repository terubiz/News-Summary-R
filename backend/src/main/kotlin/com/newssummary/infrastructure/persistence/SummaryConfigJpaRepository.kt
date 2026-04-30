package com.newssummary.infrastructure.persistence

import com.newssummary.domain.summaryconfig.SummaryConfig
import com.newssummary.domain.summaryconfig.SummaryConfigRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface SpringDataSummaryConfigJpaRepository : JpaRepository<SummaryConfig, Long> {
    fun findByUserId(userId: Long): SummaryConfig?
}

@Repository
class SummaryConfigJpaRepository(
    private val delegate: SpringDataSummaryConfigJpaRepository
) : SummaryConfigRepository {
    override fun save(config: SummaryConfig): SummaryConfig = delegate.save(config)
    override fun findByUserId(userId: Long): SummaryConfig? = delegate.findByUserId(userId)
    override fun findAll(): List<SummaryConfig> = delegate.findAll()
}
