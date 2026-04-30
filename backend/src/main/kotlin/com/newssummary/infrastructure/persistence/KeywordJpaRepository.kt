package com.newssummary.infrastructure.persistence

import com.newssummary.domain.keyword.Keyword
import com.newssummary.domain.keyword.KeywordRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface SpringDataKeywordJpaRepository : JpaRepository<Keyword, Long> {
    fun findAllByUserId(userId: Long): List<Keyword>
    fun existsByUserIdAndWord(userId: Long, word: String): Boolean
}

@Repository
class KeywordJpaRepository(
    private val delegate: SpringDataKeywordJpaRepository
) : KeywordRepository {
    override fun save(keyword: Keyword): Keyword = delegate.save(keyword)
    override fun findById(id: Long): Keyword? = delegate.findById(id).orElse(null)
    override fun findAllByUserId(userId: Long): List<Keyword> = delegate.findAllByUserId(userId)
    override fun existsByUserIdAndWord(userId: Long, word: String): Boolean = delegate.existsByUserIdAndWord(userId, word)
    override fun deleteById(id: Long) = delegate.deleteById(id)
}
