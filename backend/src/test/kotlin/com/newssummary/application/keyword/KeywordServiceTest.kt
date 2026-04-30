package com.newssummary.application.keyword

import com.newssummary.application.keyword.dto.CreateKeywordRequest
import com.newssummary.application.keyword.dto.UpdateKeywordRequest
import com.newssummary.domain.keyword.Keyword
import com.newssummary.domain.keyword.KeywordRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*

@DisplayName("KeywordService")
class KeywordServiceTest {

    private lateinit var keywordRepository: KeywordRepository
    private lateinit var service: KeywordService

    @BeforeEach
    fun setUp() {
        keywordRepository = mock()
        service = KeywordService(keywordRepository)
    }

    @Test
    @DisplayName("createKeyword: 新規キーワードを保存して KeywordResponse を返す")
    fun createKeywordSavesAndReturnsResponse() {
        whenever(keywordRepository.existsByUserIdAndWord(1L, "S&P")).thenReturn(false)
        whenever(keywordRepository.save(any())).thenAnswer {
            (it.arguments[0] as Keyword).also { k ->
                k.javaClass.getDeclaredField("id").apply { isAccessible = true }.set(k, 10L)
            }
        }

        val result = service.createKeyword(1L, CreateKeywordRequest("S&P"))

        assertThat(result.word).isEqualTo("S&P")
        assertThat(result.userId).isEqualTo(1L)
        assertThat(result.isActive).isTrue()
    }

    @Test
    @DisplayName("createKeyword: 重複キーワードで DuplicateKeywordException をスローする")
    fun createKeywordThrowsDuplicateExceptionForDuplicateWord() {
        whenever(keywordRepository.existsByUserIdAndWord(1L, "S&P")).thenReturn(true)
        assertThrows<DuplicateKeywordException> {
            service.createKeyword(1L, CreateKeywordRequest("S&P"))
        }
    }

    @Test
    @DisplayName("getKeywords: userId に紐づく全キーワードを返す")
    fun getKeywordsReturnsAllKeywordsForUser() {
        val keywords = listOf(Keyword(1L, 1L, "S&P"), Keyword(2L, 1L, "日経225"))
        whenever(keywordRepository.findAllByUserId(1L)).thenReturn(keywords)

        val result = service.getKeywords(1L)

        assertThat(result).hasSize(2)
    }

    @Test
    @DisplayName("updateKeyword: 存在しないキーワードで KeywordNotFoundException をスローする")
    fun updateKeywordThrowsNotFoundForMissingKeyword() {
        whenever(keywordRepository.findById(99L)).thenReturn(null)
        assertThrows<KeywordNotFoundException> {
            service.updateKeyword(1L, 99L, UpdateKeywordRequest(false))
        }
    }

    @Test
    @DisplayName("updateKeyword: 所有者不一致で ForbiddenResourceException をスローする")
    fun updateKeywordThrowsForbiddenForWrongOwner() {
        val keyword = Keyword(1L, 2L, "S&P")
        whenever(keywordRepository.findById(1L)).thenReturn(keyword)
        assertThrows<ForbiddenResourceException> {
            service.updateKeyword(1L, 1L, UpdateKeywordRequest(false))
        }
    }

    @Test
    @DisplayName("deleteKeyword: 正常に削除する")
    fun deleteKeywordDeletesSuccessfully() {
        val keyword = Keyword(1L, 1L, "S&P")
        whenever(keywordRepository.findById(1L)).thenReturn(keyword)

        service.deleteKeyword(1L, 1L)

        verify(keywordRepository).deleteById(1L)
    }
}
