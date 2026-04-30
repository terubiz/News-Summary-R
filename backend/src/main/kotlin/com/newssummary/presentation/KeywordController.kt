package com.newssummary.presentation

import com.newssummary.application.keyword.KeywordService
import com.newssummary.application.keyword.dto.CreateKeywordRequest
import com.newssummary.application.keyword.dto.KeywordResponse
import com.newssummary.application.keyword.dto.UpdateKeywordRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/keywords")
class KeywordController(private val keywordService: KeywordService) {

    @GetMapping
    fun getKeywords(auth: Authentication): List<KeywordResponse> =
        keywordService.getKeywords(auth.principal as Long)

    @PostMapping
    fun createKeyword(
        auth: Authentication,
        @Valid @RequestBody request: CreateKeywordRequest
    ): ResponseEntity<KeywordResponse> {
        val response = keywordService.createKeyword(auth.principal as Long, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PatchMapping("/{id}")
    fun updateKeyword(
        auth: Authentication,
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateKeywordRequest
    ): KeywordResponse =
        keywordService.updateKeyword(auth.principal as Long, id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteKeyword(auth: Authentication, @PathVariable id: Long) =
        keywordService.deleteKeyword(auth.principal as Long, id)
}
