package com.newssummary.application.summary

import com.newssummary.domain.keyword.Keyword
import com.newssummary.domain.keyword.KeywordRepository
import com.newssummary.domain.summary.AIProviderPort
import com.newssummary.domain.summary.AIProviderResult
import com.newssummary.domain.summary.Summary
import com.newssummary.domain.summary.SummaryRepository
import com.newssummary.domain.summaryconfig.SummaryConfig
import com.newssummary.domain.summaryconfig.SummaryConfigRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.Instant

@DisplayName("GenerateSummaryUseCase")
class GenerateSummaryUseCaseTest {

    private lateinit var keywordRepository: KeywordRepository
    private lateinit var summaryConfigRepository: SummaryConfigRepository
    private lateinit var aiProviderPort: AIProviderPort
    private lateinit var summaryRepository: SummaryRepository
    private lateinit var useCase: GenerateSummaryUseCase

    @BeforeEach
    fun setUp() {
        keywordRepository = mock()
        summaryConfigRepository = mock()
        aiProviderPort = mock()
        summaryRepository = mock()
        useCase = GenerateSummaryUseCase(keywordRepository, summaryConfigRepository, aiProviderPort, summaryRepository)
    }

    @Test
    @DisplayName("execute: アクティブキーワードがない場合 NoActiveKeywordsException をスローする")
    fun executeThrowsNoActiveKeywordsExceptionWhenNoActiveKeywords() {
        val keywords = listOf(Keyword(1L, 1L, "S&P", isActive = false))
        whenever(keywordRepository.findAllByUserId(1L)).thenReturn(keywords)

        assertThrows<NoActiveKeywordsException> { useCase.execute(1L) }
    }

    @Test
    @DisplayName("execute: キーワードが0件の場合 NoActiveKeywordsException をスローする")
    fun executeThrowsNoActiveKeywordsExceptionWhenNoKeywords() {
        whenever(keywordRepository.findAllByUserId(1L)).thenReturn(emptyList())

        assertThrows<NoActiveKeywordsException> { useCase.execute(1L) }
    }

    @Test
    @DisplayName("execute: 正常に要約を生成して SummaryResult を返す")
    fun executeReturnsSummaryResultOnSuccess() {
        val keywords = listOf(Keyword(1L, 1L, "S&P"), Keyword(2L, 1L, "日経225"))
        val config = SummaryConfig(userId = 1L)
        val savedSummary = Summary(10L, 1L, "要約テキスト", "S&P,日経225", Instant.now())

        whenever(keywordRepository.findAllByUserId(1L)).thenReturn(keywords)
        whenever(summaryConfigRepository.findByUserId(1L)).thenReturn(config)
        whenever(aiProviderPort.generateSummary(listOf("S&P", "日経225"), config))
            .thenReturn(AIProviderResult("要約テキスト"))
        whenever(summaryRepository.save(any())).thenReturn(savedSummary)

        val result = useCase.execute(1L)

        assertThat(result.id).isEqualTo(10L)
        assertThat(result.content).isEqualTo("要約テキスト")
        verify(summaryRepository).save(any())
    }

    @Test
    @DisplayName("execute: SummaryConfig が存在しない場合はデフォルト設定を使用する")
    fun executeUsesDefaultConfigWhenSummaryConfigNotFound() {
        val keywords = listOf(Keyword(1L, 1L, "S&P"))
        val savedSummary = Summary(1L, 1L, "要約", "S&P", Instant.now())

        whenever(keywordRepository.findAllByUserId(1L)).thenReturn(keywords)
        whenever(summaryConfigRepository.findByUserId(1L)).thenReturn(null)
        whenever(aiProviderPort.generateSummary(any(), any())).thenReturn(AIProviderResult("要約"))
        whenever(summaryRepository.save(any())).thenReturn(savedSummary)

        val result = useCase.execute(1L)

        assertThat(result.id).isEqualTo(1L)
        verify(aiProviderPort).generateSummary(eq(listOf("S&P")), any())
    }

    @Test
    @DisplayName("execute: AIProviderPort が例外をスローした場合に伝播する")
    fun executePropagatesAIProviderException() {
        val keywords = listOf(Keyword(1L, 1L, "S&P"))
        whenever(keywordRepository.findAllByUserId(1L)).thenReturn(keywords)
        whenever(summaryConfigRepository.findByUserId(1L)).thenReturn(SummaryConfig(userId = 1L))
        whenever(aiProviderPort.generateSummary(any(), any()))
            .thenThrow(AIProviderException("Gemini error"))

        assertThrows<AIProviderException> { useCase.execute(1L) }
    }
}
