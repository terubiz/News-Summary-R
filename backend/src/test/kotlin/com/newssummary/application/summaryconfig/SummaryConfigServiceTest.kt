package com.newssummary.application.summaryconfig

import com.newssummary.application.summaryconfig.dto.SummaryConfigRequest
import com.newssummary.domain.summaryconfig.SummaryConfig
import com.newssummary.domain.summaryconfig.SummaryConfigRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

@DisplayName("SummaryConfigService")
class SummaryConfigServiceTest {

    private lateinit var repository: SummaryConfigRepository
    private lateinit var service: SummaryConfigService

    @BeforeEach
    fun setUp() {
        repository = mock()
        service = SummaryConfigService(repository)
    }

    @Test
    @DisplayName("getOrCreateConfig: 既存設定を返す")
    fun getOrCreateConfigReturnsExistingConfig() {
        val config = SummaryConfig(1L, 1L, "08:00", 3, 20, "gemini")
        whenever(repository.findByUserId(1L)).thenReturn(config)

        val result = service.getOrCreateConfig(1L)

        assertThat(result.executionTime).isEqualTo("08:00")
        verify(repository, never()).save(any())
    }

    @Test
    @DisplayName("getOrCreateConfig: 設定が未存在の場合はデフォルト値で新規作成する")
    fun getOrCreateConfigCreatesDefaultConfigWhenNotFound() {
        whenever(repository.findByUserId(1L)).thenReturn(null)
        whenever(repository.save(any())).thenAnswer { it.arguments[0] }

        val result = service.getOrCreateConfig(1L)

        assertThat(result.executionTime).isEqualTo("07:00")
        assertThat(result.lookbackDays).isEqualTo(1)
        assertThat(result.fetchCount).isEqualTo(10)
        verify(repository).save(any())
    }

    @Test
    @DisplayName("updateConfig: 設定を更新して返す")
    fun updateConfigUpdatesAndReturnsConfig() {
        val config = SummaryConfig(1L, 1L, "07:00", 1, 10, "gemini")
        whenever(repository.findByUserId(1L)).thenReturn(config)
        whenever(repository.save(any())).thenAnswer { it.arguments[0] }

        val request = SummaryConfigRequest("09:00", 7, 50, "gemini")
        val result = service.updateConfig(1L, request)

        assertThat(result.executionTime).isEqualTo("09:00")
        assertThat(result.lookbackDays).isEqualTo(7)
        assertThat(result.fetchCount).isEqualTo(50)
    }
}
