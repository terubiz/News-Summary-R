package com.newssummary.domain.keyword

import jakarta.persistence.*

@Entity
@Table(
    name = "keywords",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "word"])]
)
class Keyword(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(nullable = false)
    val word: String,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
)
