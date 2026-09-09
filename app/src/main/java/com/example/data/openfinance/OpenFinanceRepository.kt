package com.example.data.openfinance

import android.util.Log
import com.example.data.db.ExpenseDao
import com.example.data.db.ExpenseEntity
import com.example.data.db.OpenFinanceAccountEntity
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

    /**
     * Garante que o banco conectado Mercado Pago e suas transações reais estejam
     * sempre presentes no SQLite local e prontos para consulta no Open Finance.
     */
    suspend fun ensureInitialSeed() = withContext(Dispatchers.IO) {
        // Limpa quaisquer dados mockados anteriores para garantir fidelidade aos dados reais
        val fakeAccount = openFinanceDao.getAccountById("mp_primary_001")
        if (fakeAccount != null) {
            openFinanceDao.deleteAccountById("mp_primary_001")
            openFinanceDao.deleteTransactionsForAccount("mp_primary_001")
            Log.d("OpenFinance", "Conta mockada removida. Sistema pronto para dados reais.")
        }
        // Sincroniza contas reais existentes na nuvem
        syncFromPluggy("sync_all")
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
                    transactionsArray = json.optJSONArray("transactions")
                    Log.d("OpenFinance", "Sync Pluggy finalizado: Contas: ${accountsJson?.length()}, Transações: ${transactionsArray?.length()}")
                } else {
                    Log.e("OpenFinance", "Erro HTTP no sync: ${resp.code} - ${resp.body?.string()}")
                }
            } catch (e: Exception) {
                Log.e("OpenFinance", "Exceção ao sincronizar via Supabase Edge: ${e.message}")
            }
        }

        // 2. Fallback: Se não retornou contas ou transações da Pluggy, buscar do Supabase REST
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
        // 3. Gravar contas reais no Room Local
        if (accountsJson != null && accountsJson.length() > 0) {
            for (i in 0 until accountsJson.length()) {
                val acc = accountsJson.getJSONObject(i)
                val accId = acc.optString("id", itemId)
                val type = acc.optString("account_type", acc.optString("subtype", acc.optString("type", "Conta Corrente")))
                val balance = acc.optDouble("current_balance", acc.optDouble("balance", 0.0))
                val number = acc.optString("account_number", acc.optString("number", "0000"))
                val name = acc.optString("bank_name", acc.optString("name", bankName))

                val entity = OpenFinanceAccountEntity(
                    id = accId,
                    bankName = name,
                    accountType = type.replace("_", " "),
                    accountNumber = if (number.startsWith("Nº")) number else "Nº $number",
                    balance = balance,
                    connectedAt = System.currentTimeMillis(),
                    status = "CONECTADO",
                    lastSync = System.currentTimeMillis()
                )
                openFinanceDao.insertAccount(entity)
                syncedCount++
            }
        }

        // 4. Gravar transações reais no Room Local e como despesas no App
        if (transactionsArray != null && transactionsArray.length() > 0) {
            val txEntities = mutableListOf<OpenFinanceTransactionEntity>()
            for (j in 0 until transactionsArray.length()) {
                val tx = transactionsArray.getJSONObject(j)
                val txId = tx.optString("id", UUID.randomUUID().toString())
                val accId = tx.optString("account_id", itemId)
                val desc = tx.optString("description", "Transação Bancária")
                val rawAmount = tx.optDouble("amount", 0.0)
                val pType = tx.optString("payment_type", tx.optString("type", ""))
                val isCredit = pType.equals("CREDIT", ignoreCase = true)
                val absAmount = Math.abs(rawAmount)
                val date = tx.optString("transaction_date", tx.optString("date", "")).take(10).ifBlank {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                }
                val category = tx.optString("category", "Outros")

                // A) Adicionar à lista de transações Open Finance normalizada
                txEntities.add(
                    OpenFinanceTransactionEntity(
                        id = txId,
                        accountId = accId,
                        description = desc,
                        amount = absAmount,
                        type = if (isCredit) "CREDIT" else "DEBIT",
                        date = date,
                        category = category,
                        isSyncedToExpenses = true
                    )
                )
                // Nao salvar duplicado em ExpenseEntity — lido diretamente de open_finance_transactions
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
