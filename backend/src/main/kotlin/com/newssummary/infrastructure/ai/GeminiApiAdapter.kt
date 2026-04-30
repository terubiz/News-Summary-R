package com.newssummary.infrastructure.ai

import com.fasterxml.jackson.annotation.JsonProperty
import com.newssummary.application.summary.AIProviderException
import com.newssummary.domain.summary.AIProviderPort
import com.newssummary.domain.summary.AIProviderResult
import com.newssummary.domain.summaryconfig.SummaryConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class GeminiApiAdapter(
    @Value("\${gemini.api-key}") private val apiKey: String,
    @Value("\${gemini.api-url:https://generativelanguage.googleapis.com/v1beta}") private val apiUrl: String,
    @Value("\${gemini.model-name:gemini-1.5-pro}") private val modelName: String
) : AIProviderPort {

    private val log = LoggerFactory.getLogger(GeminiApiAdapter::class.java)
    private val restClient = RestClient.create()

    override fun generateSummary(keywords: List<String>, config: SummaryConfig): AIProviderResult {
        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(buildPrompt(keywords, config.lookbackDays))))),
            tools = listOf(GeminiTool(googleSearch = emptyMap()))
        )
        try {
            val response = restClient.post()
                .uri("$apiUrl/models/$modelName:generateContent?key=$apiKey")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(GeminiResponse::class.java)

            val text = response?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (text.isNullOrBlank()) throw AIProviderException("Empty response from Gemini")
            return AIProviderResult(text)
        } catch (e: AIProviderException) {
            throw e
        } catch (e: Exception) {
            log.error("Gemini API呼び出しに失敗しました: {}", e.message, e)
            throw AIProviderException("Gemini API call failed: ${e.message}", e)
        }
    }

    private fun buildPrompt(keywords: List<String>, lookbackDays: Int) =
        """
        以下のキーワードに関する最新ニュースを調査し、各キーワードについて要約してください。
        キーワード: ${keywords.joinToString(", ")}
        過去${lookbackDays}日分のニュースを対象にしてください。
        """.trimIndent()

    private data class GeminiRequest(
        val contents: List<GeminiContent>,
        val tools: List<GeminiTool>? = null
    )

    private data class GeminiContent(
        val parts: List<GeminiPart>,
        val role: String = "user"
    )

    private data class GeminiPart(val text: String)

    private data class GeminiTool(
        @JsonProperty("googleSearch") val googleSearch: Map<String, Any>? = null
    )

    private data class GeminiResponse(
        val candidates: List<GeminiCandidate>? = null
    )

    private data class GeminiCandidate(
        val content: GeminiContent? = null
    )
}
