package com.example.data.remote.nfce

import com.example.data.gemini.ScannedReceiptResult
import com.example.data.remote.ai.AiProvider
import com.example.data.remote.ai.AiProviderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Responsável por buscar os dados oficiais da NFC-e no portal da SEFAZ
 * a partir da URL do QR Code ou chave de acesso de 44 dígitos,
 * combinando raspagem HTML especializada e análise inteligente via Gemini 3.6 Flash.
 */
class NfceFetcher(
    private val parser: NfceParser = NfceParser(),
    private val aiProvider: AiProvider = AiProviderFactory.createProvider()
) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun fetchReceipt(urlOrKey: String): Result<ScannedReceiptResult> = withContext(Dispatchers.IO) {
        val trimmed = urlOrKey.trim()

        var htmlBody = ""
        // Se for uma URL http/https válida, tenta fazer requisição HTTP para obter o HTML oficial da SEFAZ
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            try {
                val request = Request.Builder()
                    .url(trimmed)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 Chrome/122.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .get()
                    .build()

                val response = okHttpClient.newCall(request).execute()
                htmlBody = response.body?.string() ?: ""
            } catch (e: Exception) {
                // Prossegue mesmo com falha de conexão HTTP direta da SEFAZ
            }
        }

        // 1. Tenta análise estruturada com o parser SEFAZ
        val parsed = parser.parse(htmlBody, trimmed)

        // Se extraiu valor total positivo e nome específico da loja, retorna direto
        if (parsed.totalAmount > 0.0 && parsed.items.isNotEmpty() && !parsed.storeName.contains("Cupom Fiscal NFC-e")) {
            return@withContext Result.success(parsed)
        }

        // 2. Se a consulta local ficou incompleta ou com valor zerado, utiliza o Gemini 3.6 Flash para extrair os dados fiscais
        val contentForAi = if (htmlBody.isNotBlank()) htmlBody else trimmed
        try {
            val aiResult = aiProvider.analyzeReceiptText(contentForAi)
            if (aiResult.isSuccess) {
                val aiReceipt = aiResult.getOrNull()
                if (aiReceipt != null && (aiReceipt.totalAmount > 0.0 || aiReceipt.items.isNotEmpty())) {
                    val finalTotal = if (parsed.totalAmount > 0.0 && aiReceipt.totalAmount == 0.0) parsed.totalAmount else aiReceipt.totalAmount
                    return@withContext Result.success(
                        aiReceipt.copy(
                            totalAmount = finalTotal,
                            storeName = if (aiReceipt.storeName.isNotBlank() && !aiReceipt.storeName.contains("Estabelecimento Comercial")) aiReceipt.storeName else parsed.storeName
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Ignora falha de IA e mantém o resultado estruturado local
        }

        Result.success(parsed)
    }
}
