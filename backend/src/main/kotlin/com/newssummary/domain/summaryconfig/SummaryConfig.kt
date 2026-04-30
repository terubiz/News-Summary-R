package com.newssummary.domain.summaryconfig

import jakarta.persistence.*

@Entity
@Table(name = "summary_configs")
class SummaryConfig(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "user_id", nullable = false, unique = true)
    val userId: Long,
    @Column(name = "execution_time", nullable = false)
    var executionTime: String = "07:00",
    @Column(name = "lookback_days", nullable = false)
    var lookbackDays: Int = 1,
    @Column(name = "fetch_count", nullable = false)
    var fetchCount: Int = 10,
    @Column(name = "ai_provider_name", nullable = false)
    var aiProviderName: String = "gemini"
)
