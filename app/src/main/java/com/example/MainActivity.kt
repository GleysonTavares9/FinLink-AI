package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.remote.supabase.AuthUiState
import com.example.ui.screens.ActivitiesScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GeminiAssistantScreen
import com.example.ui.screens.HouseholdScreen
import com.example.ui.screens.ManualInputScreen
import com.example.ui.screens.OpenFinanceScreen
import com.example.ui.screens.ProfileAndOfferingsScreen
import com.example.ui.screens.ScanReceiptScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosGreen
import com.example.ui.theme.IosIndigo
import com.example.ui.theme.IosOrange
import com.example.ui.theme.IosPink
import com.example.ui.theme.IosPurple
import com.example.ui.theme.IosRed
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ExpenseViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        handleDeepLink(intent)

        // Agendamento diário de lembretes para envio de dados e notas fiscais
        com.example.util.DailyReminderManager.scheduleDailyReminder(this)

        // Solicitação de permissão de notificação para Android 13+ (TIRAMISU)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

                val expenses by viewModel.expenses.collectAsStateWithLifecycle()
                val totalSpent by viewModel.totalSpent.collectAsStateWithLifecycle()
                val connectedAccounts by viewModel.connectedAccounts.collectAsStateWithLifecycle()
                val householdProfile by viewModel.householdProfile.collectAsStateWithLifecycle()
                val budgetAnalysis by viewModel.budgetAnalysis.collectAsStateWithLifecycle()
                val scanState by viewModel.scanState.collectAsStateWithLifecycle()
                val aiResponse by viewModel.aiAdvisorResponse.collectAsStateWithLifecycle()
                val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
                val authState by viewModel.authState.collectAsStateWithLifecycle()
                val syncResult by viewModel.syncResult.collectAsStateWithLifecycle()
                val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
                val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
                val unifiedTransactions by viewModel.unifiedTransactions.collectAsStateWithLifecycle()
                val subscriptions by viewModel.subscriptions.collectAsStateWithLifecycle()

                var isSuspendedMenuOpen by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF3F5FA))
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = "splash",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .then(if (currentRoute != "splash") Modifier.padding(bottom = 80.dp) else Modifier)
                        ) {
                            composable("splash") {
                                SplashScreen(
                                    onSplashFinished = {
                                        val isAuthenticated = authState is AuthUiState.Authenticated
                                        val destination = if (isAuthenticated) "dashboard" else "auth"
                                        navController.navigate(destination) {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable("dashboard") {
                                DashboardScreen(
                                    expenses = expenses,
                                    totalSpent = totalSpent ?: 0.0,
                                    budgetAnalysis = budgetAnalysis,
                                    authState = authState,
                                    isSyncing = isSyncing,
                                    lastSyncTime = lastSyncTime,
                                    onNavigateToScan = { navController.navigate("scan_receipt") },
                                    onNavigateToManual = { navController.navigate("manual_input") },
                                    onNavigateToOpenFinance = { navController.navigate("open_finance") },
                                    onNavigateToAi = { navController.navigate("ai_assistant") },
                                    onNavigateToHousehold = { navController.navigate("household") },
                                    onNavigateToAuth = { navController.navigate("auth") },
                                    onSyncCloud = { viewModel.syncWithCloud() },
                                    onDeleteExpense = { id -> viewModel.deleteExpense(id) }
                                )
                            }

                            composable("manual_input") {
                                ManualInputScreen(
                                    onBack = { navController.popBackStack() },
                                    onSaveExpense = { storeName, amount, category, date, notes ->
                                        viewModel.addManualExpense(storeName, amount, category, date, "PIX", notes)
                                    }
                                )
                            }

                            composable("scan_receipt") {
                                ScanReceiptScreen(
                                    scanState = scanState,
                                    onBack = { navController.popBackStack() },
                                    onScanImage = { bmp -> viewModel.scanReceiptWithGemini(bmp) },
                                                                onScanNfceUrlOrKey = { urlOrKey -> viewModel.scanNfceUrlOrKey(urlOrKey) },
                            onScanNfceHtml = { html, url -> viewModel.scanNfceHtml(html, url) },
                                    onSaveReceipt = { res -> viewModel.saveScannedReceipt(res) },
                                    onResetScan = { viewModel.resetScanState() }
                                )
                            }

                            composable("open_finance") {
                                OpenFinanceScreen(
                                    connectedAccounts = connectedAccounts,
                                    availableBanks = viewModel.openFinanceRepo.availableBanks,
                                    apiSpecJson = viewModel.openFinanceRepo.getApiSpecJson(),
                                    onBack = { navController.popBackStack() },
                                    onConnectBank = { bank, type, balance, accNum, autoSync ->
                                        viewModel.connectOpenFinanceBank(bank, type, balance, accNum, autoSync)
                                    },
                                    onSyncAccount = { accId, onDone ->
                                        viewModel.syncOpenFinanceAccount(accId, onDone)
                                    },
                                    onCreateCustomAccount = { bankName, accountType, accountNumber, balance, autoSync ->
                                        viewModel.createCustomAccount(bankName, accountType, accountNumber, balance, autoSync)
                                    },
                                    onImportStatementText = { text, targetId, onDone ->
                                        viewModel.importBankStatementText(text, targetId, onDone)
                                    },
                                    onDisconnectBank = { id -> viewModel.disconnectOpenFinanceAccount(id) },
                                    onFetchToken = { callback -> viewModel.fetchPluggyConnectToken(callback) }
                                )
                            }

                            composable("ai_assistant") {
                                GeminiAssistantScreen(
                                    aiResponse = aiResponse,
                                    isLoading = isAiLoading,
                                    onBack = { navController.popBackStack() },
                                    onAskGemini = { prompt -> viewModel.askGeminiAdvisor(prompt) }
                                )
                            }

                            composable("household") {
                                HouseholdScreen(
                                    householdProfile = householdProfile,
                                    budgetAnalysis = budgetAnalysis,
                                    onUpdateProfile = { adults, children, income ->
                                        viewModel.updateHouseholdProfile(adults, children, income)
                                    },
                                    onConsultAiWithHousehold = {
                                        navController.navigate("ai_assistant")
                                        viewModel.askGeminiAdvisor("Por favor, faça um diagnóstico completo do orçamento familiar com base na composição de adultos/crianças e gastos reais.")
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable("auth") {
                                AuthScreen(
                                    authState = authState,
                                    syncResult = syncResult,
                                    isSyncing = isSyncing,
                                    onSignIn = { email, pass -> viewModel.signIn(email, pass) },
                                    onSignUp = { email, pass -> viewModel.signUp(email, pass) },
                                    onSignOut = { viewModel.signOut() },
                                    onSyncWithCloud = { viewModel.syncWithCloud() },
                        onGoogleSignIn = {
                            val googleAuthUrl = "${viewModel.supabaseClient.baseUrl}/auth/v1/authorize?provider=google&redirect_to=finlink://auth-callback"
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(googleAuthUrl))
                            startActivity(intent)
                        },
                        onBack = { navController.popBackStack() }
                                )
                            }

                            composable("activities") {
                                ActivitiesScreen(
                                    transactions = unifiedTransactions,
                                    onNavigateToScan = { navController.navigate("scan_receipt") },
                                    onNavigateToAi = { navController.navigate("ai_assistant") },
                                    onNavigateToManual = { navController.navigate("manual_input") },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable("profile") {
                                val email = if (authState is AuthUiState.Authenticated) (authState as AuthUiState.Authenticated).session.user.email ?: "gleyson@finlink.ai" else "gleyson@finlink.ai"
                                ProfileAndOfferingsScreen(
                                    userFullName = "GLEYSON TAVARES DE MIRANDA",
                                    userEmail = email,
                                    connectedBankCount = connectedAccounts.size.coerceAtLeast(1),
                                    appOfferings = viewModel.appOfferings,
                                    subscriptions = subscriptions,
                                    onToggleSubscription = { subId -> viewModel.toggleSubscriptionStatus(subId) },
                                    onNavigateToOpenFinance = { navController.navigate("open_finance") },
                                    onSignOut = {
                                        viewModel.signOut()
                                        navController.navigate("auth") {
                                            popUpTo("dashboard") { inclusive = true }
                                        }
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }

                    // Scrim suave translúcido para fechar o menu ao tocar fora (sem tela escura)
                    if (isSuspendedMenuOpen) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x33000000))
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { isSuspendedMenuOpen = false }
                        )
                    }

                    // MENU SUSPENSO MODERNO TRANSLÚCIDO EM VIDRO BRANCO
                    AnimatedVisibility(
                        visible = isSuspendedMenuOpen,
                        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 98.dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        IosSuspendedActionMenu(
                            isCloudConnected = authState is AuthUiState.Authenticated,
                            realCount = expenses.size,
                            onActivities = {
                                isSuspendedMenuOpen = false
                                navController.navigate("activities")
                            },
                            onProfile = {
                                isSuspendedMenuOpen = false
                                navController.navigate("profile")
                            },
                            onScanReceipt = {
                                isSuspendedMenuOpen = false
                                navController.navigate("scan_receipt")
                            },
                            onManualExpense = {
                                isSuspendedMenuOpen = false
                                navController.navigate("manual_input")
                            },
                            onOpenFinance = {
                                isSuspendedMenuOpen = false
                                navController.navigate("open_finance")
                            },
                            onHousehold = {
                                isSuspendedMenuOpen = false
                                navController.navigate("household")
                            },
                            onSyncCloud = {
                                isSuspendedMenuOpen = false
                                viewModel.syncWithCloud()
                            },
                            onAiAssistant = {
                                isSuspendedMenuOpen = false
                                navController.navigate("ai_assistant")
                            },
                            onAccountAuth = {
                                isSuspendedMenuOpen = false
                                navController.navigate("auth")
                            }
                        )
                    }

                    // DOCK FLUTUANTE DE NAVEGAÇÃO TRANSLÚCIDO (Floating Glass Bar)
                    if (currentRoute != "splash" && authState is AuthUiState.Authenticated) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            IosFloatingDock(
                                currentRoute = currentRoute,
                                isSuspendedOpen = isSuspendedMenuOpen,
                                onToggleSuspended = { isSuspendedMenuOpen = !isSuspendedMenuOpen },
                                onNavigate = { route ->
                                    isSuspendedMenuOpen = false
                                    if (currentRoute != route) {
                                        navController.navigate(route) {
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Quando o usuário volta do navegador após conectar uma conta bancária,
        // sincroniza automaticamente as novas contas cadastradas na nuvem
        viewModel.syncOpenFinanceAccount("sync_all") {}
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: android.content.Intent?) {
        val uri = intent?.data ?: return
        
        if (uri.scheme == "finlink") {
            if (uri.host == "auth-callback") {
                viewModel.handleAuthDeepLink(uri)
            } else if (uri.host == "pluggy-callback") {
                val itemId = uri.getQueryParameter("itemId")
                if (itemId != null) {
                    android.widget.Toast.makeText(this@MainActivity, "Conexão bancária realizada com sucesso! Sincronizando contas...", android.widget.Toast.LENGTH_SHORT).show()
                    viewModel.syncFromPluggy(itemId) { count ->
                        android.widget.Toast.makeText(this@MainActivity, "Sincronização concluída: $count transações importadas!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

}

/**
 * Menu Suspenso Flutuante (Suspended Action Sheet) com visual Glassmorphism e Efeito Vidro Translúcido.
 */
@Composable
fun IosSuspendedActionMenu(
    isCloudConnected: Boolean,
    realCount: Int,
    onActivities: () -> Unit,
    onProfile: () -> Unit,
    onScanReceipt: () -> Unit,
    onManualExpense: () -> Unit,
    onOpenFinance: () -> Unit,
    onHousehold: () -> Unit,
    onSyncCloud: () -> Unit,
    onAiAssistant: () -> Unit,
    onAccountAuth: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(24.dp, RoundedCornerShape(28.dp), spotColor = Color(0x11000000)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x99FFFFFF),
                                Color(0x66F4F6FB)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            Color(0x18000000)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                // Header com Badge do Banco de Dados Real Supabase
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "FINLINK AI • MENU SUSPENSO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = IosBlue,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Ações Rápidas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1D1D1F)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isCloudConnected) Color(0xFF34C759).copy(alpha = 0.14f) else Color(0xFFECEEF3)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isCloudConnected) Color(0xFF34C759) else IosOrange)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isCloudConnected) "Supabase Nuvem Ativa" else "Banco Local ($realCount)",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCloudConnected) Color(0xFF248A3D) else Color(0xFF1D1D1F),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Grid de Ações Rápidas com ícones iOS coloridos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IosMenuItem(
                        title = "Atividades",
                        subtitle = "Entradas e Saídas",
                        icon = Icons.Default.SwapVert,
                        gradient = listOf(IosGreen, Color(0xFF0F766E)),
                        onClick = onActivities,
                        modifier = Modifier.weight(1f)
                    )
                    IosMenuItem(
                        title = "Perfil & Planos",
                        subtitle = "Seja PRO e Recursos",
                        icon = Icons.Default.Person,
                        gradient = listOf(IosPurple, Color(0xFF6D28D9)),
                        onClick = onProfile,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IosMenuItem(
                        title = "Escanear NFC-e",
                        subtitle = "Cupom SEFAZ / OCR",
                        icon = Icons.Default.CameraAlt,
                        gradient = listOf(IosGreen, Color(0xFF029E60)),
                        onClick = onScanReceipt,
                        modifier = Modifier.weight(1f)
                    )
                    IosMenuItem(
                        title = "Lançar Manual",
                        subtitle = "Novo Gasto no Banco",
                        icon = Icons.Default.Edit,
                        gradient = listOf(IosBlue, Color(0xFF0056B3)),
                        onClick = onManualExpense,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IosMenuItem(
                        title = "Open Finance",
                        subtitle = "Contas e Extratos",
                        icon = Icons.Default.AccountBalance,
                        gradient = listOf(IosIndigo, Color(0xFF423EA8)),
                        onClick = onOpenFinance,
                        modifier = Modifier.weight(1f)
                    )
                    IosMenuItem(
                        title = "Orçamento Família",
                        subtitle = "Divisão Adulto/Criança",
                        icon = Icons.Default.FamilyRestroom,
                        gradient = listOf(IosOrange, Color(0xFFD67200)),
                        onClick = onHousehold,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IosMenuItem(
                        title = "Sincronizar Nuvem",
                        subtitle = "Supabase Realtime",
                        icon = Icons.Default.CloudSync,
                        gradient = listOf(Color(0xFF34C759), Color(0xFF1B8A5A)),
                        onClick = onSyncCloud,
                        modifier = Modifier.weight(1f)
                    )
                    IosMenuItem(
                        title = "Assistente IA",
                        subtitle = "Gemini Advisor",
                        icon = Icons.Default.AutoAwesome,
                        gradient = listOf(IosIndigo, Color(0xFF6366F1)),
                        onClick = onAiAssistant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun IosMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .shadow(4.dp, RoundedCornerShape(18.dp), spotColor = Color(0x10000000)),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFFFFFF),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF1D1D1F)
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = Color(0xFF86868B),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Dock de Navegação Flutuante com Efeito Vidro Translúcido (FinLink Floating Glass Dock).
 */
@Composable
fun IosFloatingDock(
    currentRoute: String,
    isSuspendedOpen: Boolean,
    onToggleSuspended: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isSuspendedOpen) 45f else 0f,
        label = "plus_rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(22.dp, RoundedCornerShape(34.dp), spotColor = Color(0x22000000)),
        shape = RoundedCornerShape(34.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xEEFFFFFF),
                            Color(0xF4F6F9FD)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            Color(0x18000000)
                        )
                    ),
                    shape = RoundedCornerShape(34.dp)
                )
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Item 1: Início
                IosDockTab(
                    icon = Icons.Default.Home,
                    label = "Início",
                    isSelected = currentRoute == "dashboard",
                    activeColor = IosBlue,
                    onClick = { onNavigate("dashboard") }
                )

                // Item 2: Atividades (Extrato completo com entradas, saídas e assinaturas)
                IosDockTab(
                    icon = Icons.Default.SwapVert,
                    label = "Atividades",
                    isSelected = currentRoute == "activities",
                    activeColor = IosGreen,
                    onClick = { onNavigate("activities") }
                )

                // BOTÃO CENTRAL DO MENU SUSPENSO (+)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .shadow(12.dp, CircleShape, spotColor = IosBlue.copy(alpha = 0.4f))
                        .background(
                            brush = Brush.linearGradient(
                                colors = if (isSuspendedOpen) listOf(IosRed, Color(0xFFB51A1A))
                                else listOf(IosBlue, IosGreen)
                            )
                        )
                        .clickable { onToggleSuspended() }
                        .testTag("btn_suspended_menu"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Menu Suspenso",
                        tint = Color.White,
                        modifier = Modifier
                            .size(26.dp)
                            .rotate(rotation)
                    )
                }

                // Item 3: Cupom AI
                IosDockTab(
                    icon = Icons.Default.CameraAlt,
                    label = "Cupom",
                    isSelected = currentRoute == "scan_receipt",
                    activeColor = IosOrange,
                    onClick = { onNavigate("scan_receipt") }
                )

                // Item 4: Perfil & Recursos
                IosDockTab(
                    icon = Icons.Default.Person,
                    label = "Perfil",
                    isSelected = currentRoute == "profile",
                    activeColor = IosPurple,
                    onClick = { onNavigate("profile") }
                )
            }
        }
    }
}

@Composable
fun IosDockTab(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) activeColor else Color(0xFF8E8E93),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) activeColor else Color(0xFF8E8E93)
        )
    }

}