package com.newssummary.infrastructure.persistence

import com.newssummary.domain.summary.Summary
import com.newssummary.domain.summary.SummaryRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface SpringDataSummaryJpaRepository : JpaRepository<Summary, Long> {
    fun findAllByUserIdOrderByGeneratedAtDesc(userId: Long): List<Summary>
}

@Repository
class SummaryJpaRepository(
    private val delegate: SpringDataSummaryJpaRepository
) : SummaryRepository {
    override fun save(summary: Summary): Summary = delegate.save(summary)
    override fun findById(id: Long): Summary? = delegate.findById(id).orElse(null)
    override fun findAllByUserIdOrderByGeneratedAtDesc(userId: Long): List<Summary> =
        delegate.findAllByUserIdOrderByGeneratedAtDesc(userId)
}
