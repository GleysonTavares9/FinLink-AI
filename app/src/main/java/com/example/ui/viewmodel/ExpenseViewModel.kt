package com.example.ui.viewmodel

import com.example.data.agent.FinancialAgentEngine
import com.example.data.agent.ChatHistoryRepository
import com.example.data.model.AgentChatMessage
import com.example.data.model.DefaultFinancialAgents
import com.example.data.model.FinancialAgentPersona
import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.CategoryTotal
import com.example.data.db.ExpenseEntity
import com.example.data.db.HouseholdEntity
import com.example.data.db.OpenFinanceAccountEntity
import com.example.data.db.OpenFinanceBillEntity
import com.example.data.db.OpenFinanceTransactionEntity
import com.example.data.gemini.ScannedReceiptResult
import com.example.data.openfinance.OpenFinanceBankInfo
import com.example.data.openfinance.OpenFinanceRepository
import com.example.data.agent.AgentRepository
import com.example.data.model.AgentInfo
import com.example.data.model.AgentNotification
import com.example.data.model.CustomAgent
import com.example.data.model.MonthlyReport
import com.example.data.model.AgentSpendingTarget
import com.example.data.agent.AgentSpendingTargetRepository
import com.example.data.openfinance.SyncQuotaManager
import com.example.data.remote.ai.AiProvider
import com.example.data.remote.ai.AiProviderFactory
import com.example.data.remote.nfce.NfceFetcher
import com.example.data.remote.supabase.AuthUiState
import com.example.data.remote.supabase.SupabaseAuthRepository
import com.example.data.remote.supabase.SupabaseClient
import com.example.data.remote.supabase.SupabaseSessionManager
import com.example.data.remote.supabase.SupabaseSyncRepository
import com.example.data.remote.supabase.SyncResult
import com.example.domain.model.BudgetAnalysis
import com.example.domain.model.HouseholdProfile
import com.example.domain.usecase.CalculateBudgetUseCase
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


sealed interface ScanUiState {
    object Idle : ScanUiState
    object Scanning : ScanUiState
    data class Success(val result: ScannedReceiptResult) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val expenseDao = db.expenseDao()
    private val openFinanceDao = db.openFinanceDao()
    private val householdDao = db.householdDao()

    private val aiProvider: AiProvider = AiProviderFactory.createProvider()
    private val nfceFetcher = NfceFetcher()
    private val calculateBudgetUseCase = CalculateBudgetUseCase()

    private val supabaseSessionManager = SupabaseSessionManager(application)
    val supabaseClient = SupabaseClient(supabaseSessionManager)
    val supabaseAuthRepo = SupabaseAuthRepository(supabaseClient, supabaseSessionManager)
    val supabaseSyncRepo = SupabaseSyncRepository(supabaseClient, supabaseSessionManager, expenseDao, householdDao)

    val openFinanceRepo = OpenFinanceRepository(openFinanceDao, expenseDao, supabaseClient)
    private val chatHistoryRepo = ChatHistoryRepository(application, supabaseClient)

    init {
        viewModelScope.launch {
            try {
                val savedSessions = chatHistoryRepo.loadSessions()
                if (savedSessions.isNotEmpty()) {
                    _agentChatSessions.value = savedSessions
                }
            } catch (e: Exception) {
                android.util.Log.e("ExpenseViewModel", "Erro ao carregar historico de chat", e)
            }
        }
    }

    val transactionRepo = com.example.data.repository.TransactionRepository(expenseDao, openFinanceDao)

    val unifiedTransactions: StateFlow<List<com.example.data.model.UnifiedTransaction>> = transactionRepo.getUnifiedTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val subscriptions: StateFlow<List<com.example.data.model.SubscriptionItem>> = transactionRepo.subscriptions

    val appOfferings: List<com.example.data.model.AppOfferingFeature> = transactionRepo.getAllOfferings()

    fun toggleSubscriptionStatus(subId: String) {
        transactionRepo.toggleSubscriptionStatus(subId)
    }

    fun addSubscription(item: com.example.data.model.SubscriptionItem) {
        transactionRepo.addSubscription(item)
    }

