package com.newssummary.application.keyword

import com.newssummary.application.keyword.dto.CreateKeywordRequest
import com.newssummary.application.keyword.dto.KeywordResponse
import com.newssummary.application.keyword.dto.UpdateKeywordRequest
import com.newssummary.domain.keyword.Keyword
import com.newssummary.domain.keyword.KeywordRepository
import org.springframework.stereotype.Service

class DuplicateKeywordException(word: String) : RuntimeException("キーワード '$word' は既に登録されています")
class KeywordNotFoundException(id: Long) : RuntimeException("キーワード ID=$id が見つかりません")
class ForbiddenResourceException : RuntimeException("このリソースへのアクセス権がありません")

@Service
class KeywordService(private val keywordRepository: KeywordRepository) {

    fun createKeyword(userId: Long, request: CreateKeywordRequest): KeywordResponse {
        if (keywordRepository.existsByUserIdAndWord(userId, request.word)) {
            throw DuplicateKeywordException(request.word)
        }
        val saved = keywordRepository.save(Keyword(userId = userId, word = request.word))
        return saved.toResponse()
    }

    fun getKeywords(userId: Long): List<KeywordResponse> =
        keywordRepository.findAllByUserId(userId).map { it.toResponse() }

    fun updateKeyword(userId: Long, keywordId: Long, request: UpdateKeywordRequest): KeywordResponse {
        val keyword = keywordRepository.findById(keywordId)
            ?: throw KeywordNotFoundException(keywordId)
        if (keyword.userId != userId) throw ForbiddenResourceException()
        keyword.isActive = request.isActive
        return keywordRepository.save(keyword).toResponse()
    }

    fun deleteKeyword(userId: Long, keywordId: Long) {
        val keyword = keywordRepository.findById(keywordId)
            ?: throw KeywordNotFoundException(keywordId)
        if (keyword.userId != userId) throw ForbiddenResourceException()
        keywordRepository.deleteById(keywordId)
    }

    private fun Keyword.toResponse() = KeywordResponse(id, userId, word, isActive)
}
