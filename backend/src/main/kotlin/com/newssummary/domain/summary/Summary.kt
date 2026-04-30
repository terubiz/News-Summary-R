package com.newssummary.domain.summary

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "summaries")
class Summary(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,
    @Column(nullable = false)
    val keywords: String,
    @Column(name = "generated_at", nullable = false)
    val generatedAt: Instant = Instant.now()
)