    fun addUnifiedTransaction(
        title: String,
        amount: Double,
        type: com.example.data.model.TransactionType,
        category: String,
        paymentMethod: String,
        date: String,
        bankName: String = "Mercado Pago",
        notes: String? = null
    ) {
        viewModelScope.launch {
            transactionRepo.addManualTransaction(title, amount, type, category, paymentMethod, date, bankName, notes)
        }
    }

    val agentRepo = AgentRepository(application)

    private val _defaultAgents = MutableStateFlow<List<AgentInfo>>(agentRepo.getDefaultAgents())
    val defaultAgents: StateFlow<List<AgentInfo>> = _defaultAgents.asStateFlow()

    private val _agentNotifications = MutableStateFlow<List<AgentNotification>>(emptyList())
    val agentNotifications: StateFlow<List<AgentNotification>> = _agentNotifications.asStateFlow()

    private val _customAgents = MutableStateFlow<List<CustomAgent>>(emptyList())
    val customAgents: StateFlow<List<CustomAgent>> = _customAgents.asStateFlow()

    private val _monthlyReport = MutableStateFlow<MonthlyReport?>(null)
    val monthlyReport: StateFlow<MonthlyReport?> = _monthlyReport.asStateFlow()

    private val _isAgentRunning = MutableStateFlow(false)
    val isAgentRunning: StateFlow<Boolean> = _isAgentRunning.asStateFlow()

    private val _agentStatusMessage = MutableStateFlow("")
    val agentStatusMessage: StateFlow<String> = _agentStatusMessage.asStateFlow()

    val authState: StateFlow<AuthUiState> = supabaseAuthRepo.authState

    private val _syncResult = MutableStateFlow<SyncResult?>(null)
    val syncResult: StateFlow<SyncResult?> = _syncResult.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val syncQuotaManager = SyncQuotaManager(application)
    private val agentSpendingTargetRepo = AgentSpendingTargetRepository(application)

    private val _remainingManualSyncs = MutableStateFlow(syncQuotaManager.getRemainingManualSyncs())
    val remainingManualSyncs: StateFlow<Int> = _remainingManualSyncs.asStateFlow()

    private val _agentSpendingTargets = MutableStateFlow<List<AgentSpendingTarget>>(agentSpendingTargetRepo.getTargets())
    val agentSpendingTargets: StateFlow<List<AgentSpendingTarget>> = _agentSpendingTargets.asStateFlow()

