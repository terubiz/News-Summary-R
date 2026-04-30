package com.newssummary.application.keyword.dto

import jakarta.validation.constraints.NotNull

data class UpdateKeywordRequest(@field:NotNull val isActive: Boolean)
