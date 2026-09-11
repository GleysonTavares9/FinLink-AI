package com.example.data.remote.ai

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.gemini.ScannedReceiptResult
import com.example.data.ocr.ReceiptOcrEngine
import com.example.util.parseBrDouble
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Implementação do AiProvider utilizando a API Google Gemini (modelo gemini-3.6-flash)
 * combinada com o motor de OCR On-Device ML Kit para máxima precisão na leitura de cupons fiscais brasileiros.
 */
class GeminiProvider(
    private val modelName: String = "gemini-3.6-flash",
    private val ocrEngine: ReceiptOcrEngine = ReceiptOcrEngine()
) : AiProvider {

    override val providerName: String = "Google Gemini ($modelName) + ML Kit OCR"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun analyzeReceipt(bitmap: Bitmap, householdContext: String?): Result<ScannedReceiptResult> = withContext(Dispatchers.IO) {
        // 1. Extração local prévia com ML Kit On-Device (rápido, sem rede e confiável)
        val localOcrResult = ocrEngine.analyzeReceipt(bitmap)

        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.success(localOcrResult)
        }

        // Redimensiona o bitmap para no máximo 1280px para envio leve e ultra-rápido à IA
        val scaledBitmap = bitmap.scalePreservingAspect(2560)
        val base64Image = scaledBitmap.toBase64()

                // Requisitos do Guia Oficial de Leitura de NF brasileira (NF-e 55 / NFC-e 65)
        val promptText = """
            Você é um extrator de dados fiscais brasileiros (NF-e modelo 55 / NFC-e modelo 65).
            Analise a imagem da nota fiscal e preencha o schema JSON abaixo.

            Regras:
            1. Não invente valores. Se um campo não estiver legível, use null.
            2. Se houver dúvida razoável sobre um valor, marque-o em "camposIncertos".
            3. Preserve a formatação original de CNPJ, CPF e chave de acesso (apenas números).
            4. Valores monetários em formato numérico (ponto como separador decimal).

            Texto auxiliar detectado via OCR On-Device (para suporte):
            ${'$'}{localOcrResult.rawText.take(2000)}
        """.trimIndent()

        val fiscalSchema = """
          {
            "type": "OBJECT",
            "properties": {
              "chaveAcesso": { "type": "STRING" },
              "numero": { "type": "STRING" },
              "serie": { "type": "STRING" },
              "modelo": { "type": "STRING", "enum": ["55", "65"] },
              "dataEmissao": { "type": "STRING" },
              "naturezaOperacao": { "type": "STRING" },
              "emitente": {
                "type": "OBJECT",
                "properties": {
                  "razaoSocial": { "type": "STRING" },
                  "cnpj": { "type": "STRING" },
                  "inscricaoEstadual": { "type": "STRING" }
                }
              },
              "destinatario": {
                "type": "OBJECT",
                "properties": {
                  "nome": { "type": "STRING" },
                  "cnpjCpf": { "type": "STRING" }
                }
              },
              "itens": {
                "type": "ARRAY",
                "items": {
                  "type": "OBJECT",
                  "properties": {
                    "codigo": { "type": "STRING" },
                    "descricao": { "type": "STRING" },
                    "ncm": { "type": "STRING" },
                    "cfop": { "type": "STRING" },
                    "quantidade": { "type": "NUMBER" },
                    "valorUnitario": { "type": "NUMBER" },
                    "valorTotal": { "type": "NUMBER" }
                  }
                }
              },
              "valorTotalProdutos": { "type": "NUMBER" },
              "valorIcms": { "type": "NUMBER" },
              "valorTotalNota": { "type": "NUMBER" },
              "camposIncertos": {
                "type": "ARRAY",
                "items": { "type": "STRING" }
              }
            },
            "required": ["chaveAcesso", "numero", "modelo", "emitente", "itens", "valorTotalNota"]
          }
        """.trimIndent()

        val jsonPayload = """
            {
              "contents": [
                {
                  "parts": [
                    { "text": ${'$'}{JSONObject.quote(promptText)} },
                    {
                      "inlineData": {
                        "mimeType": "image/jpeg",
                        "data": "$base64Image"
                      }
                    }
                  ]
                }
              ],
              "generationConfig": {
                "temperature": 0.1,
                "responseMimeType": "application/json",
                "responseSchema": ${'$'}fiscalSchema
              }
            }
        """.trimIndent()

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey")
            .post(jsonPayload.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.success(localOcrResult)
            }

            val extractedJsonText = extractTextFromGeminiResponse(responseBody)
            val parsedResult = parseReceiptJson(extractedJsonText, localOcrResult)
            Result.success(parsedResult)
        } catch (e: Exception) {
            Result.success(localOcrResult)
        }
    }

    override suspend fun analyzeReceiptText(text: String, householdContext: String?): Result<ScannedReceiptResult> = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("API Key não configurada"))
        }

        val promptText = """
            Você é um especialista em leitura de dados de NFC-e e cupons fiscais brasileiros (SEFAZ / SAT / Danfe).
            Examine detalhadamente os dados fiscais e o conteúdo HTML/URL abaixo e extraia com precisão absoluta as informações no seguinte formato JSON estrito:
            {
              "storeName": "Nome Fantasia ou Razão Social do Estabelecimento",
              "totalAmount": 123.45,
              "transactionDate": "YYYY-MM-DD",
              "category": "Mercado, Alimentação, Transporte, Saúde, Moradia, Lazer, ou Outros",
              "paymentMethod": "PIX, Cartão de Crédito, Cartão de Débito, Dinheiro, ou NFC-e",
              "items": ["1x Item Descrição - R$ 10.00", "2x Item Descrição - R$ 15.00"],
              "confidence": "ALTA (SEFAZ Oficial)",\n              "familyAnalysis": "Sua anÃ¡lise do suprimento familiar baseada na compra (Opcional)"\n            }
            Atenção crítica: o campo totalAmount DEVE ser o valor total da nota em número decimal com ponto (ex: 89.50).
            Se houver \'Contexto Familiar\', use-o para gerar a anÃ¡lise de suprimento em \'familyAnalysis\'.\n            Retorne estritamente o JSON.\n
            MUITO IMPORTANTE:
            Se a imagem OU texto nÃ£o for um cupom fiscal, nota fiscal, recibo de pagamento ou nfce legÃ­vel, VOCÃŠ DEVE OBRIGATORIAMENTE retornar o valor totalAmount como 0.0 e a lista items VAZIA. NÃƒO INVENTE PRODUTOS OU VALORES se nÃ£o estiverem claramente na imagem.
\n            Contexto Familiar (se houver): ${householdContext ?: "Nenhum"}

            Conteúdo da Nota/Cupom:
            ${text.take(12000)}
        """.trimIndent()

        val jsonPayload = """
            {
              "contents": [
                {
                  "parts": [
                    { "text": ${JSONObject.quote(promptText)} }
                  ]
                }
              ],
              "generationConfig": {
                "temperature": 0.1,
                "responseMimeType": "application/json"
              }
            }
        """.trimIndent()

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey")
            .post(jsonPayload.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Erro na API Gemini: ${response.code}"))
            }

            val extractedJsonText = extractTextFromGeminiResponse(responseBody)
            val parsedResult = parseReceiptJson(extractedJsonText, null)
            Result.success(parsedResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFinancialAdvice(
        expensesSummary: String,
        userPrompt: String,
        householdContext: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }

        val householdSection = if (!householdContext.isNullOrBlank()) {
            "\nContexto da Composição Familiar:\n$householdContext\n"
        } else {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.success(
                "💡 **Análise Financeira Inteligente (Modo Offline/Demonstração)**:\n\n" +
                "• **Diagnóstico de Gastos**: Registre cupons fiscais e lançamentos para mapear o custo real familiar.\n" +
                (if (!householdContext.isNullOrBlank()) "• **Perfil Familiar**: $householdContext\n" else "") +
                "• **Regra 50-30-20 Adaptada**: Aloque 50% em necessidades básicas da família (Alimentação, Moradia, Saúde e Educação), 30% em lazer/estilo de vida e 20% para reserva de emergência.\n" +
                "• **Economia em Mercado**: Compras em atacarejos e acompanhamento de itens da cesta básica geram economia média de 12% a 20% no orçamento familiar."
            )
        }

        val promptText = """
            Você é um consultor financeiro sênior especializado em finanças familiares e economia brasileira no aplicativo FinLink AI (Cupom & Finanças AI).
            
            Relação real das despesas registradas pelo usuário:
            $expensesSummary
            $householdSection
            Pergunta/Solicitação do usuário: $userPrompt

            Instruções:
            1. Seja claro, empático, motivador e altamente técnico.
            2. Se houver informações de composição familiar (adultos e crianças), calcule o impacto per capita e forneça recomendações adaptadas ao custo real por pessoa da casa.
            3. Destaque dicas práticas (alocação de renda, economia em compras de supermercado via cupom fiscal, reserva de emergência).
            4. Responda em português brasileiro com formatação Markdown impecável.
        """.trimIndent()

        val jsonPayload = """
            {
              "contents": [
                {
                  "parts": [
                    { "text": ${JSONObject.quote(promptText)} }
                  ]
                }
              ]
            }
        """.trimIndent()

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey")
            .post(jsonPayload.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.success(
                    "Acompanhe o painel de despesas e o módulo familiar para manter as metas de orçamento equilibradas."
                )
            }

            val text = extractTextFromGeminiResponse(responseBody)
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractTextFromGeminiResponse(responseBody: String): String {
        return try {
            val jsonObj = JSONObject(responseBody)
            val candidates = jsonObj.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts != null && parts.length() > 0) {
                val sb = StringBuilder()
                for (i in 0 until parts.length()) {
                    val p = parts.optJSONObject(i)
                    val t = p?.optString("text")
                    if (!t.isNullOrBlank()) {
                        sb.append(t)
                    }
                }
                if (sb.isNotEmpty()) return sb.toString()
            }
            responseBody
        } catch (e: Exception) {
            responseBody
        }
    }

    private fun parseReceiptJson(
        jsonText: String,
        fallbackResult: ScannedReceiptResult?
    ): ScannedReceiptResult {
        return try {
            val cleanJson = extractJsonSubstring(jsonText)
            val obj = JSONObject(cleanJson)

            // 1. Razão Social / Nome do Estabelecimento pelo schema oficial NF-e / NFC-e
            val emitenteObj = obj.optJSONObject("emitente")
            val rawStoreName = emitenteObj?.optString("razaoSocial")?.takeIf { it.isNotBlank() && it != "null" }
                ?: obj.optString("storeName", "").takeIf { it.isNotBlank() && it != "null" }
                ?: fallbackResult?.storeName
                ?: "Estabelecimento Comercial"
            val storeName = rawStoreName.trim()

            // 2. Valor Total da Nota Fiscal (Guia Oficial: valorTotalNota)
            var totalAmount = obj.optDouble("valorTotalNota", -1.0)
            if (totalAmount <= 0.0) {
                totalAmount = obj.optDouble("valorTotalProdutos", -1.0)
            }
            if (totalAmount <= 0.0) {
                totalAmount = extractTotalAmount(obj, cleanJson)
            }
            if (totalAmount <= 0.0 && fallbackResult != null && fallbackResult.totalAmount > 0.0) {
                totalAmount = fallbackResult.totalAmount
            }
            if (totalAmount < 0.0) totalAmount = 0.0

            // 3. Data de Emissão da Nota
            val rawDate = obj.optString("dataEmissao", "").takeIf { it.isNotBlank() && it != "null" }
                ?: obj.optString("transactionDate", "").takeIf { it.isNotBlank() && it != "null" }
                ?: fallbackResult?.transactionDate
                ?: currentDateString()
            val transactionDate = normalizeDate(rawDate)

            // 4. Categoria
            val category = obj.optString("category", "").takeIf { it.isNotBlank() && it != "null" }
                ?: inferCategoryFromStore(storeName)

            // 5. Forma de Pagamento
            val paymentMethod = obj.optString("paymentMethod", "").takeIf { it.isNotBlank() && it != "null" }
                ?: fallbackResult?.paymentMethod
                ?: "Cartão de Crédito"

            // 6. Itens da Nota Fiscal com descrição, quantidade e valor total
            val itemsList = mutableListOf<String>()
            val itensArr = obj.optJSONArray("itens")
            if (itensArr != null && itensArr.length() > 0) {
                for (i in 0 until itensArr.length()) {
                    val itemObj = itensArr.optJSONObject(i)
                    if (itemObj != null) {
                        val desc = itemObj.optString("descricao", "Item")
                        val qtd = itemObj.optDouble("quantidade", 1.0)
                        val vlTot = itemObj.optDouble("valorTotal", 0.0)
                        val qtdStr = if (qtd == qtd.toLong().toDouble()) "${qtd.toLong()}" else String.format(Locale.US, "%.2f", qtd)
                        itemsList.add("${qtdStr}x $desc - R$ ${String.format(Locale.US, "%.2f", vlTot)}")
                    } else {
                        itemsList.add(itensArr.optString(i))
                    }
                }
            } else {
                val legacyItems = obj.optJSONArray("items")
                if (legacyItems != null && legacyItems.length() > 0) {
                    for (i in 0 until legacyItems.length()) {
                        itemsList.add(legacyItems.optString(i))
                    }
                } else if (fallbackResult != null && fallbackResult.items.isNotEmpty()) {
                    itemsList.addAll(fallbackResult.items)
                }
            }

            // 7. Chave de Acesso e Campos Incertos (Validação do Guia Oficial)
            val chaveAcesso = obj.optString("chaveAcesso", "").takeIf { it.isNotBlank() && it != "null" }
            val camposIncertosArr = obj.optJSONArray("camposIncertos") ?: obj.optJSONArray("campos_incertos")
            val incertosList = mutableListOf<String>()
            if (camposIncertosArr != null) {
                for (i in 0 until camposIncertosArr.length()) {
                    incertosList.add(camposIncertosArr.optString(i))
                }
            }

            val confidence = if (incertosList.isNotEmpty()) {
                "REVISÃO (Campos incertos: ${incertosList.joinToString()})"
            } else if (!chaveAcesso.isNullOrBlank()) {
                "ALTA (Chave NFC-e: ${chaveAcesso.take(8)}...)"
            } else {
                "ALTA (Oficial NF-e / NFC-e)"
            }

            ScannedReceiptResult(
                storeName = storeName,
                totalAmount = totalAmount,
                transactionDate = transactionDate,
                category = category,
                paymentMethod = paymentMethod,
                items = itemsList,
                rawText = cleanJson,
                confidence = confidence,
                familyAnalysis = obj.optString("familyAnalysis", "").takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            fallbackResult ?: ScannedReceiptResult(
                storeName = "Cupom Fiscal",
                totalAmount = 0.0,
                transactionDate = currentDateString(),
                category = "Alimentação",
                paymentMethod = "Cartão de Crédito",
                items = emptyList(),
                rawText = jsonText,
                confidence = "REVISíO NECESSÁRIA"
            )
        }
    }

    private fun extractJsonSubstring(raw: String): String {
        val s = raw.indexOf('{')
        val e = raw.lastIndexOf('}')
        return if (s != -1 && e != -1 && e > s) {
            raw.substring(s, e + 1)
        } else {
            raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        }
    }

    private fun extractTotalAmount(obj: JSONObject, rawFallbackText: String): Double {
        val candidateKeys = listOf("totalAmount", "total_amount", "valorTotal", "valor_total", "total", "amount", "vNF", "valor")
        for (key in candidateKeys) {
            if (obj.has(key)) {
                val raw = obj.opt(key)
                if (raw is Number) {
                    val d = raw.toDouble()
                    if (d > 0.0) return d
                } else if (raw is String) {
                    val parsed = raw.parseBrDouble()
                    if (parsed != null && parsed > 0.0) return parsed
                }
            }
        }
        // Fallback com regex no texto JSON bruto
        val regex = Regex("""(?:totalAmount|valorTotal|total|pagar)[\"']?\s*:\s*[\"']?([0-9\.,]+)""", RegexOption.IGNORE_CASE)
        val match = regex.find(rawFallbackText)
        if (match != null) {
            val parsed = match.groupValues[1].parseBrDouble()
            if (parsed != null && parsed > 0.0) return parsed
        }
        return 0.0
    }

    
    private fun normalizeDate(dateStr: String): String {
        return try {
            val trimmed = dateStr.trim()
            if (trimmed.length >= 10 && trimmed[4] == '-' && trimmed[7] == '-') {
                trimmed.substring(0, 10)
            } else if (trimmed.length >= 10 && trimmed[2] == '/' && trimmed[5] == '/') {
                val d = trimmed.substring(0, 2)
                val m = trimmed.substring(3, 5)
                val y = trimmed.substring(6, 10)
                "$y-$m-$d"
            } else {
                currentDateString()
            }
        } catch (e: Exception) {
            currentDateString()
        }
    }

    private fun inferCategoryFromStore(storeName: String): String {
        val lower = storeName.lowercase(Locale.getDefault())
        return when {
            lower.contains("supermercado") || lower.contains("hipermercado") || lower.contains("mercado") ||
            lower.contains("atacadao") || lower.contains("assai") || lower.contains("carrefour") ||
            lower.contains("pao de acucar") || lower.contains("hortifruti") -> "Mercado"
            lower.contains("restaurante") || lower.contains("lanchonete") || lower.contains("padaria") ||
            lower.contains("bar") || lower.contains("pizzaria") || lower.contains("churrascaria") ||
            lower.contains("burger") || lower.contains("cafe") || lower.contains("doceria") -> "Alimentação"
            lower.contains("farmacia") || lower.contains("drogaria") || lower.contains("droga") ||
            lower.contains("saude") || lower.contains("hospital") || lower.contains("clinica") -> "Saúde"
            lower.contains("posto") || lower.contains("combustivel") || lower.contains("gasolina") ||
            lower.contains("auto") || lower.contains("ipiranga") || lower.contains("shell") -> "Transporte"
            lower.contains("cinema") || lower.contains("teatro") || lower.contains("show") ||
            lower.contains("livraria") || lower.contains("parque") -> "Lazer"
            lower.contains("eletro") || lower.contains("moveis") || lower.contains("casa") ||
            lower.contains("construcao") || lower.contains("leroy") -> "Moradia"
            else -> "Outros"
        }
    }

    private fun currentDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun Bitmap.scalePreservingAspect(maxDim: Int = 2560): Bitmap {
        val maxSide = max(width, height)
        if (maxSide <= maxDim) return this
        val scale = maxDim.toFloat() / maxSide.toFloat()
        val newWidth = (width * scale).toInt().coerceAtLeast(1)
        val newHeight = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(this, newWidth, newHeight, true)
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, 92, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
