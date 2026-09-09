package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.CategoryTotal
import com.example.data.db.ExpenseEntity
import com.example.data.db.HouseholdEntity
import com.example.data.db.OpenFinanceAccountEntity
import com.example.data.gemini.ScannedReceiptResult
import com.example.data.openfinance.OpenFinanceBankInfo
import com.example.data.openfinance.OpenFinanceRepository
import com.example.data.agent.AgentRepository
import com.example.data.model.AgentInfo
import com.example.data.model.AgentNotification
import com.example.data.model.CustomAgent
import com.example.data.model.MonthlyReport
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

    val householdProfile: StateFlow<HouseholdProfile?> = householdDao.getHouseholdProfile()
        .map { it?.toDomain() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val budgetAnalysis: StateFlow<BudgetAnalysis?> = combine(expenses, householdProfile) { exps, profile ->
        if (profile != null && profile.isConfigured && profile.monthlyIncome > 0) {
            calculateBudgetUseCase.execute(exps, profile)
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
        // Inicializa dados reais do banco conectado (Mercado Pago e extratos) se ainda não existirem
        viewModelScope.launch {
            try {
                openFinanceRepo.ensureInitialSeed()
            } catch (e: Exception) {
                android.util.Log.e("ExpenseViewModel", "Erro ao inicializar seed Open Finance: ${e.message}")
            }
        }

        // Auto-sincronizacao em tempo real ao inicializar caso haja sessao ativa
        viewModelScope.launch {
            if (supabaseAuthRepo.getCurrentSession() != null) {
                syncWithCloud()
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

    fun updateExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            expenseDao.updateExpense(expense)
        }
    }

    fun scanReceiptWithGemini(bitmap: Bitmap) {
        viewModelScope.launch {
            _scanState.value = ScanUiState.Scanning
            val result = aiProvider.analyzeReceipt(bitmap)
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
            val result = nfceFetcher.fetchReceiptFromHtml(html, urlOrKey)
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
            val result = nfceFetcher.fetchReceipt(urlOrKey)
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
                itemsJson = receipt.items.joinToString(" | ")
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
}
