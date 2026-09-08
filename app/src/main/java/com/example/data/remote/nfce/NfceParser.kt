package com.example.data.remote.nfce

import com.example.data.gemini.ScannedReceiptResult
import com.example.util.parseBrDouble
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

/**
 * Parser de cupons fiscais eletrônicos brasileiros (NFC-e / SAT / SEFAZ).
 * Suporta tanto extração do HTML retornado pelo portal da SEFAZ quanto
 * decodificação dos parâmetros padronizados da URL do QR Code (chave de 44 dígitos, CNPJ, valor total).
 */
class NfceParser {

    data class NfceMetadata(
        val accessKey: String,
        val cnpj: String,
        val ufCode: String,
        val totalAmount: Double,
        val emissionDate: String,
        val rawUrlOrKey: String
    )

    /**
     * Analisa o conteúdo recebido (HTML e/ou URL do QR Code) e gera o ScannedReceiptResult estruturado.
     */
    fun parse(content: String, originalUrlOrKey: String): ScannedReceiptResult {
        val qrMetadata = extractQrCodeMetadata(originalUrlOrKey.ifBlank { content })

        // Se o conteúdo possuir marcação HTML de portal SEFAZ, faz a raspagem dos campos
        if (content.contains("<html", ignoreCase = true) || content.contains("<table", ignoreCase = true) || content.contains("txtTopo", ignoreCase = true)) {
            val htmlResult = parseSefazHtml(content, qrMetadata)
            if (htmlResult != null && (htmlResult.totalAmount > 0.0 || htmlResult.items.isNotEmpty())) {
                return htmlResult
            }
        }

        // Caso seja apenas a URL ou a consulta HTML tenha retornado vazia, extrai dos parâmetros da NFC-e
        val storeName = when {
            qrMetadata.cnpj.isNotBlank() -> "Estabelecimento CNPJ ${formatCnpj(qrMetadata.cnpj)}"
            else -> "Cupom Fiscal NFC-e"
        }

        val items = if (qrMetadata.totalAmount > 0.0) {
            listOf("1x Compra NFC-e (Chave ${qrMetadata.accessKey.take(18)}...) - ${com.example.util.formatBr(qrMetadata.totalAmount)}")
        } else {
            listOf("1x Compra registrada via NFC-e SEFAZ")
        }

        return ScannedReceiptResult(
            storeName = storeName,
            totalAmount = qrMetadata.totalAmount,
            transactionDate = qrMetadata.emissionDate.ifBlank { currentDateString() },
            category = inferCategory(storeName, items),
            paymentMethod = "NFC-e / Cartão",
            items = items,
            rawText = "NFC-e Chave: ${qrMetadata.accessKey}\nCNPJ: ${qrMetadata.cnpj}\nUF: ${qrMetadata.ufCode}",
            confidence = "OFICIAL (SEFAZ NFC-e)"
        )
    }

    /**
     * Extrai metadados estruturados a partir do parâmetro 'p' da URL do QR Code da NFC-e ou chave direta.
     * Padrão Nacional NFC-e QR Code:
     * p=chNFe|nVersao|tpAmb|cDest|dhEmi|vNF|vICMS|digVal|cIdToken|cHashQRCode
     */
    fun extractQrCodeMetadata(input: String): NfceMetadata {
        val cleaned = input.trim()
        val decodedInput = try {
            if (cleaned.contains("%")) URLDecoder.decode(cleaned, "UTF-8") else cleaned
        } catch (e: Exception) {
            cleaned
        }

        var accessKey = ""
        var totalAmount = 0.0
        var emissionDate = currentDateString()

        // 1. Verificar se é uma chave de 44 dígitos direta
        val keyMatcher = Pattern.compile("(\\d{44})").matcher(decodedInput)
        if (keyMatcher.find()) {
            accessKey = keyMatcher.group(1) ?: ""
        }

        // 2. Extrair parâmetros diretos da URL (ex: &vNF=152.80 ou &valor=152.80 ou &total=152.80)
        val vParamMatcher = Pattern.compile("[?&](?:vNF|valor|total|vTotalNf)=([\\d\\.,]+)", Pattern.CASE_INSENSITIVE).matcher(decodedInput)
        if (vParamMatcher.find()) {
            val raw = vParamMatcher.group(1) ?: ""
            val v = raw.parseBrDouble()
            if (v != null && v > 0.0) {
                totalAmount = v
            }
        }

        // 3. Extrair parâmetro 'p' (padrão nacional NFC-e QR Code)
        val pParam = when {
            decodedInput.contains("p=") -> {
                val sub = decodedInput.substringAfter("p=")
                if (sub.contains("&")) sub.substringBefore("&") else sub
            }
            decodedInput.contains("|") -> decodedInput
            else -> ""
        }

        if (pParam.isNotBlank()) {
            val parts = pParam.split("|")
            if (parts.isNotEmpty()) {
                val candidateKey = parts[0].filter { it.isDigit() }
                if (candidateKey.length == 44) {
                    accessKey = candidateKey
                }
            }

            // No padrão nacional da NFC-e:
            // parts[1] é a versão. Se for "1", parts[5] é o vNF (valor total).
            // Na versão "2", o valor total foi removido da URL por segurança.
            val isVersion1 = parts.size > 1 && parts[1].trim() == "1"
            if (isVersion1 && parts.size > 5) {
                val candidate = parts[5].trim().parseBrDouble()
                if (candidate != null && candidate > 0.0) {
                    totalAmount = candidate
                }
            }
        }

        // Se encontrou a chave de 44 dígitos, extrai CNPJ e UF
        val cnpj = if (accessKey.length == 44) {
            // Chave NFC-e: UF(2) + AAMM(4) + CNPJ(14) + Mod(2) + Serie(3) + NFe(9) + Forma(1) + Cod(8) + DV(1)
            val year = accessKey.substring(2, 4)
            val month = accessKey.substring(4, 6)
            emissionDate = "20$year-$month-01"
            accessKey.substring(6, 20)
        } else {
            ""
        }

        val ufCode = if (accessKey.length >= 2) {
            getUfFromCode(accessKey.substring(0, 2))
        } else {
            ""
        }

        return NfceMetadata(
            accessKey = accessKey,
            cnpj = cnpj,
            ufCode = ufCode,
            totalAmount = totalAmount,
            emissionDate = emissionDate,
            rawUrlOrKey = input
        )
    }

