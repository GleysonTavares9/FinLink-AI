package com.example.data.openfinance

import com.example.data.db.ExpenseDao
import com.example.data.db.ExpenseEntity
import com.example.data.db.OpenFinanceAccountEntity
import com.example.data.db.OpenFinanceDao
import com.example.data.db.OpenFinanceTransactionEntity
import com.example.util.parseBrDouble
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class OpenFinanceBankInfo(
    val id: String,
    val name: String,
    val primaryColor: Long,
    val code: String
)

class OpenFinanceRepository(
    private val openFinanceDao: OpenFinanceDao,
    private val expenseDao: ExpenseDao
) {

    val availableBanks = listOf(
        OpenFinanceBankInfo("nubank", "Nu Pagamentos (Nubank)", 0xFF8A05BE, "260"),
        OpenFinanceBankInfo("itau", "Banco Itaú Unibanco", 0xFFEC7000, "341"),
        OpenFinanceBankInfo("bradesco", "Banco Bradesco", 0xFFCC092F, "237"),
        OpenFinanceBankInfo("bb", "Banco do Brasil", 0xFF0038A8, "001"),
        OpenFinanceBankInfo("santander", "Banco Santander", 0xFFEC0000, "033"),
        OpenFinanceBankInfo("inter", "Banco Inter", 0xFFFF7A00, "077"),
        OpenFinanceBankInfo("c6", "C6 Bank", 0xFF242424, "336"),
        OpenFinanceBankInfo("caixa", "Caixa Econômica Federal", 0xFF0066B3, "104"),
        OpenFinanceBankInfo("carteira", "Carteira / Dinheiro Físico", 0xFF10B981, "000")
    )

    fun getConnectedAccounts(): Flow<List<OpenFinanceAccountEntity>> = openFinanceDao.getAllAccounts()

    fun getTransactions(): Flow<List<OpenFinanceTransactionEntity>> = openFinanceDao.getAllTransactions()

    suspend fun createAccount(
        bankName: String,
        accountType: String,
        accountNumber: String,
        initialBalance: Double,
        autoSyncTransactions: Boolean = true
    ): OpenFinanceAccountEntity {
        val accountId = "acc_${System.currentTimeMillis()}"
        val cleanAccountNumber = accountNumber.ifBlank { "Ag 0001 • C/C 87642-1" }

        val newAccount = OpenFinanceAccountEntity(
            id = accountId,
            bankName = bankName,
            accountType = accountType,
            accountNumber = cleanAccountNumber,
            balance = initialBalance,
            status = "CONECTADO",
            lastSync = System.currentTimeMillis()
        )
        openFinanceDao.insertAccount(newAccount)

        if (autoSyncTransactions) {
            syncAccountTransactions(accountId, initialSync = true)
        }

        return newAccount
    }

    suspend fun connectPredefinedBank(
        bankInfo: OpenFinanceBankInfo,
        accountType: String = if (bankInfo.id == "nubank") "Cartão de Crédito / NuConta" else "Conta Corrente",
        initialBalance: Double = 1500.0,
        accountNumber: String = "Ag 0001 • C/C ${((10000..99999).random())}-${(1..9).random()}",
        autoSync: Boolean = true
    ): OpenFinanceAccountEntity {
        return createAccount(
            bankName = bankInfo.name,
            accountType = accountType,
            accountNumber = accountNumber,
            initialBalance = initialBalance,
            autoSyncTransactions = autoSync
        )
    }

    suspend fun syncAccountTransactions(accountId: String, initialSync: Boolean = false): Int {
        val account = openFinanceDao.getAccountById(accountId) ?: return 0
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.forLanguageTag("pt-BR"))
        val today = sdf.format(Date())

        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
        val yesterday = sdf.format(calendar.time)
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -2)
        val twoDaysAgo = sdf.format(calendar.time)

        // Modelos de transações bancárias autênticas do Brasil (valores em Real Brasileiro)
        val candidateExpenses = if (initialSync) {
            listOf(
                ExpenseEntity(
                    storeName = "Supermercado Pão de Açúcar",
                    totalAmount = 142.80,
                    transactionDate = yesterday,
                    category = "Alimentação",
                    paymentMethod = if (account.accountType.contains("Crédito")) "Cartão de Crédito" else "PIX",
                    isManual = false,
                    source = account.bankName,
                    notes = "Sincronizado via Open Finance Brasil (${account.bankName})"
                ),
                ExpenseEntity(
                    storeName = "Posto Shell Combustível",
                    totalAmount = 85.00,
                    transactionDate = today,
                    category = "Transporte",
                    paymentMethod = if (account.accountType.contains("Crédito")) "Cartão de Crédito" else "Cartão de Débito",
                    isManual = false,
                    source = account.bankName,
                    notes = "Sincronizado via Open Finance Brasil (${account.bankName})"
                ),
                ExpenseEntity(
                    storeName = "Drogaria São Paulo",
                    totalAmount = 43.50,
                    transactionDate = twoDaysAgo,
                    category = "Saúde",
                    paymentMethod = "PIX",
                    isManual = false,
                    source = account.bankName,
                    notes = "Sincronizado via Open Finance Brasil (${account.bankName})"
                )
            )
        } else {
            // Nova transação em sincronização manual periódica
            listOf(
                ExpenseEntity(
                    storeName = "Padaria e Restaurante",
                    totalAmount = 32.90,
                    transactionDate = today,
                    category = "Alimentação",
                    paymentMethod = "PIX",
                    isManual = false,
                    source = account.bankName,
                    notes = "Sincronização em Tempo Real (${account.bankName})"
                )
            )
        }

        var importedCount = 0
        var totalDeducted = 0.0
        for (expense in candidateExpenses) {
            expenseDao.insertExpense(expense)
            totalDeducted += expense.totalAmount
            importedCount++
        }

        // Atualiza o saldo e o horário da sincronização da conta
        val updatedBalance = (account.balance - totalDeducted).coerceAtLeast(0.0)
        openFinanceDao.updateAccount(
            account.copy(
                balance = if (account.balance > 0) updatedBalance else account.balance,
                lastSync = System.currentTimeMillis(),
                status = "CONECTADO"
            )
        )

        return importedCount
    }

    suspend fun disconnectBank(accountId: String) {
        openFinanceDao.deleteAccountById(accountId)
    }

    suspend fun importStatementText(rawText: String, targetAccountId: String? = null): Int {
        val lines = rawText.split("\n")
        var count = 0
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.forLanguageTag("pt-BR")).format(Date())

        val targetAccount = targetAccountId?.let { openFinanceDao.getAccountById(it) }
        val bankSourceName = targetAccount?.bankName ?: "Extrato Bancário"
        var totalAmountImported = 0.0

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue

            val parsedExpense = parseSingleStatementLine(trimmed, currentDate, bankSourceName)
            if (parsedExpense != null) {
                expenseDao.insertExpense(parsedExpense)
                totalAmountImported += parsedExpense.totalAmount
                count++
            }
        }

        if (targetAccount != null && totalAmountImported > 0) {
            val newBalance = (targetAccount.balance - totalAmountImported).coerceAtLeast(0.0)
            openFinanceDao.updateAccount(targetAccount.copy(balance = newBalance, lastSync = System.currentTimeMillis()))
        }

        return count
    }

    private fun parseSingleStatementLine(line: String, defaultDate: String, bankSourceName: String): ExpenseEntity? {
        // Padrão de extração para moeda brasileira (ex: R$ 150,00, R$ 1.250,50, 45,90, 45.90)
        val amountPattern = Pattern.compile("(?:R\\$|BRL)?\\s*([0-9]+(?:\\.[0-9]{3})*,[0-9]{2}|[0-9]+[.,][0-9]{2})", Pattern.CASE_INSENSITIVE)
        val matcher = amountPattern.matcher(line)

        var amount = 0.0
        var amountMatched = ""
        if (matcher.find()) {
            amountMatched = matcher.group(0) ?: ""
            val rawNum = matcher.group(1) ?: ""
            amount = rawNum.parseBrDouble() ?: 0.0
        }

        if (amount <= 0.0) return null

        // Extração de Data (ex: 14/08/2026, 14/08 ou 2026-08-14)
        val datePattern = Pattern.compile("([0-9]{1,2}/[0-9]{1,2}(?:/[0-9]{2,4})?)|([0-9]{4}-[0-9]{2}-[0-9]{2})")
        val dateMatcher = datePattern.matcher(line)
        var parsedDate = defaultDate
        if (dateMatcher.find()) {
            val d = dateMatcher.group(0) ?: ""
            if (d.contains("/")) {
                val parts = d.split("/")
                val day = parts.getOrNull(0)?.padStart(2, '0') ?: "01"
                val month = parts.getOrNull(1)?.padStart(2, '0') ?: "01"
                val year = if (parts.size >= 3) {
                    if (parts[2].length == 2) "20" + parts[2] else parts[2]
                } else {
                    SimpleDateFormat("yyyy", Locale.forLanguageTag("pt-BR")).format(Date())
                }
                parsedDate = "$year-$month-$day"
            } else if (d.contains("-")) {
                parsedDate = d
            }
        }

        // Limpeza do nome do estabelecimento
        var merchant = line
        if (amountMatched.isNotBlank()) {
            merchant = merchant.replace(amountMatched, "")
        }
        merchant = merchant.replace(Pattern.compile("([0-9]{1,2}/[0-9]{1,2}(?:/[0-9]{2,4})?)|([0-9]{4}-[0-9]{2}-[0-9]{2})").toRegex(), "")
        merchant = merchant.replace(Regex("(?i)(PIX Enviado|PIX Recebido|Compra no Débito|Compra no Crédito|TED|DOC|Pagamento|Cartão|Boleto)"), "")
        merchant = merchant.replace(Regex("[-–—:;,]"), " ").trim()

        if (merchant.isBlank()) {
            merchant = "Lançamento de Extrato"
        }

        // Detecção de Categoria
        val lowerLine = line.lowercase(Locale.forLanguageTag("pt-BR"))
        val category = when {
            lowerLine.contains("mercado") || lowerLine.contains("supermercado") || lowerLine.contains("pão de açúcar") ||
            lowerLine.contains("carrefour") || lowerLine.contains("padaria") || lowerLine.contains("restaurante") ||
            lowerLine.contains("lanche") || lowerLine.contains("ifood") || lowerLine.contains("café") -> "Alimentação"

            lowerLine.contains("uber") || lowerLine.contains("99") || lowerLine.contains("posto") ||
            lowerLine.contains("gasolina") || lowerLine.contains("combustivel") || lowerLine.contains("estacionamento") ||
            lowerLine.contains("pedagio") -> "Transporte"

            lowerLine.contains("farmacia") || lowerLine.contains("drogaria") || lowerLine.contains("raia") ||
            lowerLine.contains("drogasil") || lowerLine.contains("medico") || lowerLine.contains("consulta") ||
            lowerLine.contains("hospital") || lowerLine.contains("dentista") -> "Saúde"

            lowerLine.contains("cinema") || lowerLine.contains("netflix") || lowerLine.contains("spotify") ||
            lowerLine.contains("jogos") || lowerLine.contains("show") || lowerLine.contains("bar") ||
            lowerLine.contains("viagem") -> "Lazer"

            lowerLine.contains("aluguel") || lowerLine.contains("condominio") || lowerLine.contains("luz") ||
            lowerLine.contains("energia") || lowerLine.contains("agua") || lowerLine.contains("internet") -> "Moradia"

            else -> "Outros"
        }

        val paymentMethod = when {
            lowerLine.contains("pix") -> "PIX"
            lowerLine.contains("débito") || lowerLine.contains("debito") -> "Cartão de Débito"
            lowerLine.contains("crédito") || lowerLine.contains("credito") -> "Cartão de Crédito"
            lowerLine.contains("boleto") -> "Boleto"
            lowerLine.contains("dinheiro") -> "Dinheiro"
            else -> "PIX"
        }

        return ExpenseEntity(
            storeName = merchant.take(40),
            totalAmount = amount,
            transactionDate = parsedDate,
            category = category,
            paymentMethod = paymentMethod,
            isManual = false,
            source = bankSourceName,
            notes = "Importado via Extrato Open Finance"
        )
    }

    fun getApiSpecJson(): String {
        return """
            {
              "open_finance_brasil": {
                "version": "v3.0.1",
                "authority": "Banco Central do Brasil (BACEN)",
                "directory": "https://directory.openfinancebrasil.org.br",
                "supported_apis": [
                  {
                    "api": "Consents API",
                    "version": "v3.0",
                    "endpoints": ["POST /open-banking/consents/v3/consents"]
                  },
                  {
                    "api": "Accounts API",
                    "version": "v2.0",
                    "endpoints": ["GET /open-banking/accounts/v2/accounts/{accountId}/balances"]
                  },
                  {
                    "api": "Credit Cards Accounts API",
                    "version": "v2.0",
                    "endpoints": ["GET /open-banking/credit-cards-accounts/v2/accounts/{accountId}/bills"]
                  },
                  {
                    "api": "Automatic Payments (Pix Automático / ITP)",
                    "version": "v4.0",
                    "endpoints": ["POST /open-banking/payments/v4/pix/payments"]
                  }
                ],
                "security": {
                  "tls_version": "TLS 1.3",
                  "mtls_client_cert": "ICP-Brasil / Open Finance Directory Signed",
                  "fapi_profile": "FAPI 1.0 Advanced / FAPI 2.0 Security Profile",
                  "jws_algorithm": "PS256"
                }
              }
            }
        """.trimIndent()
    }
}
