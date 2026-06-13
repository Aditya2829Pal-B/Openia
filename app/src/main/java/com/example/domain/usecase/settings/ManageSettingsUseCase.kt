package com.example.domain.usecase.settings

import com.example.network.GeminiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

data class AppSettingsModel(
    val darkTheme: Boolean,
    val geminiApiKeyConfigured: Boolean,
    val offlineCacheEnabled: Boolean
)

class ManageSettingsUseCase {

    fun getSettingsFlow(): Flow<AppSettingsModel> {
        return flowOf(
            AppSettingsModel(
                darkTheme = true, // Openia defaults to premium dark
                geminiApiKeyConfigured = GeminiClient.isApiKeyAvailable,
                offlineCacheEnabled = true
            )
        )
    }

    fun isGeminiActive(): Boolean {
        return GeminiClient.isApiKeyAvailable
    }
}
