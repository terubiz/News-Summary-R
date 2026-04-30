package com.newssummary.domain.summary

interface SummaryRepository {
    fun save(summary: Summary): Summary
    fun findById(id: Long): Summary?
    fun findAllByUserIdOrderByGeneratedAtDesc(userId: Long): List<Summary>
}
