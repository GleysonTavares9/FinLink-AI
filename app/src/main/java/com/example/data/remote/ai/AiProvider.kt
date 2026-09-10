package com.example.data.remote.ai

import android.graphics.Bitmap
import com.example.data.gemini.ScannedReceiptResult

/**
 * Interface plugável para provedores de Inteligência Artificial.
 * Permite alternar entre Google Gemini (remoto), modelos locais/on-device ou outros serviços
 * sem impactar a arquitetura ou as regras de negócio do aplicativo.
 */
interface AiProvider {
    val providerName: String

    /**
     * Analisa uma imagem de cupom fiscal multimodalmente e extrai dados estruturados.
     */
    suspend fun analyzeReceipt(bitmap: Bitmap, householdContext: String? = null): Result<ScannedReceiptResult>

    /**
     * Gera recomendações e conselhos financeiros baseados no histórico de despesas e composição familiar.
     */
    suspend fun getFinancialAdvice(
        expensesSummary: String,
        userPrompt: String,
        householdContext: String? = null
    ): Result<String>

    /**
     * Analisa texto ou HTML de NFC-e/SEFAZ e extrai dados fiscais estruturados.
     */
    suspend fun analyzeReceiptText(text: String, householdContext: String? = null): Result<ScannedReceiptResult>
}