    val expenses: StateFlow<List<ExpenseEntity>> = expenseDao.getAllExpenses()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val categoryTotals: StateFlow<List<CategoryTotal>> = expenseDao.getCategoryTotals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalSpent: StateFlow<Double?> = expenseDao.getTotalSpent()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val connectedAccounts: StateFlow<List<OpenFinanceAccountEntity>> = openFinanceRepo.getConnectedAccounts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val bills: StateFlow<List<OpenFinanceBillEntity>> = openFinanceRepo.getAllBills()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val futureInstallments: StateFlow<List<OpenFinanceTransactionEntity>> = openFinanceRepo.getFutureInstallments()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val pendingTransactions: StateFlow<List<OpenFinanceTransactionEntity>> = openFinanceRepo.getPendingTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val householdProfile: StateFlow<HouseholdProfile?> = householdDao.getHouseholdProfile()
        .map { it?.toDomain() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val budgetAnalysis: StateFlow<BudgetAnalysis?> = combine(unifiedTransactions, householdProfile) { unifiedList, profile ->
        if (profile != null && profile.isConfigured && profile.monthlyIncome > 0) {
            calculateBudgetUseCase.executeWithUnified(unifiedList, profile)
        } else {
            null
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    private val _aiAdvisorResponse = MutableStateFlow<String>("")
    val aiAdvisorResponse: StateFlow<String> = _aiAdvisorResponse.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<String>("")
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    init {
        // Seed inicial executado APENAS na primeira abertura do app
        // Protege dados reais da Pluggy de serem sobrescritos em toda abertura
        if (syncQuotaManager.isFirstRun()) {
            viewModelScope.launch {
                try {
                    openFinanceRepo.ensureInitialSeed()
                    syncQuotaManager.markFirstRunDone()
                    android.util.Log.d("ExpenseViewModel", "Seed inicial executado pela primeira vez.")
                } catch (e: Exception) {
                    android.util.Log.e("ExpenseViewModel", "Erro seed: ${e.message}")
                }
            }
        }
        // Auto-sincronizacao em tempo real ao inicializar caso haja sessao ativa
        viewModelScope.launch {
            if (supabaseAuthRepo.getCurrentSession() != null) {
                syncWithCloud()
            }
        }

        // Auto-sincronização diária ao abrir o aplicativo (1x por dia automaticamente via Pluggy)
        viewModelScope.launch {
            syncQuotaManager.checkAndPerformDailyAutoSync {
                android.util.Log.d("ExpenseViewModel", "Iniciando auto-sync diário do Open Finance ao abrir o app...")
                viewModelScope.launch {
                    try {
                        _isSyncing.value = true
                        if (supabaseAuthRepo.getCurrentSession() != null) {
                            syncWithCloud()
                        }
                        openFinanceRepo.syncFromPluggy("sync_all")
                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                        _lastSyncTime.value = sdf.format(Date())
                        android.util.Log.d("ExpenseViewModel", "Auto-sync diário da Pluggy concluído.")
                    } catch (e: Exception) {
                        android.util.Log.e("ExpenseViewModel", "Erro no auto-sync diário: ${e.message}")
                    } finally {
                        _isSyncing.value = false
                    }
                }
            }
        }

        // Monitoramento contínuo dos limites customizados dos agentes educadores
        viewModelScope.launch {
            combine(unifiedTransactions, connectedAccounts) { txList, accounts ->
                val cardBalance = accounts.filter {
                    it.accountType.contains("Cartão", true) ||
                    it.accountType.contains("Crédito", true) ||
                    it.accountType == "CREDIT"
                }.sumOf { it.balance }
                Pair(txList, cardBalance)
            }.collect { (txList, cardBalance) ->
                val evaluated = agentSpendingTargetRepo.evaluateSpendingAndNotify(txList, cardBalance)
                _agentSpendingTargets.value = evaluated
            }
        }
    }

    fun addManualExpense(
        storeName: String,
        amount: Double,
        category: String,
        date: String,
        paymentMethod: String = "PIX",
        notes: String? = null
    ) {
        viewModelScope.launch {
            val entity = ExpenseEntity(
                storeName = storeName,
                totalAmount = amount,
                category = category,
                paymentMethod = paymentMethod,
                transactionDate = date,
                isManual = true,
                source = "MANUAL",
                notes = notes
            )
            val generatedId = expenseDao.insertExpense(entity)
            // Sincroniza imediatamente com a nuvem Supabase em background
            if (supabaseAuthRepo.getCurrentSession() != null) {
                syncWithCloud()
            }
        }
    }

    /**
     * Separa despesas parceladas mês a mês, distribuindo cada parcela para o seu mês recorrente.
     * Ex: 3 parcelas de R$ 100 iniciadas em 2026-09:
     * - Parcela 1/3: 2026-09
     * - Parcela 2/3: 2026-10
     * - Parcela 3/3: 2026-11
     */
    fun addInstallmentExpense(
        storeName: String,
        totalAmount: Double,
        category: String,
        startDate: String,
        totalInstallments: Int,
        paymentMethod: String = "Cartão de Crédito",
        notes: String? = null
    ) {
        viewModelScope.launch {
            val count = totalInstallments.coerceAtLeast(1)
            val instAmount = totalAmount / count
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val baseDate = try { sdf.parse(startDate) ?: Date() } catch (e: Exception) { Date() }

            for (i in 0 until count) {
                val cal = Calendar.getInstance()
                cal.time = baseDate
                cal.add(Calendar.MONTH, i)
                val instDate = sdf.format(cal.time)
                val title = if (count > 1) "$storeName (Parcela ${i + 1} de $count)" else storeName
                val instNote = if (count > 1) "Parcela ${i + 1}/$count${if (!notes.isNullOrBlank()) " • $notes" else ""}" else notes

                val entity = ExpenseEntity(
                    storeName = title,
                    totalAmount = instAmount,
                    category = category,
                    paymentMethod = paymentMethod,
                    transactionDate = instDate,
                    isManual = true,
                    source = if (count > 1) "MANUAL_PARCELADO" else "MANUAL",
                    notes = instNote
                )
                expenseDao.insertExpense(entity)
            }
            if (supabaseAuthRepo.getCurrentSession() != null) {
                syncWithCloud()
            }
        }
    }

    fun updateExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            expenseDao.updateExpense(expense)
            if (supabaseAuthRepo.getCurrentSession() != null) {
                syncWithCloud()
            }
        }
    }

    fun updateManualExpense(
        id: Long,
        storeName: String,
        amount: Double,
        category: String,
        date: String,
        paymentMethod: String,
        notes: String?
    ) {
        viewModelScope.launch {
            val existing = expenseDao.getExpenseById(id)
            if (existing != null) {
                val updated = existing.copy(
                    storeName = storeName,
                    totalAmount = amount,
                    category = category,
                    transactionDate = date,
                    paymentMethod = paymentMethod,
                    notes = notes
                )
                expenseDao.updateExpense(updated)
                if (supabaseAuthRepo.getCurrentSession() != null) {
                    syncWithCloud()
                }
            }
        }
    }

    private fun getHouseholdContextString(): String? {
        val profile = householdProfile.value
        if (profile == null || !profile.isConfigured) return null
        return "FamÃ­lia com ${profile.adultCount} adulto(s) e ${profile.childCount} crianÃ§a(s). Renda Mensal: R$ ${profile.monthlyIncome}."
    }

    fun scanReceiptWithGemini(bitmap: Bitmap) {
        viewModelScope.launch {
            _scanState.value = ScanUiState.Scanning
            val context = getHouseholdContextString()
            val result = aiProvider.analyzeReceipt(bitmap, context)
            result.fold(
                onSuccess = { receipt ->
                    _scanState.value = ScanUiState.Success(receipt)
                },
                onFailure = { error ->
                    _scanState.value = ScanUiState.Error(error.message ?: "Erro ao analisar cupom via IA")
                }
            )
        }
    }

        fun scanNfceHtml(html: String, urlOrKey: String) {
        viewModelScope.launch {
            _scanState.value = ScanUiState.Scanning
            val context = getHouseholdContextString()
            val result = nfceFetcher.fetchReceiptFromHtml(html, urlOrKey, context)
            result.fold(
                onSuccess = { receipt ->
                    _scanState.value = ScanUiState.Success(receipt)
                },
                onFailure = { error ->
                    _scanState.value = ScanUiState.Error(error.message ?: "Erro ao extrair NFC-e a partir do navegador")
                }
            )
        }
    }

    fun scanNfceUrlOrKey(urlOrKey: String) {
        viewModelScope.launch {
            _scanState.value = ScanUiState.Scanning
            val context = getHouseholdContextString()
            val result = nfceFetcher.fetchReceipt(urlOrKey, context)
            result.fold(
                onSuccess = { receipt ->
                    _scanState.value = ScanUiState.Success(receipt)
                },
                onFailure = { error ->
                    _scanState.value = ScanUiState.Error(error.message ?: "Erro ao consultar NFC-e na SEFAZ")
                }
            )
        }
    }

    fun saveScannedReceipt(receipt: ScannedReceiptResult, imagePath: String? = null) {
        viewModelScope.launch {
            val source = if (receipt.confidence.contains("SEFAZ", ignoreCase = true)) "NFC-E_SEFAZ" else "OCR_GEMINI"
            val finalNotes = if (!receipt.familyAnalysis.isNullOrBlank()) "AnÃ¡lise Familiar via Gemini: ${receipt.familyAnalysis}" else null
            val entity = ExpenseEntity(
                storeName = receipt.storeName,
                totalAmount = receipt.totalAmount,
                category = receipt.category,
                paymentMethod = receipt.paymentMethod,
                transactionDate = receipt.transactionDate,
                rawOcrText = receipt.rawText,
                receiptImagePath = imagePath,
                isManual = false,
                source = source,
                itemsJson = receipt.items.joinToString(" | "),
                notes = finalNotes
            )
            expenseDao.insertExpense(entity)
            _scanState.value = ScanUiState.Idle
            if (supabaseAuthRepo.getCurrentSession() != null) {
                syncWithCloud()
            }
        }
    }

    fun resetScanState() {
        _scanState.value = ScanUiState.Idle
    }

    fun updateHouseholdProfile(adults: Int, children: Int, monthlyIncome: Double, notes: String = "Família padrão") {
        viewModelScope.launch {
            val profile = HouseholdProfile(
                id = 1L,
                adultCount = adults.coerceAtLeast(1),
                childCount = children.coerceAtLeast(0),
                monthlyIncome = monthlyIncome.coerceAtLeast(0.0),
                notes = notes
            )
            householdDao.insertOrUpdateProfile(HouseholdEntity.fromDomain(profile))
            if (supabaseAuthRepo.getCurrentSession() != null) {
                syncWithCloud()
            }
        }
    }

    fun connectOpenFinanceBank(
        bankInfo: OpenFinanceBankInfo,
        accountType: String = if (bankInfo.id == "nubank") "Cartão de Crédito / NuConta" else "Conta Corrente",
        initialBalance: Double = 1500.0,
        accountNumber: String = "",
        autoSync: Boolean = true,
        onSuccess: (Int) -> Unit = {}
    ) {
        viewModelScope.launch {
            val account = openFinanceRepo.connectPredefinedBank(bankInfo, accountType, initialBalance, accountNumber, autoSync)
            if (autoSync && supabaseAuthRepo.getCurrentSession() != null) {
                syncWithCloud()
            }
            onSuccess(if (autoSync) 3 else 0)
        }
    }

    fun fetchPluggyConnectToken(onReady: (String?) -> Unit) {
        viewModelScope.launch {
            val token = openFinanceRepo.fetchPluggyConnectToken()
            onReady(token)
        }
    }

    fun syncFromPluggy(itemId: String, onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val count = openFinanceRepo.syncFromPluggy(itemId)
            if (supabaseAuthRepo.getCurrentSession() != null) {
                syncWithCloud()
            }
            onComplete(count)
        }
    }

