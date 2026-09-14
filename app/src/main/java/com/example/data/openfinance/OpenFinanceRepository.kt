package com.example.data.openfinance

import android.util.Log
import com.example.data.db.ExpenseDao
import com.example.data.db.ExpenseEntity
import com.example.data.db.OpenFinanceAccountEntity
import com.example.data.db.OpenFinanceBillEntity
import com.example.data.db.OpenFinanceDao
import com.example.data.db.OpenFinanceTransactionEntity
import com.example.data.remote.supabase.SupabaseClient
import com.example.util.parseBrDouble
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

data class OpenFinanceBankInfo(
    val id: String,
    val name: String,
    val primaryColor: Long,
    val code: String
)

class OpenFinanceRepository(
    private val openFinanceDao: OpenFinanceDao,
    private val expenseDao: ExpenseDao,
    private val supabaseClient: SupabaseClient? = null
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val availableBanks = listOf(
        OpenFinanceBankInfo("mercadopago", "Mercado Pago", 0xFF009EE3, "323"),
        OpenFinanceBankInfo("nubank", "Nu Pagamentos (Nubank)", 0xFF8A05BE, "260"),
        OpenFinanceBankInfo("itau", "Banco Itaú Unibanco", 0xFFEC7000, "341"),
        OpenFinanceBankInfo("bradesco", "Banco Bradesco", 0xFFCC092F, "237"),
        OpenFinanceBankInfo("bb", "Banco do Brasil", 0xFF0038A8, "001"),
        OpenFinanceBankInfo("santander", "Banco Santander", 0xFFEC0000, "033"),
        OpenFinanceBankInfo("inter", "Banco Inter", 0xFFFF7A00, "077"),
        OpenFinanceBankInfo("c6", "C6 Bank", 0xFF242424, "336"),
        OpenFinanceBankInfo("caixa", "Caixa Econômica Federal", 0xFF0066B3, "104")
    )

    fun getConnectedAccounts(): Flow<List<OpenFinanceAccountEntity>> = openFinanceDao.getAllAccounts()

    fun getTransactions(): Flow<List<OpenFinanceTransactionEntity>> = openFinanceDao.getAllTransactions()

    fun getAllBills(): Flow<List<OpenFinanceBillEntity>> = openFinanceDao.getAllBills()

    fun getBillsForAccount(accountId: String): Flow<List<OpenFinanceBillEntity>> = openFinanceDao.getBillsForAccount(accountId)

    fun getPendingTransactions(): Flow<List<OpenFinanceTransactionEntity>> = openFinanceDao.getPendingTransactions()

    fun getFutureInstallments(): Flow<List<OpenFinanceTransactionEntity>> = openFinanceDao.getFutureInstallments()

    /**
     * Garante que os dados de conta corrente, cartão de crédito, faturas e parcelas futuras
     * estejam inicializados e prontos para consulta completa.
     */
    suspend fun ensureInitialSeed() = withContext(Dispatchers.IO) {
        val existingCard = openFinanceDao.getAccountById("mp_card_001")
        if (existingCard == null) {
            // 1. Cartão de Crédito Mercado Pago com Fatura Aberta e Limites
            val card = OpenFinanceAccountEntity(
                id = "mp_card_001",
                bankName = "Mercado Pago",
                accountType = "Cartão de Crédito",
                accountNumber = "Final 4829",
                balance = 684.50, // Valor da fatura em aberto
                creditLimit = 3500.00,
                availableCredit = 2815.50,
                closeDate = "2026-09-28",
                dueDate = "2026-10-07",
                minimumPayment = 102.68,
                brand = "MASTERCARD",
                level = "PLATINUM",
                connectedAt = System.currentTimeMillis() - 86400000L * 30,
                status = "CONECTADO",
                lastSync = System.currentTimeMillis()
            )
            openFinanceDao.insertAccount(card)

            // 2. Conta Corrente Mercado Pago
            val checking = OpenFinanceAccountEntity(
                id = "mp_checking_001",
                bankName = "Mercado Pago",
                accountType = "Conta Corrente",
                accountNumber = "Ag 0001 • CC 482910-3",
                balance = 1854.20,
                connectedAt = System.currentTimeMillis() - 86400000L * 30,
                status = "CONECTADO",
                lastSync = System.currentTimeMillis()
            )
            openFinanceDao.insertAccount(checking)

            // 3. Faturas Fechadas / Históricas (Bills)
            val seedBills = listOf(
                OpenFinanceBillEntity(
                    id = "bill_mp_ago_2026",
                    accountId = "mp_card_001",
                    dueDate = "2026-09-07",
                    closingDate = "2026-08-28",
                    totalAmount = 512.90,
                    minimumPaymentAmount = 76.93,
                    allowsInstallments = true,
                    isPaid = true
                ),
                OpenFinanceBillEntity(
                    id = "bill_mp_jul_2026",
                    accountId = "mp_card_001",
                    dueDate = "2026-08-07",
                    closingDate = "2026-07-28",
                    totalAmount = 489.30,
                    minimumPaymentAmount = 73.40,
                    allowsInstallments = true,
                    isPaid = true
                )
            )
            openFinanceDao.insertBills(seedBills)

            // 4. Lançamentos na Fatura Aberta (PENDING) e Parcelas Futuras (billForecastDate)
            val seedTxs = listOf(
                OpenFinanceTransactionEntity(
                    id = "tx_card_1",
                    accountId = "mp_card_001",
                    description = "Supermercado Pão de Açúcar",
                    amount = 248.30,
                    type = "DEBIT",
                    status = "PENDING",
                    date = "2026-09-06",
                    category = "Mercado",
                    billForecastDate = "2026-10",
                    operationType = "CARTAO"
                ),
                OpenFinanceTransactionEntity(
                    id = "tx_card_2",
                    accountId = "mp_card_001",
                    description = "Amazon Marketplace (Parcela 2 de 4)",
                    amount = 89.90,
                    type = "DEBIT",
                    status = "PENDING",
                    date = "2026-09-04",
                    category = "Compras",
                    billForecastDate = "2026-10",
                    installmentNumber = 2,
                    totalInstallments = 4,
                    totalInstallmentAmount = 359.60,
                    operationType = "CARTAO"
                ),
                OpenFinanceTransactionEntity(
                    id = "tx_card_3",
                    accountId = "mp_card_001",
                    description = "Magazine Luiza (Parcela 1 de 10)",
                    amount = 129.90,
                    type = "DEBIT",
                    status = "PENDING",
                    date = "2026-09-01",
                    category = "Eletrônicos",
                    billForecastDate = "2026-10",
                    installmentNumber = 1,
                    totalInstallments = 10,
                    totalInstallmentAmount = 1299.00,
                    operationType = "CARTAO"
                ),
                // Parcela futura: Novembro 2026
                OpenFinanceTransactionEntity(
                    id = "tx_card_fut_1",
                    accountId = "mp_card_001",
                    description = "Magazine Luiza (Parcela 2 de 10)",
                    amount = 129.90,
                    type = "DEBIT",
                    status = "PENDING",
                    date = "2026-09-01",
                    category = "Eletrônicos",
                    billForecastDate = "2026-11",
                    installmentNumber = 2,
                    totalInstallments = 10,
                    totalInstallmentAmount = 1299.00,
                    operationType = "CARTAO"
                ),
                OpenFinanceTransactionEntity(
                    id = "tx_card_fut_2",
                    accountId = "mp_card_001",
                    description = "Amazon Marketplace (Parcela 3 de 4)",
                    amount = 89.90,
                    type = "DEBIT",
                    status = "PENDING",
                    date = "2026-09-04",
                    category = "Compras",
                    billForecastDate = "2026-11",
                    installmentNumber = 3,
                    totalInstallments = 4,
                    totalInstallmentAmount = 359.60,
                    operationType = "CARTAO"
                ),
                // Parcela futura: Dezembro 2026
                OpenFinanceTransactionEntity(
                    id = "tx_card_fut_3",
                    accountId = "mp_card_001",
                    description = "Magazine Luiza (Parcela 3 de 10)",
                    amount = 129.90,
                    type = "DEBIT",
                    status = "PENDING",
                    date = "2026-09-01",
                    category = "Eletrônicos",
                    billForecastDate = "2026-12",
                    installmentNumber = 3,
                    totalInstallments = 10,
                    totalInstallmentAmount = 1299.00,
                    operationType = "CARTAO"
                )
            )
            openFinanceDao.insertTransactions(seedTxs)
            Log.d("OpenFinance", "Seed completo de Cartão de Crédito, Faturas e Parcelas Futuras inicializado.")
        }
        // Sincroniza contas reais existentes na nuvem
        try {
            syncFromPluggy("sync_all")
        } catch (_: Exception) {}
    }

    suspend fun fetchPluggyConnectToken(): String? = withContext(Dispatchers.IO) {
        supabaseClient?.let { sb ->
            try {
                val tokenUrl = "${sb.baseUrl}/functions/v1/pluggy-connect?action=token"
                val req = Request.Builder()
                    .url(tokenUrl)
                    .addHeader("apikey", sb.anonKey)
                    .addHeader("Authorization", "Bearer ${sb.anonKey}")
                    .post("{}".toRequestBody("application/json".toMediaType()))
                    .build()

                val resp = httpClient.newCall(req).execute()
                if (resp.isSuccessful) {
                    val bodyStr = resp.body?.string() ?: ""
                    val json = JSONObject(bodyStr)
                    val token = json.optString("accessToken")
                    if (token.isNotBlank()) {
                        Log.d("OpenFinance", "Connect Token gerado com sucesso!")
                        return@withContext token
                    }
                } else {
                    Log.e("OpenFinance", "Erro ao obter token: ${resp.code} - ${resp.body?.string()}")
                }
            } catch (e: Exception) {
                Log.e("OpenFinance", "Erro ao obter token via Supabase: ${e.message}")
            }
        }
        null
    }

    /**
     * Sincroniza dados reais da Pluggy (Contas, Saldos e Extrato de Transações)
     * e salva no SQLite local (Room) e no Supabase.
     */
    suspend fun syncFromPluggy(itemId: String): Int = withContext(Dispatchers.IO) {
        try {
            openFinanceDao.cleanLegacyMockAccounts()
        } catch (_: Exception) {}

        var syncedCount = 0

        var accountsJson: JSONArray? = null
        var billsJson: JSONArray? = null
        var transactionsArray: JSONArray? = null
        var bankName = "Banco Conectado"

        // 1. Chamar a Supabase Edge Function pluggy-connect com action=sync
        supabaseClient?.let { sb ->
            try {
                val syncUrl = "${sb.baseUrl}/functions/v1/pluggy-connect?action=sync"
                val payload = JSONObject().apply {
                    put("action", "sync")
                    put("itemId", itemId)
                }

                val req = Request.Builder()
                    .url(syncUrl)
                    .addHeader("apikey", sb.anonKey)
                    .addHeader("Authorization", "Bearer ${sb.anonKey}")
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val resp = httpClient.newCall(req).execute()
                if (resp.isSuccessful) {
                    val bodyStr = resp.body?.string() ?: ""
                    val json = JSONObject(bodyStr)
                    val itemObj = json.optJSONObject("item")
                    bankName = itemObj?.optJSONObject("connector")?.optString("name") ?: "Open Finance"
                    accountsJson = json.optJSONArray("accounts")
                    billsJson = json.optJSONArray("bills")
                    transactionsArray = json.optJSONArray("transactions")
                    Log.d("OpenFinance", "Sync Pluggy finalizado: Contas: ${accountsJson?.length()}, Faturas: ${billsJson?.length()}, Transações: ${transactionsArray?.length()}")
                } else {
                    Log.e("OpenFinance", "Erro HTTP no sync: ${resp.code} - ${resp.body?.string()}")
                }
            } catch (e: Exception) {
                Log.e("OpenFinance", "Exceção ao sincronizar via Supabase Edge: ${e.message}")
            }
        }

        // 2. Fallback: Se não retornou do Edge Function, buscar do Supabase REST
        if (supabaseClient != null) {
            if (accountsJson == null || accountsJson.length() == 0) {
                try {
                    val restUrl = "${supabaseClient.baseUrl}/rest/v1/open_finance_accounts?select=*"
                    val req = Request.Builder()
                        .url(restUrl)
                        .addHeader("apikey", supabaseClient.anonKey)
                        .addHeader("Authorization", "Bearer ${supabaseClient.anonKey}")
                        .get()
                        .build()
                    val resp = httpClient.newCall(req).execute()
                    if (resp.isSuccessful) {
                        val raw = resp.body?.string() ?: "[]"
                        val parsed = JSONArray(raw)
                        if (parsed.length() > 0) accountsJson = parsed
                    }
                } catch (e: Exception) {
                    Log.e("OpenFinance", "Erro ao buscar contas do Supabase REST: ${e.message}")
                }
            }

            if (billsJson == null || billsJson.length() == 0) {
                try {
                    val billsRestUrl = "${supabaseClient.baseUrl}/rest/v1/open_finance_bills?select=*&order=due_date.desc"
                    val req = Request.Builder()
                        .url(billsRestUrl)
                        .addHeader("apikey", supabaseClient.anonKey)
                        .addHeader("Authorization", "Bearer ${supabaseClient.anonKey}")
                        .get()
                        .build()
                    val resp = httpClient.newCall(req).execute()
                    if (resp.isSuccessful) {
                        val raw = resp.body?.string() ?: "[]"
                        val parsed = JSONArray(raw)
                        if (parsed.length() > 0) billsJson = parsed
                    }
                } catch (e: Exception) {
                    Log.e("OpenFinance", "Erro ao buscar faturas do Supabase REST: ${e.message}")
                }
            }

            if (transactionsArray == null || transactionsArray.length() == 0) {
                try {
                    val txRestUrl = "${supabaseClient.baseUrl}/rest/v1/open_finance_transactions?select=*&order=transaction_date.desc&limit=500"
                    val req = Request.Builder()
                        .url(txRestUrl)
                        .addHeader("apikey", supabaseClient.anonKey)
                        .addHeader("Authorization", "Bearer ${supabaseClient.anonKey}")
                        .get()
                        .build()
                    val resp = httpClient.newCall(req).execute()
                    if (resp.isSuccessful) {
                        val raw = resp.body?.string() ?: "[]"
                        val parsed = JSONArray(raw)
                        if (parsed.length() > 0) {
                            transactionsArray = parsed
                            Log.d("OpenFinance", "Carregadas ${parsed.length()} transações do Supabase REST")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("OpenFinance", "Erro ao buscar transações do Supabase REST: ${e.message}")
                }
            }
        }

        // 3. Gravar contas reais com metadados de cartão no Room Local
        if (accountsJson != null && accountsJson.length() > 0) {
            for (i in 0 until accountsJson.length()) {
                val acc = accountsJson.getJSONObject(i)
                val accId = acc.optString("id", itemId)
                val type = acc.optString("account_type", acc.optString("subtype", acc.optString("type", "Conta Corrente")))
                val balance = acc.optDouble("current_balance", acc.optDouble("balance", 0.0))
                val number = acc.optString("account_number", acc.optString("number", "0000"))
                val name = acc.optString("bank_name", acc.optString("name", bankName))
                val imageUrl = if (acc.has("connector_image_url") && !acc.isNull("connector_image_url")) acc.optString("connector_image_url") else null
                val creditLimit = if (acc.has("credit_limit") && !acc.isNull("credit_limit")) acc.optDouble("credit_limit") else null
                val availableCredit = if (acc.has("available_credit_limit") && !acc.isNull("available_credit_limit")) acc.optDouble("available_credit_limit") else null
                val closeDate = if (acc.has("balance_close_date") && !acc.isNull("balance_close_date")) acc.optString("balance_close_date") else null
                val dueDate = if (acc.has("balance_due_date") && !acc.isNull("balance_due_date")) acc.optString("balance_due_date") else null
                val minPayment = if (acc.has("minimum_payment") && !acc.isNull("minimum_payment")) acc.optDouble("minimum_payment") else null
                val brand = if (acc.has("credit_card_brand") && !acc.isNull("credit_card_brand")) acc.optString("credit_card_brand") else null
                val level = if (acc.has("credit_card_level") && !acc.isNull("credit_card_level")) acc.optString("credit_card_level") else null

                val entity = OpenFinanceAccountEntity(
                    id = accId,
                    bankName = name,
                    accountType = type.replace("_", " "),
                    accountNumber = if (number.startsWith("Nº") || number.startsWith("NÂº")) number else "Nº $number",
                    balance = balance,
                    imageUrl = imageUrl,
                    creditLimit = creditLimit,
                    availableCredit = availableCredit,
                    closeDate = closeDate,
                    dueDate = dueDate,
                    minimumPayment = minPayment,
                    brand = brand,
                    level = level,
                    connectedAt = System.currentTimeMillis(),
                    status = "CONECTADO",
                    lastSync = System.currentTimeMillis()
                )
                openFinanceDao.insertAccount(entity)
                syncedCount++
            }
        }

        // 4. Gravar faturas fechadas/vencidas (Bills) no Room Local
        if (billsJson != null && billsJson.length() > 0) {
            val billEntities = mutableListOf<OpenFinanceBillEntity>()
            for (k in 0 until billsJson.length()) {
                val bill = billsJson.getJSONObject(k)
                billEntities.add(
                    OpenFinanceBillEntity(
                        id = bill.optString("id", UUID.randomUUID().toString()),
                        accountId = bill.optString("account_id"),
                        dueDate = bill.optString("due_date", ""),
                        closingDate = if (bill.has("bill_closing_date") && !bill.isNull("bill_closing_date")) bill.optString("bill_closing_date") else null,
                        totalAmount = bill.optDouble("total_amount", 0.0),
                        minimumPaymentAmount = bill.optDouble("minimum_payment_amount", 0.0),
                        allowsInstallments = bill.optBoolean("allows_installments", true),
                        isPaid = bill.optBoolean("is_paid", false),
                        financeChargesJson = if (bill.has("finance_charges") && !bill.isNull("finance_charges")) bill.opt("finance_charges")?.toString() else null,
                        paymentsJson = if (bill.has("payments") && !bill.isNull("payments")) bill.opt("payments")?.toString() else null
                    )
                )
            }
            if (billEntities.isNotEmpty()) {
                openFinanceDao.insertBills(billEntities)
                Log.d("OpenFinance", "${billEntities.size} faturas gravadas no SQLite local")
            }
        }

        // 5. Gravar transações reais com status, parcelas e projeção de fatura
        if (transactionsArray != null && transactionsArray.length() > 0) {
            val txEntities = mutableListOf<OpenFinanceTransactionEntity>()
            for (j in 0 until transactionsArray.length()) {
                val tx = transactionsArray.getJSONObject(j)
                val txId = tx.optString("id", UUID.randomUUID().toString())
                val accId = tx.optString("account_id", itemId)
                val desc = tx.optString("description", "Transação Bancária")
                val rawAmount = tx.optDouble("amount", 0.0)
                val pType = tx.optString("payment_type", tx.optString("type", "DEBIT"))
                val isCredit = pType.equals("CREDIT", ignoreCase = true)
                val absAmount = Math.abs(rawAmount)
                val status = tx.optString("status", "POSTED")
                val date = tx.optString("transaction_date", tx.optString("date", "")).take(10).ifBlank {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                }
                val category = tx.optString("category", "Outros")
                val billId = if (tx.has("bill_id") && !tx.isNull("bill_id")) tx.optString("bill_id") else null
                val billForecastDate = if (tx.has("bill_forecast_date") && !tx.isNull("bill_forecast_date")) tx.optString("bill_forecast_date") else null
                val instNum = if (tx.has("installment_number") && !tx.isNull("installment_number")) tx.optInt("installment_number") else null
                val totalInst = if (tx.has("total_installments") && !tx.isNull("total_installments")) tx.optInt("total_installments") else null
                val totalInstAmt = if (tx.has("total_installment_amount") && !tx.isNull("total_installment_amount")) tx.optDouble("total_installment_amount") else null
                val operationType = if (tx.has("operation_type") && !tx.isNull("operation_type")) tx.optString("operation_type") else null

                txEntities.add(
                    OpenFinanceTransactionEntity(
                        id = txId,
                        accountId = accId,
                        description = desc,
                        amount = absAmount,
                        type = if (isCredit) "CREDIT" else "DEBIT",
                        status = status,
                        date = date,
                        category = category,
                        billId = billId,
                        billForecastDate = billForecastDate,
                        installmentNumber = instNum,
                        totalInstallments = totalInst,
                        totalInstallmentAmount = totalInstAmt,
                        operationType = operationType,
                        isSyncedToExpenses = true
                    )
                )
            }
            if (txEntities.isNotEmpty()) {
                openFinanceDao.insertTransactions(txEntities)
            }
        }

        syncedCount
    }

    suspend fun connectPredefinedBank(
        bank: OpenFinanceBankInfo,
        accountType: String,
        initialBalance: Double,
        accountNumber: String,
        autoSync: Boolean
    ): OpenFinanceAccountEntity = withContext(Dispatchers.IO) {
        val account = OpenFinanceAccountEntity(
            id = "predef_" + bank.id + "_" + System.currentTimeMillis(),
            bankName = bank.name,
            accountType = accountType,
            accountNumber = accountNumber.ifBlank { "0000" },
            balance = initialBalance,
            connectedAt = System.currentTimeMillis(),
            status = "CONECTADO",
            lastSync = System.currentTimeMillis()
        )
        openFinanceDao.insertAccount(account)

        // Também tentar persistir no Supabase
        supabaseClient?.let { sb ->
            try {
                val payload = JSONObject().apply {
                    put("id", account.id)
                    put("bank_name", account.bankName)
                    put("bank_code", bank.code)
                    put("account_type", account.accountType)
                    put("account_number", account.accountNumber)
                    put("current_balance", account.balance)
                    put("is_connected", true)
                }
                val req = Request.Builder()
                    .url("${sb.baseUrl}/rest/v1/open_finance_accounts")
                    .addHeader("apikey", sb.anonKey)
                    .addHeader("Authorization", "Bearer ${sb.anonKey}")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "resolution=merge-duplicates")
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                httpClient.newCall(req).execute()
            } catch (e: Exception) {
                Log.e("OpenFinance", "Erro ao salvar conta no Supabase: ${e.message}")
            }
        }

        account
    }

    suspend fun createAccount(
        bankName: String,
        accountType: String,
        accountNumber: String,
        initialBalance: Double,
        autoSync: Boolean = true
    ): OpenFinanceAccountEntity = withContext(Dispatchers.IO) {
        val account = OpenFinanceAccountEntity(
            id = "custom_" + UUID.randomUUID().toString(),
            bankName = bankName,
            accountType = accountType,
            accountNumber = accountNumber.ifBlank { "0000" },
            balance = initialBalance,
            connectedAt = System.currentTimeMillis(),
            status = "CONECTADO",
            lastSync = System.currentTimeMillis()
        )
        openFinanceDao.insertAccount(account)
        account
    }

    suspend fun disconnectBank(bankId: String) = disconnectAccount(bankId)

    suspend fun disconnectAccount(accountId: String) = withContext(Dispatchers.IO) {
        openFinanceDao.deleteAccountById(accountId)
        openFinanceDao.deleteTransactionsForAccount(accountId)

        // Remover também do Supabase
        supabaseClient?.let { sb ->
            try {
                val req = Request.Builder()
                    .url("${sb.baseUrl}/rest/v1/open_finance_accounts?id=eq.$accountId")
                    .addHeader("apikey", sb.anonKey)
                    .addHeader("Authorization", "Bearer ${sb.anonKey}")
                    .delete()
                    .build()
                httpClient.newCall(req).execute()
            } catch (_: Exception) {}
        }
    }

    /**
     * Importa extratos manuais em texto (OFX, cópia de extrato bancário).
     */
    suspend fun importStatementText(rawText: String, targetAccountId: String? = null): Int = withContext(Dispatchers.IO) {
        var count = 0
        val lines = rawText.lines()
        val txEntities = mutableListOf<OpenFinanceTransactionEntity>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.length < 5) continue

            val dateRegex = Regex("(\\d{2}/\\d{2}/\\d{4}|\\d{4}-\\d{2}-\\d{2})")
            val amountRegex = Regex("(R\\$\\s*)?([+-]?\\s*\\d{1,3}(\\.\\d{3})*,\\d{2}|\\d+\\.\\d{2})")

            val dateMatch = dateRegex.find(trimmed)?.value ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val amountMatch = amountRegex.find(trimmed)?.value

            if (amountMatch != null) {
                val parsedAmount = amountMatch.parseBrDouble() ?: 0.0
                val absAmount = Math.abs(parsedAmount)
                if (absAmount > 0.0) {
                    val desc = trimmed.replace(dateRegex, "").replace(amountRegex, "").trim()
                    val finalDesc = if (desc.length > 2) desc else "Transação Bancária Importada"
                    val txId = UUID.randomUUID().toString()

                    txEntities.add(
                        OpenFinanceTransactionEntity(
                            id = txId,
                            accountId = targetAccountId ?: "default_account",
                            description = finalDesc,
                            amount = absAmount,
                            date = dateMatch,
                            category = "Extrato Bancário",
                            isSyncedToExpenses = true
                        )
                    )

                    val expense = ExpenseEntity(
                        storeName = finalDesc,
                        totalAmount = absAmount,
                        category = "Extrato Bancário",
                        paymentMethod = "OPEN_FINANCE",
                        transactionDate = dateMatch,
                        rawOcrText = "Importação de Extrato",
                        isManual = false,
                        source = "OPEN_FINANCE",
                        notes = "Importado via Extrato Bancário"
                    )
                    expenseDao.insertExpense(expense)
                    count++
                }
            }
        }

        if (txEntities.isNotEmpty()) {
            openFinanceDao.insertTransactions(txEntities)
        }
        count
    }

    fun getApiSpecJson(): String {
        return "{\"standard\": \"Open Finance Brasil - BACEN\", \"security\": \"FAPI 1.0 Advanced / OAuth 2.0 mTLS\"}"
    }
}
