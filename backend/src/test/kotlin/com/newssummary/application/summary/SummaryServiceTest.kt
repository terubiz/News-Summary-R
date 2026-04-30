package com.newssummary.application.summary

import com.newssummary.domain.summary.Summary
import com.newssummary.domain.summary.SummaryRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant

@DisplayName("SummaryService")
class SummaryServiceTest {

    private lateinit var summaryRepository: SummaryRepository
    private lateinit var service: SummaryService

    @BeforeEach
    fun setUp() {
        summaryRepository = mock()
        service = SummaryService(summaryRepository)
    }

    @Test
    @DisplayName("getSummaries: userId に紐づく要約一覧を降順で返す")
    fun getSummariesReturnsAllSummariesForUser() {
        val summaries = listOf(
            Summary(2L, 1L, "要約2", "S&P", Instant.now()),
            Summary(1L, 1L, "要約1", "日経225", Instant.now())
        )
        whenever(summaryRepository.findAllByUserIdOrderByGeneratedAtDesc(1L)).thenReturn(summaries)

        val result = service.getSummaries(1L)

        assertThat(result).hasSize(2)
        assertThat(result[0].id).isEqualTo(2L)
    }

    @Test
    @DisplayName("getSummaries: 要約が存在しない場合は空リストを返す")
    fun getSummariesReturnsEmptyListWhenNoSummaries() {
        whenever(summaryRepository.findAllByUserIdOrderByGeneratedAtDesc(1L)).thenReturn(emptyList())

        val result = service.getSummaries(1L)

        assertThat(result).isEmpty()
    }
}
