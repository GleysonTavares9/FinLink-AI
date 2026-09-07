package com.example.data.ocr

import android.graphics.Bitmap
import com.example.data.gemini.ScannedReceiptResult
import com.example.util.parseBrDouble
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import kotlin.coroutines.resume

/**
 * Motor de OCR local e offline usando Google ML Kit Text Recognition.
 * Reconhece automaticamente texto de cupons fiscais brasileiros mesmo sem internet,
 * extraindo Estabelecimento, Valor Total, Data, Categoria e Itens.
 */
class ReceiptOcrEngine {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractTextFromBitmap(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    cont.resume(visionText.text)
                }
                .addOnFailureListener {
                    cont.resume("")
                }
        } catch (e: Exception) {
            cont.resume("")
        }
    }

    suspend fun analyzeReceipt(bitmap: Bitmap): ScannedReceiptResult {
        val ocrText = extractTextFromBitmap(bitmap)
        return parseOcrText(ocrText, bitmap.width, bitmap.height)
    }

    fun parseOcrText(rawText: String, width: Int = 0, height: Int = 0): ScannedReceiptResult {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        // 1. Extrair Nome do Estabelecimento (geralmente uma das primeiras linhas não-técnicas)
        var storeName = ""
        val ignoreStoreWords = listOf("danfe", "nfc-e", "sat", "extrato", "documento", "auxiliar", "fiscal", "cnpj", "ie:", "im:", "rua", "av.", "avenida", "cep", "fone", "tel")
        for (line in lines.take(6)) {
            val lower = line.lowercase(Locale.getDefault())
            val hasIgnored = ignoreStoreWords.any { lower.contains(it) }
            val hasLetters = line.any { it.isLetter() }
            if (!hasIgnored && hasLetters && line.length >= 3) {
                storeName = line
                break
            }
        }
        if (storeName.isBlank()) {
            storeName = lines.firstOrNull { it.any { c -> c.isLetter() } && it.length > 3 } ?: "Estabelecimento Comercial"
        }

        // 2. Extrair Valor Total da Nota / Cupom
        var totalAmount = 0.0

        // Padrões de total prioritários em cupons fiscais brasileiros
        val totalRegexes = listOf(
            Regex("""(?:valor\s*a\s*pagar|valor\s*total|total\s*a\s*pagar|total\s*geral|total\s*r\$|vtotal)\s*:?\s*r?\$?\s*([\d\.,]+)""", RegexOption.IGNORE_CASE),
            Regex("""(?:total|pagar|a\s*pagar|líquido|liquido)\s*:?\s*r?\$?\s*([\d\.,]+)""", RegexOption.IGNORE_CASE),
            Regex("""(?:cart[aã]o|cr[eé]dito|d[eé]bito|pix|dinheiro)\s*:?\s*r?\$?\s*([\d\.,]+)""", RegexOption.IGNORE_CASE),
            Regex("""r\$\s*([\d\.,]+)""", RegexOption.IGNORE_CASE)
        )

        for (regex in totalRegexes) {
            val match = regex.find(rawText)
            if (match != null) {
                val candidateStr = match.groupValues[1]
                val candidateVal = candidateStr.parseBrDouble()
                if (candidateVal != null && candidateVal > 0.0) {
                    totalAmount = candidateVal
                    break
                }
            }
        }

        // Se ainda não encontrou total por palavras-chave, procura o maior valor com formato R$ ou decimal
        if (totalAmount <= 0.0) {
            val decimalRegex = Regex("""\b\d{1,5}[,\.]\d{2}\b""")
            val candidates = decimalRegex.findAll(rawText)
                .mapNotNull { it.value.parseBrDouble() }
                .filter { it in 0.50..50000.0 }
                .toList()

            if (candidates.isNotEmpty()) {
                // Em cupons fiscais, o total geralmente é o maior valor presente ou o último valor listado
                totalAmount = candidates.maxOrNull() ?: 0.0
            }
        }

        // 3. Extrair Data da Transação
        var transactionDate = ""
        val dateRegex = Regex("""(\d{2})[/.-](\d{2})[/.-](\d{4})""")
        val dateMatch = dateRegex.find(rawText)
        if (dateMatch != null) {
            val day = dateMatch.groupValues[1]
            val month = dateMatch.groupValues[2]
            val year = dateMatch.groupValues[3]
            transactionDate = "$year-$month-$day"
        }
        if (transactionDate.isBlank()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            transactionDate = sdf.format(Date())
        }

        // 4. Extrair Itens da Compra
        val items = mutableListOf<String>()
        val itemPriceRegex = Regex("""^(.+?)\s+R?\$?\s*(\d+[.,]\d{2})$""")
        for (line in lines) {
            val match = itemPriceRegex.find(line)
            if (match != null) {
                val desc = match.groupValues[1].trim()
                val price = match.groupValues[2].trim()
                if (desc.length > 2 && !desc.contains("total", ignoreCase = true) && !desc.contains("pagar", ignoreCase = true)) {
                    items.add("1x $desc - R$ $price")
                }
            }
        }

        // 5. Detectar Forma de Pagamento
        var paymentMethod = "Cartão de Crédito"
        val lowerRaw = rawText.lowercase(Locale.getDefault())
        when {
            lowerRaw.contains("pix") -> paymentMethod = "PIX"
            lowerRaw.contains("débito") || lowerRaw.contains("debito") -> paymentMethod = "Cartão de Débito"
            lowerRaw.contains("crédito") || lowerRaw.contains("credito") -> paymentMethod = "Cartão de Crédito"
            lowerRaw.contains("dinheiro") -> paymentMethod = "Dinheiro"
            lowerRaw.contains("boleto") -> paymentMethod = "Boleto"
            lowerRaw.contains("nfc-e") || lowerRaw.contains("sat") -> paymentMethod = "NFC-e"
        }

        // 6. Inferir Categoria
        val category = inferCategory(storeName, items)

        return ScannedReceiptResult(
            storeName = storeName,
            totalAmount = totalAmount,
            transactionDate = transactionDate,
            category = category,
            paymentMethod = paymentMethod,
            items = items.ifEmpty {
                if (totalAmount > 0.0) listOf("1x Compra em $storeName - R$ ${String.format(Locale.US, "%.2f", totalAmount)}")
                else listOf("1x Cupom identificado ($width x $height px)")
            },
            rawText = rawText,
            confidence = if (totalAmount > 0.0) "ALTA (OCR Local ML Kit)" else "MÉDIA (OCR Local)"
        )
    }

    private fun inferCategory(storeName: String, items: List<String>): String {
        val combined = (storeName + " " + items.joinToString(" ")).lowercase(Locale.getDefault())
        return when {
            combined.contains("supermercado") || combined.contains("mercado") || combined.contains("atacado") ||
            combined.contains("hortifruti") || combined.contains("alimento") || combined.contains("padaria") ||
            combined.contains("açougue") || combined.contains("acougue") || combined.contains("sacolao") -> "Mercado"
            combined.contains("restaurante") || combined.contains("lanchonete") || combined.contains("bar") ||
            combined.contains("pizza") || combined.contains("burguer") || combined.contains("cafe") ||
            combined.contains("churrascaria") || combined.contains("refeicao") -> "Alimentação"
            combined.contains("farmacia") || combined.contains("drogaria") || combined.contains("droga") ||
            combined.contains("medic") || combined.contains("saude") || combined.contains("hospital") -> "Saúde"
            combined.contains("posto") || combined.contains("combustivel") || combined.contains("gasolina") ||
            combined.contains("etanol") || combined.contains("auto") || combined.contains("uber") ||
            combined.contains("estacionamento") -> "Transporte"
            combined.contains("cinema") || combined.contains("livraria") || combined.contains("parque") ||
            combined.contains("show") || combined.contains("teatro") -> "Lazer"
            combined.contains("condominio") || combined.contains("aluguel") || combined.contains("luz") ||
            combined.contains("agua") || combined.contains("internet") -> "Moradia"
            else -> "Outros"
        }
    }
}
