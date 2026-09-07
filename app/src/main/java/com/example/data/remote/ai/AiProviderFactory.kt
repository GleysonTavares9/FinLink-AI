package com.example.data.remote.ai

enum class AiProviderType {
    GEMINI_FLASH_3_6,
    GEMINI_FLASH_3_5,
    LOCAL_FALLBACK
}

/**
 * Fábrica para instanciar o provedor de IA configurado.
 */
object AiProviderFactory {

    fun createProvider(type: AiProviderType = AiProviderType.GEMINI_FLASH_3_6): AiProvider {
        return when (type) {
            AiProviderType.GEMINI_FLASH_3_6 -> GeminiProvider(modelName = "gemini-3.6-flash")
            AiProviderType.GEMINI_FLASH_3_5 -> GeminiProvider(modelName = "gemini-3.6-flash")
            AiProviderType.LOCAL_FALLBACK -> GeminiProvider(modelName = "gemini-3.6-flash")
        }
    }
}
