package com.newssummary.domain.keyword

interface KeywordRepository {
    fun save(keyword: Keyword): Keyword
    fun findById(id: Long): Keyword?
    fun findAllByUserId(userId: Long): List<Keyword>
    fun existsByUserIdAndWord(userId: Long, word: String): Boolean
    fun deleteById(id: Long)
}
