package com.newssummary.infrastructure.ai

import com.newssummary.application.summary.UnsupportedAIProviderException
import com.newssummary.domain.summary.AIProviderResult
import com.newssummary.domain.summaryconfig.SummaryConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*

@DisplayName("AIProviderRouter")
class AIProviderRouterTest {

    private lateinit var geminiAdapter: GeminiApiAdapter
    private lateinit var router: AIProviderRouter

    @BeforeEach
    fun setUp() {
        geminiAdapter = mock()
        router = AIProviderRouter(geminiAdapter)
    }

    @Test
    @DisplayName("generateSummary: aiProviderName が gemini のとき GeminiAdapter に委譲する")
    fun generateSummaryDelegatesToGeminiAdapterForGeminiProvider() {
        val config = SummaryConfig(userId = 1L, aiProviderName = "gemini")
        whenever(geminiAdapter.generateSummary(any(), any())).thenReturn(AIProviderResult("要約"))

        val result = router.generateSummary(listOf("S&P"), config)

        assertThat(result.summaryText).isEqualTo("要約")
        verify(geminiAdapter).generateSummary(listOf("S&P"), config)
    }

    @Test
    @DisplayName("generateSummary: 未知の aiProviderName で UnsupportedAIProviderException をスローする")
    fun generateSummaryThrowsUnsupportedProviderExceptionForUnknownProvider() {
        val config = SummaryConfig(userId = 1L, aiProviderName = "openai")

        assertThrows<UnsupportedAIProviderException> {
            router.generateSummary(listOf("S&P"), config)
        }
    }
}