    fun syncOpenFinanceAccount(accountId: String, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val count = openFinanceRepo.syncFromPluggy(accountId)
            if (count > 0 && supabaseAuthRepo.getCurrentSession() != null) {
                syncWithCloud()
            }
            onComplete(count)
        }
    }

    fun createCustomAccount(
        bankName: String,
        accountType: String,
        accountNumber: String,
        initialBalance: Double,
        autoSync: Boolean = true
    ) {
        viewModelScope.launch {
            openFinanceRepo.createAccount(bankName, accountType, accountNumber, initialBalance, autoSync)
            if (autoSync && supabaseAuthRepo.getCurrentSession() != null) {
                syncWithCloud()
            }
        }
    }

    /**
     * Executa sincronização manual respeitando o limite diário de até 2 atualizações.
     */
    fun performManualSyncWithQuota(onResult: (Boolean, String) -> Unit) {
        val allowed = syncQuotaManager.consumeManualSync()
        _remainingManualSyncs.value = syncQuotaManager.getRemainingManualSyncs()

        if (!allowed) {
            onResult(false, "Você já utilizou suas 2 atualizações manuais de hoje. O FinLink AI atualiza seus dados automaticamente todo dia ao abrir o app.")
            return
        }

        viewModelScope.launch {
            _isSyncing.value = true
            try {
                if (supabaseAuthRepo.getCurrentSession() != null) {
                    syncWithCloud()
                }
                val totalUpdated = openFinanceRepo.syncFromPluggy("sync_all")
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                _lastSyncTime.value = sdf.format(Date())
                val remaining = _remainingManualSyncs.value
                val msg = if (totalUpdated > 0) {
                    "Sincronização manual concluída! $totalUpdated atualizações da Pluggy. ($remaining/2 restantes hoje)"
                } else {
                    "Sincronização concluída. Seus dados da Pluggy estão em dia! ($remaining/2 restantes hoje)"
                }
                onResult(true, msg)
            } catch (e: Exception) {
                onResult(true, "Sincronização concluída com sucesso! (${_remainingManualSyncs.value}/2 restantes hoje)")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun saveAgentSpendingTarget(target: AgentSpendingTarget) {
        viewModelScope.launch {
            agentSpendingTargetRepo.saveTarget(target)
            val accounts = connectedAccounts.value
            val cardBalance = accounts.filter {
                it.accountType.contains("Cartão", true) ||
                it.accountType.contains("Crédito", true) ||
                it.accountType == "CREDIT"
            }.sumOf { it.balance }
            val evaluated = agentSpendingTargetRepo.evaluateSpendingAndNotify(unifiedTransactions.value, cardBalance)
            _agentSpendingTargets.value = evaluated
        }
    }

    fun deleteAgentSpendingTarget(targetId: String) {
        viewModelScope.launch {
            agentSpendingTargetRepo.deleteTarget(targetId)
            _agentSpendingTargets.value = agentSpendingTargetRepo.getTargets()
        }
    }

    fun importBankStatementText(rawText: String, targetAccountId: String? = null, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val count = openFinanceRepo.importStatementText(rawText, targetAccountId)
            if (count > 0 && supabaseAuthRepo.getCurrentSession() != null) {
                syncWithCloud()
            }
            onComplete(count)
        }
    }

    fun disconnectOpenFinanceAccount(id: String) {
        viewModelScope.launch {
            openFinanceRepo.disconnectBank(id)
        }
    }

    fun askGeminiAdvisor(userPrompt: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            val currentExpenses = expenses.value
            val profile = householdProfile.value
            val analysis = budgetAnalysis.value
            val ptBr = Locale.forLanguageTag("pt-BR")

            val summaryText = if (currentExpenses.isEmpty()) {
                "Sem lançamentos cadastrados até o momento."
            } else {
                val total = currentExpenses.sumOf { it.totalAmount }
                val categories = currentExpenses.groupBy { it.category }
                    .map { "${it.key}: R$ ${String.format(ptBr, "%,.2f", it.value.sumOf { exp -> exp.totalAmount })}" }
                    .joinToString(", ")
                "Total gasto: R$ ${String.format(ptBr, "%,.2f", total)}\nDistribuição por categorias: $categories\n" +
                "Últimas 8 transações:\n" +
                currentExpenses.take(8).joinToString("\n") {
                    "• ${it.transactionDate} - ${it.storeName}: R$ ${String.format(ptBr, "%,.2f", it.totalAmount)} [${it.category} / ${it.paymentMethod}]"
                }
            }

            val householdContext = if (profile != null && analysis != null) {
                """
                    Composição Familiar: ${profile.adultCount} adulto(s) e ${profile.childCount} criança(s) (Total: ${profile.totalMembers} pessoas).
                    Renda familiar declarada: R$ ${String.format(ptBr, "%,.2f", profile.monthlyIncome)}.
                    Orçamento de referência ideal: R$ ${String.format(ptBr, "%,.2f", analysis.totalRecommendedBudget)}.
                    Gasto atual per capita: R$ ${String.format(ptBr, "%,.2f", analysis.totalPerCapitaSpent)} por pessoa.
                    Projeção para o fim do mês: R$ ${String.format(ptBr, "%,.2f", analysis.projectedMonthEndSpent)}.
                    Diagnóstico de categorias: ${analysis.summaryMessage}
                """.trimIndent()
            } else {
                "Perfil familiar ainda não configurado pelo usuário."
            }

            val response = aiProvider.getFinancialAdvice(summaryText, userPrompt, householdContext)
            response.fold(
                onSuccess = { advice -> _aiAdvisorResponse.value = advice },
                onFailure = { _aiAdvisorResponse.value = "Não foi possível conectar ao assistente no momento. Verifique sua conexão." }
            )
            _isAiLoading.value = false
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            expenseDao.deleteExpenseById(id)
            if (supabaseAuthRepo.getCurrentSession() != null) {
                supabaseSyncRepo.deleteRemoteExpense(id)
            }
        }
    }

    fun signIn(email: String, pass: String) {
        viewModelScope.launch {
            val res = supabaseAuthRepo.signIn(email, pass)
            if (res.isSuccess) {
                syncWithCloud()
            }
        }
    }

    fun signUp(email: String, pass: String) {
        viewModelScope.launch {
            val res = supabaseAuthRepo.signUp(email, pass)
            if (res.isSuccess) {
                syncWithCloud()
            }
        }
    }

    
    

    
    fun handleAuthDeepLink(uri: android.net.Uri) {
        viewModelScope.launch {
            supabaseAuthRepo.handleOAuthCallback(uri)
            syncWithCloud()
        }
    }

    fun signOut() {
        viewModelScope.launch {
            supabaseAuthRepo.signOut()
            _syncResult.value = null
            _lastSyncTime.value = ""
        }
    }

    fun updateUserProfile(name: String, avatarUrl: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = supabaseAuthRepo.updateUserProfile(name, avatarUrl)
            onDone(res.isSuccess)
        }
    }

    fun syncWithCloud() {
        viewModelScope.launch {
            _isSyncing.value = true
            val currentExpenses = expenses.value
            val currentHousehold = householdProfile.value
            val res = supabaseSyncRepo.syncAll(currentExpenses, currentHousehold)
            _syncResult.value = res
            _isSyncing.value = false
            if (res.isSuccess) {
                val now = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                _lastSyncTime.value = now
            }
        }
    }

    fun loadAgentsData() {
        viewModelScope.launch {
            _agentNotifications.value = agentRepo.fetchNotifications()
            _customAgents.value = agentRepo.fetchCustomAgents()
            _monthlyReport.value = agentRepo.fetchMonthlyReport()
        }
    }

    fun runAlbertTriage() {
        viewModelScope.launch {
            _isAgentRunning.value = true
            _agentStatusMessage.value = "Guardião Diário analisando anomalias e duplicidades..."
            val res = agentRepo.runAlbertTriage()
            _agentStatusMessage.value = res.getOrDefault("Varredura concluída com sucesso.")
            _agentNotifications.value = agentRepo.fetchNotifications()
            _isAgentRunning.value = false
        }
    }

    fun createCustomAgent(prompt: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isAgentRunning.value = true
            _agentStatusMessage.value = "Transformando regra em agente autônomo com IA..."
            val res = agentRepo.createCustomAgentFromPrompt(prompt)
            if (res.isSuccess) {
                _customAgents.value = agentRepo.fetchCustomAgents()
                _agentStatusMessage.value = "Novo Agente Ativado com Sucesso!"
                onComplete(true)
            } else {
                _agentStatusMessage.value = "Falha ao criar agente."
                onComplete(false)
            }
            _isAgentRunning.value = false
        }
    }

    fun toggleCustomAgent(agentId: String) {
        _customAgents.value = _customAgents.value.map { custom ->
            if (custom.id == agentId) custom.copy(isActive = !custom.isActive) else custom
        }
    }

    // --- INTEGRACAO DE AGENTES FINANCEIROS COM RACIOCINIO E WIDGETS ---
    private val financialAgentEngine = FinancialAgentEngine()

    private val _activeAgentPersona = MutableStateFlow<FinancialAgentPersona>(DefaultFinancialAgents.list.first())
    val activeAgentPersona: StateFlow<FinancialAgentPersona> = _activeAgentPersona.asStateFlow()

    private val _agentChatMessages = MutableStateFlow<List<AgentChatMessage>>(financialAgentEngine.getInitialMessages())
    val agentChatMessages: StateFlow<List<AgentChatMessage>> = _agentChatMessages.asStateFlow()

    private val _agentChatSessions = MutableStateFlow<List<com.example.data.model.AgentChatSession>>(emptyList())
    val agentChatSessions: StateFlow<List<com.example.data.model.AgentChatSession>> = _agentChatSessions.asStateFlow()

    private var currentChatSessionId: String? = null

    private val _isAgentThinking = MutableStateFlow<Boolean>(false)
    val isAgentThinking: StateFlow<Boolean> = _isAgentThinking.asStateFlow()

    private val _agentSearchStatus = MutableStateFlow<String>("")
    val agentSearchStatus: StateFlow<String> = _agentSearchStatus.asStateFlow()

    fun selectAgentPersona(persona: FinancialAgentPersona) {
        _activeAgentPersona.value = persona
    }

    fun toggleReasoningExpanded(messageId: String) {
        _agentChatMessages.value = _agentChatMessages.value.map { msg ->
            if (msg.id == messageId) msg.copy(isReasoningExpanded = !msg.isReasoningExpanded) else msg
        }
    }

    fun clearAgentChat() {
        val curId = currentChatSessionId
        val curMsgs = _agentChatMessages.value
        if (curId != null && curMsgs.isNotEmpty()) {
            val sessionToSave = _agentChatSessions.value.find { it.id == curId }
            if (sessionToSave != null) {
                viewModelScope.launch {
                    chatHistoryRepo.saveSession(sessionToSave)
                }
            }
        }
        currentChatSessionId = null
        _agentChatMessages.value = emptyList()
    }

    fun loadChatSession(session: com.example.data.model.AgentChatSession) {
        currentChatSessionId = session.id
        _agentChatMessages.value = session.messages
    }

    fun deleteChatSession(sessionId: String) {
        _agentChatSessions.value = _agentChatSessions.value.filter { it.id != sessionId }
        viewModelScope.launch {
            chatHistoryRepo.deleteSession(sessionId)
        }
        if (currentChatSessionId == sessionId) {
            currentChatSessionId = null
            _agentChatMessages.value = emptyList()
        }
    }

    fun sendAgentUserMessage(userPrompt: String) {
        if (userPrompt.isBlank()) return
        val userMsg = AgentChatMessage(
            id = java.util.UUID.randomUUID().toString(),
            isUser = true,
            text = userPrompt.trim(),
            timestamp = "Agora"
        )
        val updatedMsgs = _agentChatMessages.value + userMsg
        _agentChatMessages.value = updatedMsgs

        val sessionId = currentChatSessionId ?: java.util.UUID.randomUUID().toString().also { currentChatSessionId = it }
        val sessionTitle = _agentChatSessions.value.find { it.id == sessionId }?.title ?: userPrompt.trim().take(38)
        val sessionDate = java.text.SimpleDateFormat("dd/MM • HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val currentSessionObj = com.example.data.model.AgentChatSession(
            id = sessionId,
            title = sessionTitle,
            dateFormatted = sessionDate,
            messages = updatedMsgs
        )
        _agentChatSessions.value = listOf(currentSessionObj) + _agentChatSessions.value.filter { it.id != sessionId }
        viewModelScope.launch {
            chatHistoryRepo.saveSession(currentSessionObj)
        }

        viewModelScope.launch {
            _isAgentThinking.value = true
            try {
                val currentTransactions = unifiedTransactions.value
                val agentResponse = financialAgentEngine.processUserPrompt(
                    prompt = userPrompt,
                    activePersona = _activeAgentPersona.value,
                    transactions = currentTransactions,
                    householdProfile = householdProfile.value,
                    budgetAnalysis = budgetAnalysis.value,
                    aiProvider = aiProvider,
                    onProgress = { status ->
                        _agentSearchStatus.value = status
                    }
                )
                val finalMsgs = _agentChatMessages.value + agentResponse
                _agentChatMessages.value = finalMsgs
                val curId = currentChatSessionId
                if (curId != null) {
                    val updatedList = _agentChatSessions.value.map { sess ->
                        if (sess.id == curId) sess.copy(messages = finalMsgs) else sess
                    }
                    _agentChatSessions.value = updatedList
                    val finishedSession = updatedList.find { it.id == curId }
                    if (finishedSession != null) {
                        chatHistoryRepo.saveSession(finishedSession)
                    }
                }
                try {
                    com.example.util.FinancialAgentNotificationManager.notifyAgentResponse(
                        context = getApplication(),
                        agentName = _activeAgentPersona.value.name,
                        responseText = agentResponse.text
                    )
                } catch (e: Exception) {
                    android.util.Log.e("ExpenseViewModel", "Erro ao disparar notificacao do agente", e)
                }
            } catch (e: Exception) {
                android.util.Log.e("ExpenseViewModel", "Erro ao processar mensagem do agente", e)
            } finally {
                _isAgentThinking.value = false
                _agentSearchStatus.value = ""
            }
        }
    }
}
