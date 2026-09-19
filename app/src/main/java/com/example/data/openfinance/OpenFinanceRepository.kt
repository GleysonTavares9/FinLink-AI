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
        // Limpar mocks e seeds legados do SQLite local
        try {
            openFinanceDao.cleanLegacyMockAccounts()
            openFinanceDao.cleanLegacyMockTransactions()
            openFinanceDao.cleanLegacyMockBills()
            expenseDao.cleanLegacyMockExpenses()
            Log.d("OpenFinance", "Limpeza de dados legados executada com sucesso.")
        } catch (e: Exception) {
            Log.e("OpenFinance", "Erro ao limpar dados legados: ${e.message}")
        }

        // Sincroniza contas reais existentes na nuvem / Pluggy
        try {
            syncFromPluggy("sync_all")
        } catch (e: Exception) {
            Log.e("OpenFinance", "Erro ao sincronizar Pluggy inicial: ${e.message}")
        }
    }

    suspend fun fetchPluggyConnectToken(): String? = withContext(Dispatchers.IO) {
        val sb = supabaseClient ?: return@withContext null
        val session = sb.sessionManager.getSession()
        val tokenUrl = "${sb.baseUrl}/functions/v1/pluggy-connect?action=token"

        // 1. Tentar com JWT da sessão ativa
        val token = callTokenEndpoint(tokenUrl, sb.anonKey, session?.accessToken, session?.user?.id)
        if (!token.isNullOrBlank()) {
            Log.d("OpenFinance", "Connect Token gerado com sucesso via sessão.")
            return@withContext token
        }

        // 2. Fallback resiliente com anonKey caso o token de sessão tenha expirado
        val fallbackToken = callTokenEndpoint(tokenUrl, sb.anonKey, null, session?.user?.id)
        if (!fallbackToken.isNullOrBlank()) {
            Log.d("OpenFinance", "Connect Token gerado com sucesso via anonKey fallback.")
            return@withContext fallbackToken
        }

        Log.e("OpenFinance", "Falha definitiva ao obter Connect Token da Pluggy.")
        null
    }

    private fun callTokenEndpoint(url: String, anonKey: String, accessToken: String?, userId: String?): String? {
        try {
            val payload = JSONObject().apply {
                userId?.let { put("userId", it) }
            }
            val authHeader = if (!accessToken.isNullOrBlank()) "Bearer $accessToken" else "Bearer $anonKey"
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val bodyStr = resp.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                val token = json.optString("accessToken")
                if (token.isNotBlank()) return token
            } else {
                Log.w("OpenFinance", "callTokenEndpoint retornou código HTTP ${resp.code}")
            }
        } catch (e: Exception) {
            Log.e("OpenFinance", "Exceção em callTokenEndpoint: ${e.message}")
        }
        return null
    }

    /**
     * Sincroniza dados reais da Pluggy (Contas, Saldos e Extrato de Transações)
     * e salva no SQLite local (Room) e no Supabase.
     */
    suspend fun syncFromPluggy(itemId: String): Int = withContext(Dispatchers.IO) {
        try {
            openFinanceDao.cleanLegacyMockAccounts()
            openFinanceDao.cleanLegacyMockTransactions()
            openFinanceDao.cleanLegacyMockBills()
            expenseDao.cleanLegacyMockExpenses()
        } catch (_: Exception) {}

        val sb = supabaseClient ?: return@withContext 0
        val session = sb.sessionManager.getSession()
        val authHeader = if (session != null && session.accessToken.isNotBlank()) {
            "Bearer ${session.accessToken}"
        } else {
            "Bearer ${sb.anonKey}"
        }

        var syncedCount = 0

        // Se itemId == "sync_all", primeiro sincronizar cada item conectado via Edge Function
        if (itemId == "sync_all") {
            try {
                val itemIdsUrl = "${sb.baseUrl}/rest/v1/open_finance_accounts?select=item_id"
                val itemReq = Request.Builder()
                    .url(itemIdsUrl)
                    .addHeader("apikey", sb.anonKey)
                    .addHeader("Authorization", authHeader)
                    .get()
                    .build()
                val itemResp = httpClient.newCall(itemReq).execute()
                if (itemResp.isSuccessful) {
                    val rawItems = itemResp.body?.string() ?: "[]"
                    val parsedItems = JSONArray(rawItems)
                    val distinctIds = mutableSetOf<String>()
                    for (k in 0 until parsedItems.length()) {
                        val itObj = parsedItems.getJSONObject(k)
                        val idVal = itObj.optString("item_id")
                        if (idVal.isNotBlank() && idVal != "null") {
                            distinctIds.add(idVal)
                        }
                    }
                    for (singleItemId in distinctIds) {
                        try {
                            syncSingleItemFromPluggy(sb, authHeader, session?.user?.id, singleItemId)
                        } catch (itemErr: Exception) {
                            Log.e("OpenFinance", "Erro ao sincronizar item individual $singleItemId: ${itemErr.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("OpenFinance", "Erro ao buscar item_ids para sync_all: ${e.message}")
            }
        } else {
            // Sincroniza o item específico recebido (ex: callback do Pluggy Connect)
            try {
                val count = syncSingleItemFromPluggy(sb, authHeader, session?.user?.id, itemId)
                if (count > 0) syncedCount += count
            } catch (e: Exception) {
                Log.e("OpenFinance", "Erro ao sincronizar item $itemId: ${e.message}")
            }
        }

        // Buscar todas as contas e transações consolidadas do Supabase REST para popular o Room Local
        var accountsJson: JSONArray? = null
        var billsJson: JSONArray? = null
        var transactionsArray: JSONArray? = null

        try {
            val restUrl = "${sb.baseUrl}/rest/v1/open_finance_accounts?select=*"
            val req = Request.Builder()
                .url(restUrl)
                .addHeader("apikey", sb.anonKey)
                .addHeader("Authorization", authHeader)
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

        try {
            val billsRestUrl = "${sb.baseUrl}/rest/v1/open_finance_bills?select=*&order=due_date.desc"
            val req = Request.Builder()
                .url(billsRestUrl)
                .addHeader("apikey", sb.anonKey)
                .addHeader("Authorization", authHeader)
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

        try {
            val txRestUrl = "${sb.baseUrl}/rest/v1/open_finance_transactions?select=*&order=transaction_date.desc&limit=500"
            val req = Request.Builder()
                .url(txRestUrl)
                .addHeader("apikey", sb.anonKey)
                .addHeader("Authorization", authHeader)
                .get()
                .build()
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val raw = resp.body?.string() ?: "[]"
                val parsed = JSONArray(raw)
                if (parsed.length() > 0) transactionsArray = parsed
            }
        } catch (e: Exception) {
            Log.e("OpenFinance", "Erro ao buscar transações do Supabase REST: ${e.message}")
        }

        // 3. Gravar contas reais no Room Local
        if (accountsJson != null && accountsJson.length() > 0) {
            for (i in 0 until accountsJson.length()) {
                val acc = accountsJson.getJSONObject(i)
                val accId = acc.optString("id", itemId)
                val type = acc.optString("account_type", acc.optString("subtype", acc.optString("type", "Conta Corrente")))
                val balance = acc.optDouble("current_balance", acc.optDouble("balance", 0.0))
                val number = acc.optString("account_number", acc.optString("number", "0000"))
                val name = acc.optString("bank_name", acc.optString("name", "Mercado Pago"))
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
                Log.d("OpenFinance", "${txEntities.size} transações reais gravadas no SQLite local")
            }
        }

        syncedCount
    }

    private fun syncSingleItemFromPluggy(
        sb: SupabaseClient,
        authHeader: String,
        userId: String?,
        itemId: String
    ): Int {
        val syncUrl = "${sb.baseUrl}/functions/v1/pluggy-connect?action=sync"
        val payload = JSONObject().apply {
            put("action", "sync")
            put("itemId", itemId)
            userId?.let { put("userId", it) }
        }

        val req = Request.Builder()
            .url(syncUrl)
            .addHeader("apikey", sb.anonKey)
            .addHeader("Authorization", authHeader)
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val resp = httpClient.newCall(req).execute()
        if (resp.isSuccessful) {
            val bodyStr = resp.body?.string() ?: ""
            val json = JSONObject(bodyStr)
            val txs = json.optJSONArray("transactions")
            val count = txs?.length() ?: 0
            Log.d("OpenFinance", "Sync do item $itemId finalizado com sucesso. Transações retornadas: $count")
            return count
        } else {
            Log.e("OpenFinance", "Erro HTTP no sync do item $itemId: ${resp.code}")
            return 0
        }
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
