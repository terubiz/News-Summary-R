package com.newssummary.infrastructure.persistence

import com.newssummary.domain.user.User
import com.newssummary.domain.user.UserRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface SpringDataUserJpaRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean
}

@Repository
class UserJpaRepository(
    private val delegate: SpringDataUserJpaRepository
) : UserRepository {
    override fun save(user: User): User = delegate.save(user)
    override fun findById(id: Long): User? = delegate.findById(id).orElse(null)
    override fun findByEmail(email: String): User? = delegate.findByEmail(email)
    override fun existsByEmail(email: String): Boolean = delegate.existsByEmail(email)
}
