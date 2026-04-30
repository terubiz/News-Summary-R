package com.newssummary.infrastructure.scheduler

import com.newssummary.application.scheduler.ExecuteScheduledSummaryUseCase
import com.newssummary.domain.scheduler.SchedulerPort
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class SpringSchedulerAdapter(
    private val executeScheduledSummaryUseCase: ExecuteScheduledSummaryUseCase
) : SchedulerPort {

    private val log = LoggerFactory.getLogger(SpringSchedulerAdapter::class.java)

    @Scheduled(fixedDelay = 60000)
    override fun executeScheduledJobs() {
        log.debug("スケジューラー: 毎分チェック開始")
        executeScheduledSummaryUseCase.run()
    }
}
