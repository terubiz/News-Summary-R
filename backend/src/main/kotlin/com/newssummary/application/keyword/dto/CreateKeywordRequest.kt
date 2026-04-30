package com.newssummary.application.keyword.dto

import jakarta.validation.constraints.NotBlank

data class CreateKeywordRequest(@field:NotBlank val word: String)
