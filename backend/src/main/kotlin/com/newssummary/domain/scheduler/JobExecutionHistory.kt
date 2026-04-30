package com.newssummary.domain.scheduler

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "job_execution_histories")
class JobExecutionHistory(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(nullable = false)
    val status: String,
    @Column(name = "executed_at", nullable = false)
    val executedAt: Instant = Instant.now(),
    @Column(name = "error_message", columnDefinition = "TEXT")
    val errorMessage: String? = null
)
