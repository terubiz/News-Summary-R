package com.newssummary.application.scheduler

import com.newssummary.application.summary.AIProviderException
import com.newssummary.application.summary.GenerateSummaryUseCase
import com.newssummary.application.summary.NoActiveKeywordsException
import com.newssummary.domain.scheduler.JobExecutionHistory
import com.newssummary.domain.scheduler.JobExecutionHistoryRepository
import com.newssummary.domain.summaryconfig.SummaryConfigRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

@Service
class ExecuteScheduledSummaryUseCase(
    private val summaryConfigRepository: SummaryConfigRepository,
    private val generateSummaryUseCase: GenerateSummaryUseCase,
    private val jobExecutionHistoryRepository: JobExecutionHistoryRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    private val log = LoggerFactory.getLogger(ExecuteScheduledSummaryUseCase::class.java)
    private val runningJobs = ConcurrentHashMap<Long, Boolean>()
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun run() {
        val currentTime = LocalTime.now(clock).format(timeFormatter)
        val configs = summaryConfigRepository.findAll()
        var attempted = 0
        var succeeded = 0
        var failed = 0

        configs.forEach { config ->
            if (config.executionTime != currentTime) return@forEach

            val userId = config.userId
            if (runningJobs.putIfAbsent(userId, true) != null) {
                log.warn("スケジューラー: userId={} のジョブが既に実行中のためスキップ", userId)
                return@forEach
            }

            attempted++
            try {
                val startTime = System.currentTimeMillis()
                val result = generateSummaryUseCase.execute(userId)
                val elapsed = System.currentTimeMillis() - startTime
                log.info("スケジューラー: ジョブ成功 userId={}, generatedAt={}, 処理時間={}ms", userId, result.generatedAt, elapsed)
                jobExecutionHistoryRepository.save(
                    JobExecutionHistory(userId = userId, status = "SUCCESS")
                )
                succeeded++
            } catch (e: NoActiveKeywordsException) {
                log.warn("スケジューラー: アクティブキーワードなし userId={}, message={}", userId, e.message)
                jobExecutionHistoryRepository.save(
                    JobExecutionHistory(userId = userId, status = "FAILURE", errorMessage = e.message)
                )
                failed++
            } catch (e: AIProviderException) {
                log.error("スケジューラー: AIプロバイダーエラー userId={}", userId, e)
                jobExecutionHistoryRepository.save(
                    JobExecutionHistory(userId = userId, status = "FAILURE", errorMessage = e.message)
                )
                failed++
            } catch (e: Exception) {
                log.error("スケジューラー: 予期せぬエラー userId={}", userId, e)
                jobExecutionHistoryRepository.save(
                    JobExecutionHistory(userId = userId, status = "FAILURE", errorMessage = e.message)
                )
                failed++
            } finally {
                runningJobs.remove(userId)
            }
        }

        if (attempted > 0) {
            log.info("スケジューラー: 実行完了 試行={}, 成功={}, 失敗={}", attempted, succeeded, failed)
        }
    }
}
