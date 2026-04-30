package com.newssummary.domain.user

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "users")
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false, unique = true)
    val email: String,
    @Column(nullable = false)
    var passwordHash: String,
    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