    private fun parseSefazHtml(html: String, fallbackMeta: NfceMetadata): ScannedReceiptResult? {
        try {
            // Extrai Razão Social / Nome Fantasia
            var storeName = ""
            val storePatterns = listOf(
                Pattern.compile("<div[^>]*class=[\"'][^\"']*txtTopo[^\"']*[\"'][^>]*>(.*?)</div>", Pattern.CASE_INSENSITIVE),
                Pattern.compile("<span[^>]*class=[\"'][^\"']*txtTopo[^\"']*[\"'][^>]*>(.*?)</span>", Pattern.CASE_INSENSITIVE),
                Pattern.compile("<div[^>]*class=[\"'][^\"']*text-center[^\"']*[\"'][^>]*>(.*?)</div>", Pattern.CASE_INSENSITIVE),
                Pattern.compile("<h2[^>]*class=[\"'][^\"']*text-center[^\"']*[\"'][^>]*>(.*?)</h2>", Pattern.CASE_INSENSITIVE),
                Pattern.compile("<h4[^>]*class=[\"'][^\"']*text-center[^\"']*[\"'][^>]*>(.*?)</h4>", Pattern.CASE_INSENSITIVE),
                Pattern.compile("<div[^>]*id=[\"']conteudo[\"'][^>]*>\\s*<h4[^>]*>(.*?)</h4>", Pattern.CASE_INSENSITIVE),
                Pattern.compile("<div[^>]*class=[\"'][^\"']*ui-body[^\"']*[\"'][^>]*>\\s*<h3[^>]*>(.*?)</h3>", Pattern.CASE_INSENSITIVE),
                Pattern.compile("<span[^>]*class=[\"'][^\"']*txtRazaoSocial[^\"']*[\"'][^>]*>(.*?)</span>", Pattern.CASE_INSENSITIVE)
            )

            for (pat in storePatterns) {
                val m = pat.matcher(html)
                if (m.find()) {
                    storeName = cleanHtml(m.group(1) ?: "")
                    if (storeName.isNotBlank()) break
                }
            }

            if (storeName.isBlank() && fallbackMeta.cnpj.isNotBlank()) {
                storeName = "Estabelecimento CNPJ ${formatCnpj(fallbackMeta.cnpj)}"
            }

            // Extrai Valor Total da Nota
            var totalAmount = fallbackMeta.totalAmount
            val totalPatterns = listOf(
                Pattern.compile("Valor\\s*a\\s*pagar[^\\d]*R?\\$?\\s*:?\\s*([\\d\\.,]+)", Pattern.CASE_INSENSITIVE),
                Pattern.compile("Valor\\s*Total[^\\d]*R?\\$?\\s*:?\\s*([\\d\\.,]+)", Pattern.CASE_INSENSITIVE),
                Pattern.compile("Total[^\\d]*R?\\$\\s*([\\d\\.,]+)", Pattern.CASE_INSENSITIVE),
                Pattern.compile("vTotalNf[^>]*>([\\d\\.,]+)<", Pattern.CASE_INSENSITIVE),
                Pattern.compile("totalNfeValor[^>]*>([\\d\\.,]+)<", Pattern.CASE_INSENSITIVE),
                Pattern.compile("class=[\"'][^\"']*totalNfe[^\"']*[\"'][^>]*>.*?([\\d]+[\\.,]\\d{2})", Pattern.CASE_INSENSITIVE),
                Pattern.compile("<td[^>]*>\\s*Valor Total\\s*</td>\\s*<td[^>]*>\\s*([\\d\\.,]+)\\s*</td>", Pattern.CASE_INSENSITIVE),
                Pattern.compile("<span[^>]*id=[\"'][^\"']*vNF[^\"']*[\"'][^>]*>([\\d\\.,]+)</span>", Pattern.CASE_INSENSITIVE),
                Pattern.compile("<strong[^>]*>\\s*R?\\$\\s*([\\d\\.,]+)\\s*</strong>", Pattern.CASE_INSENSITIVE)
            )

            for (pat in totalPatterns) {
                val m = pat.matcher(html)
                if (m.find()) {
                    val rawVal = m.group(1) ?: ""
                    val parsed = rawVal.parseBrDouble()
                    if (parsed != null && parsed > 0.0) {
                        totalAmount = parsed
                        break
                    }
                }
            }

            // Extrai Itens do cupom fiscal
            val items = mutableListOf<String>()
            val itemPattern = Pattern.compile(
                "<tr[^>]*id=[\"']Item[ +]?\\d+[\"'][^>]*>(.*?)</tr>",
                Pattern.CASE_INSENSITIVE or Pattern.DOTALL
            )
            val matcher = itemPattern.matcher(html)
            while (matcher.find()) {
                val rowHtml = matcher.group(1) ?: ""
                val desc = extractTagText(rowHtml, "txtTit")
                val qtd = extractTagText(rowHtml, "Rqtd")
                val vUnit = extractTagText(rowHtml, "RvalUnit")
                val vTotal = extractTagText(rowHtml, "valor")

                if (desc.isNotBlank()) {
                    val formatted = "${qtd.ifBlank { "1" }}x $desc - R$ ${vTotal.ifBlank { vUnit }}"
                    items.add(formatted)
                }
            }

            // Extrai Forma de Pagamento
            var paymentMethod = "NFC-e / Cartão"
            if (html.contains("Cartão de Crédito", ignoreCase = true)) paymentMethod = "Cartão de Crédito"
            else if (html.contains("Cartão de Débito", ignoreCase = true)) paymentMethod = "Cartão de Débito"
            else if (html.contains("PIX", ignoreCase = true)) paymentMethod = "PIX"
            else if (html.contains("Dinheiro", ignoreCase = true)) paymentMethod = "Dinheiro"

            val dateStr = fallbackMeta.emissionDate.ifBlank { currentDateString() }

            return ScannedReceiptResult(
                storeName = storeName.ifBlank { "Cupom NFC-e SEFAZ" },
                totalAmount = totalAmount,
                transactionDate = dateStr,
                category = inferCategory(storeName, items),
                paymentMethod = paymentMethod,
                items = items.ifEmpty { listOf("1x Compra NFC-e Registrada - ${com.example.util.formatBr(totalAmount)}") },
                rawText = "NFC-e Chave: ${fallbackMeta.accessKey}\nEstabelecimento: $storeName",
                confidence = "ALTA (SEFAZ Oficial)"
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun inferCategory(storeName: String, items: List<String>): String {
        val combined = (storeName + " " + items.joinToString(" ")).lowercase(Locale.getDefault())
        return when {
            combined.contains("supermercado") || combined.contains("mercado") || combined.contains("atacado") ||
            combined.contains("hortifruti") || combined.contains("alimento") || combined.contains("padaria") -> "Mercado"
            combined.contains("restaurante") || combined.contains("lanchonete") || combined.contains("bar") ||
            combined.contains("pizza") || combined.contains("burguer") || combined.contains("cafe") -> "Alimentação"
            combined.contains("farmacia") || combined.contains("drogaria") || combined.contains("droga") ||
            combined.contains("medic") || combined.contains("saude") -> "Saúde"
            combined.contains("posto") || combined.contains("combustivel") || combined.contains("gasolina") ||
            combined.contains("etanol") || combined.contains("auto") -> "Transporte"
            combined.contains("cinema") || combined.contains("livraria") || combined.contains("parque") -> "Lazer"
            else -> "Outros"
        }
    }

    private fun extractTagText(source: String, className: String): String {
        val p = Pattern.compile("class=[\"'][^\"']*$className[^\"']*[\"'][^>]*>(.*?)<", Pattern.CASE_INSENSITIVE)
        val m = p.matcher(source)
        return if (m.find()) cleanHtml(m.group(1) ?: "") else ""
    }

    private fun cleanHtml(text: String): String {
        return text.replace(Regex("<.*?>"), "").replace("&nbsp;", " ").replace("&amp;", "&").trim()
    }

    private fun formatCnpj(cnpj: String): String {
        val digits = cnpj.filter { it.isDigit() }
        if (digits.length != 14) return cnpj
        return "${digits.substring(0, 2)}.${digits.substring(2, 5)}.${digits.substring(5, 8)}/${digits.substring(8, 12)}-${digits.substring(12, 14)}"
    }

    private fun currentDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getUfFromCode(code: String): String {
        return when (code) {
            "35" -> "SP"
            "31" -> "MG"
            "33" -> "RJ"
            "43" -> "RS"
            "41" -> "PR"
            "42" -> "SC"
            "29" -> "BA"
            "53" -> "DF"
            "52" -> "GO"
            "26" -> "PE"
            "23" -> "CE"
            else -> "BR"
        }
    }
}
