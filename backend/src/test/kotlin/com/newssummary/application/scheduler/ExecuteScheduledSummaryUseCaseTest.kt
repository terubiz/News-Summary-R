package com.newssummary.application.scheduler

import com.newssummary.application.summary.AIProviderException
import com.newssummary.application.summary.GenerateSummaryUseCase
import com.newssummary.application.summary.NoActiveKeywordsException
import com.newssummary.application.summary.dto.SummaryResult
import com.newssummary.domain.scheduler.JobExecutionHistory
import com.newssummary.domain.scheduler.JobExecutionHistoryRepository
import com.newssummary.domain.summaryconfig.SummaryConfig
import com.newssummary.domain.summaryconfig.SummaryConfigRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@DisplayName("ExecuteScheduledSummaryUseCase")
class ExecuteScheduledSummaryUseCaseTest {

    private lateinit var summaryConfigRepository: SummaryConfigRepository
    private lateinit var generateSummaryUseCase: GenerateSummaryUseCase
    private lateinit var jobExecutionHistoryRepository: JobExecutionHistoryRepository
    private lateinit var useCase: ExecuteScheduledSummaryUseCase

    // 固定時刻: 2024-01-01T07:00:00Z (HH:mm = "07:00")
    private val fixedClock = Clock.fixed(
        Instant.parse("2024-01-01T07:00:00Z"),
        ZoneId.of("UTC")
    )

    @BeforeEach
    fun setUp() {
        summaryConfigRepository = mock()
        generateSummaryUseCase = mock()
        jobExecutionHistoryRepository = mock()
        useCase = ExecuteScheduledSummaryUseCase(
            summaryConfigRepository,
            generateSummaryUseCase,
            jobExecutionHistoryRepository,
            fixedClock
        )

        whenever(jobExecutionHistoryRepository.save(any())).thenAnswer { it.arguments[0] }
    }

    @Test
    @DisplayName("run: SummaryConfigが0件の場合は何もしない")
    fun runDoesNothingWhenNoSummaryConfigs() {
        whenever(summaryConfigRepository.findAll()).thenReturn(emptyList())

        useCase.run()

        verify(generateSummaryUseCase, never()).execute(any())
        verify(jobExecutionHistoryRepository, never()).save(any())
    }

    @Test
    @DisplayName("run: executionTimeが現在時刻と不一致の場合はジョブを実行しない")
    fun runSkipsWhenExecutionTimeDoesNotMatch() {
        val config = SummaryConfig(userId = 1L, executionTime = "09:00")
        whenever(summaryConfigRepository.findAll()).thenReturn(listOf(config))

        useCase.run()

        verify(generateSummaryUseCase, never()).execute(any())
        verify(jobExecutionHistoryRepository, never()).save(any())
    }

    @Test
    @DisplayName("run: executionTimeが現在時刻と一致する場合にGenerateSummaryUseCaseを呼び出す")
    fun runExecutesJobWhenExecutionTimeMatches() {
        val config = SummaryConfig(userId = 1L, executionTime = "07:00")
        val summaryResult = SummaryResult(1L, "要約", "キーワード", Instant.now())
        whenever(summaryConfigRepository.findAll()).thenReturn(listOf(config))
        whenever(generateSummaryUseCase.execute(1L)).thenReturn(summaryResult)

        useCase.run()

        verify(generateSummaryUseCase).execute(1L)
    }

    @Test
    @DisplayName("run: ジョブ成功時にSUCCESSステータスで履歴を保存する")
    fun runSavesSuccessHistoryOnSuccess() {
        val config = SummaryConfig(userId = 1L, executionTime = "07:00")
        val summaryResult = SummaryResult(1L, "要約", "キーワード", Instant.now())
        whenever(summaryConfigRepository.findAll()).thenReturn(listOf(config))
        whenever(generateSummaryUseCase.execute(1L)).thenReturn(summaryResult)

        useCase.run()

        val captor = argumentCaptor<JobExecutionHistory>()
        verify(jobExecutionHistoryRepository).save(captor.capture())
        assertThat(captor.firstValue.status).isEqualTo("SUCCESS")
        assertThat(captor.firstValue.userId).isEqualTo(1L)
        assertThat(captor.firstValue.errorMessage).isNull()
    }

    @Test
    @DisplayName("run: NoActiveKeywordsExceptionのときFAILURE履歴を保存してWARNログを出力する")
    fun runSavesFailureHistoryOnNoActiveKeywordsException() {
        val config = SummaryConfig(userId = 1L, executionTime = "07:00")
        whenever(summaryConfigRepository.findAll()).thenReturn(listOf(config))
        whenever(generateSummaryUseCase.execute(1L)).thenThrow(NoActiveKeywordsException(1L))

        useCase.run()

        val captor = argumentCaptor<JobExecutionHistory>()
        verify(jobExecutionHistoryRepository).save(captor.capture())
        assertThat(captor.firstValue.status).isEqualTo("FAILURE")
        assertThat(captor.firstValue.userId).isEqualTo(1L)
        assertThat(captor.firstValue.errorMessage).isNotNull()
    }

    @Test
    @DisplayName("run: AIProviderExceptionのときFAILURE履歴を保存してERRORログを出力する")
    fun runSavesFailureHistoryOnAIProviderException() {
        val config = SummaryConfig(userId = 1L, executionTime = "07:00")
        whenever(summaryConfigRepository.findAll()).thenReturn(listOf(config))
        whenever(generateSummaryUseCase.execute(1L)).thenThrow(AIProviderException("Gemini error"))

        useCase.run()

        val captor = argumentCaptor<JobExecutionHistory>()
        verify(jobExecutionHistoryRepository).save(captor.capture())
        assertThat(captor.firstValue.status).isEqualTo("FAILURE")
        assertThat(captor.firstValue.errorMessage).contains("Gemini error")
    }

    @Test
    @DisplayName("run: あるユーザーでジョブ失敗しても次ユーザーのジョブが継続される")
    fun runContinuesToNextUserAfterFailure() {
        val config1 = SummaryConfig(id = 1L, userId = 1L, executionTime = "07:00")
        val config2 = SummaryConfig(id = 2L, userId = 2L, executionTime = "07:00")
        val summaryResult = SummaryResult(10L, "要約", "キーワード", Instant.now())
        whenever(summaryConfigRepository.findAll()).thenReturn(listOf(config1, config2))
        whenever(generateSummaryUseCase.execute(1L)).thenThrow(RuntimeException("予期せぬエラー"))
        whenever(generateSummaryUseCase.execute(2L)).thenReturn(summaryResult)

        useCase.run()

        verify(generateSummaryUseCase).execute(1L)
        verify(generateSummaryUseCase).execute(2L)
        verify(jobExecutionHistoryRepository, times(2)).save(any())
    }

    @Test
    @DisplayName("run: 複数ユーザーのexecutionTimeが一致する場合、全ユーザー分実行する")
    fun runExecutesJobsForAllMatchingUsers() {
        val config1 = SummaryConfig(id = 1L, userId = 1L, executionTime = "07:00")
        val config2 = SummaryConfig(id = 2L, userId = 2L, executionTime = "07:00")
        val config3 = SummaryConfig(id = 3L, userId = 3L, executionTime = "09:00")
        val summaryResult = SummaryResult(10L, "要約", "キーワード", Instant.now())
        whenever(summaryConfigRepository.findAll()).thenReturn(listOf(config1, config2, config3))
        whenever(generateSummaryUseCase.execute(any())).thenReturn(summaryResult)

        useCase.run()

        verify(generateSummaryUseCase).execute(1L)
        verify(generateSummaryUseCase).execute(2L)
        verify(generateSummaryUseCase, never()).execute(3L)
    }
}
