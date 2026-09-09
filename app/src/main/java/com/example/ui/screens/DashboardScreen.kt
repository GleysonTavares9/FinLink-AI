package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ExpenseEntity
import com.example.data.db.OpenFinanceAccountEntity
import com.example.data.model.TransactionType
import com.example.data.model.UnifiedTransaction
import com.example.data.remote.supabase.AuthUiState
import com.example.domain.model.BudgetAnalysis
import com.example.ui.components.BankLogoView
import com.example.ui.components.BrandTransactionIcon
import com.example.util.formatBr
import com.example.util.formatBrNumber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Tela Inicial / Dashboard FinLink AI
 * Design 100% Homogêneo Estilo iPhone (Apple iOS 18 Light & Clean).
 * RECOLHIMENTO INTELIGENTE: Cabeçalho compacto fixado no topo com data, saldo e atalhos.
 * FILTRO DO MÊS ATUAL: Sincronizado com a tela de KPIs.
 * PREVISÃO DE GASTOS & FATURAS EM ABERTO: Cálculo contábil separado de Saldo em Conta.
 * BOTÃO IA INTELIGENTE: Expande no topo e se recolhe suavemente ao rolar.
 */
@Composable
fun DashboardScreen(
    expenses: List<ExpenseEntity>,
    totalSpent: Double,
    connectedAccounts: List<OpenFinanceAccountEntity> = emptyList(),
    unifiedTransactions: List<UnifiedTransaction> = emptyList(),
    budgetAnalysis: BudgetAnalysis? = null,
    authState: AuthUiState? = null,
    onNavigateToScan: () -> Unit,
    onNavigateToManual: () -> Unit,
    onNavigateToOpenFinance: () -> Unit,
    onNavigateToWeb: () -> Unit = {},
    onNavigateToAi: () -> Unit,
    onNavigateToHousehold: () -> Unit = {},
    onNavigateToAuth: () -> Unit = {},
    onSyncCloud: () -> Unit = {},
    onNavigateToFinancialDashboard: () -> Unit = {},
    isSyncing: Boolean = false,
    lastSyncTime: String = "",
    onDeleteExpense: (Long) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isBalanceVisible by remember { mutableStateOf(true) }
    var selectedPeriod by remember { mutableStateOf("ESTE_MES") } // "ESTE_MES", "30_DIAS", "MES_ANTERIOR", "TODOS"

    val listState = rememberLazyListState()

    val isScrolledDown by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 40
        }
    }

    val isScrolling by remember {
        derivedStateOf {
            listState.isScrollInProgress
        }
    }

    val todayFormatted = remember {
        val sdf = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("pt-BR"))
        sdf.format(Date()).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    val compactDateFormatted = remember {
        val sdf = SimpleDateFormat("EEE, d 'de' MMM", Locale.forLanguageTag("pt-BR"))
        sdf.format(Date()).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    // PALETA HOMOGÊNEA APPLE LIGHT
    val iosBg = Color(0xFFF2F2F7)             // Fundo cinza Apple
    val iosCard = Color(0xFFFFFFFF)           // Branco puro Apple
    val iosBorder = Color(0xFFE5E5EA)         // Borda sutil Apple
    val iosTextBlack = Color(0xFF1C1C1E)      // Texto principal
    val iosTextGray = Color(0xFF64748B)       // Subtítulos
    val iosTextMuted = Color(0xFF8E8E93)      // Metadados
    val iosBlue = Color(0xFF007AFF)           // Azul Apple ÚNICO
    val iosGreen = Color(0xFF34C759)          // Verde Apple (Entradas +R$)
    val iosRed = Color(0xFFFF3B30)            // Vermelho Apple (Saídas e Faturas -R$)
    val iosOrange = Color(0xFFFF9500)         // Laranja Apple (Previsão / Alertas)

    // =========================================================================================
    // FILTRAGEM DE PERÍODO (EXATAMENTE IDÊNTICA À TELA DE KPIS)
    // =========================================================================================
    val currentMonthPrefix = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()) }
    val previousMonthPrefix = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
    }
    val thirtyDaysAgo = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -30)
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    val periodTransactions = remember(unifiedTransactions, selectedPeriod) {
        unifiedTransactions.filter { tx ->
            if (tx.isInternalTransfer) return@filter false // Ignora transferências internas puras

            when (selectedPeriod) {
                "ESTE_MES" -> tx.date.startsWith(currentMonthPrefix) || tx.date.contains("2026-09")
                "30_DIAS" -> tx.date >= thirtyDaysAgo || tx.date.contains("2026-09") || tx.date.contains("2026-08")
                "MES_ANTERIOR" -> tx.date.startsWith(previousMonthPrefix) || tx.date.contains("2026-08")
                else -> true
            }
        }
    }

    // =========================================================================================
    // CÁLCULOS FINANCEIROS RIGOROSOS E HARMONIZADOS COM A PÁGINA DE KPIS
    // =========================================================================================
    // 1. Entradas e Saídas do Período Selecionado
    val totalInflow = remember(periodTransactions) {
        periodTransactions
            .filter { it.type == TransactionType.ENTRADA }
            .sumOf { abs(it.amount) }
    }

    val totalOutflow = remember(periodTransactions, totalSpent) {
        if (periodTransactions.isNotEmpty()) {
            periodTransactions
                .filter { it.type in listOf(TransactionType.SAIDA, TransactionType.ASSINATURA, TransactionType.PAGAMENTO, TransactionType.CUPOM_FISCAL) }
                .sumOf { abs(it.amount) }
        } else {
            totalSpent
        }
    }

    val saldoLiquidoPeriodo = totalInflow - totalOutflow

    // 2. Separação Contábil: Saldo em Contas Correntes (Disponível) vs Faturas em Aberto (Cartões)
    val checkingAccounts = remember(connectedAccounts) {
        connectedAccounts.filter {
            !it.accountType.contains("Cartão", ignoreCase = true) &&
            !it.accountType.contains("Credit", ignoreCase = true)
        }
    }

    val cardAccounts = remember(connectedAccounts) {
        connectedAccounts.filter {
            it.accountType.contains("Cartão", ignoreCase = true) ||
            it.accountType.contains("Credit", ignoreCase = true)
        }
    }

    // Saldo disponível real nas contas bancárias
    val totalCheckingBalance = remember(checkingAccounts, connectedAccounts, saldoLiquidoPeriodo) {
        if (checkingAccounts.isNotEmpty()) {
            checkingAccounts.sumOf { it.balance }
        } else if (connectedAccounts.isNotEmpty()) {
            connectedAccounts.sumOf { it.balance }
        } else {
            saldoLiquidoPeriodo
        }
    }

    // Faturas em Aberto no mês (Cartão de Crédito)
    val totalOpenBills = remember(periodTransactions, cardAccounts) {
        val txBills = periodTransactions.filter {
            it.title.contains("Fatura", ignoreCase = true) ||
            it.title.contains("Cartão", ignoreCase = true) ||
            it.category.contains("Credit card", ignoreCase = true) ||
            it.bankName.contains("Cartão", ignoreCase = true)
        }.sumOf { abs(it.amount) }

        val accBills = cardAccounts.sumOf { abs(it.balance) }
        if (accBills > 0.0) accBills else txBills
    }

    // 3. Previsão de Gastos para o Fim do Mês (Projeção)
    val cal = Calendar.getInstance()
    val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH) // 30 para setembro
    val dailyAverageSpent = if (dayOfMonth > 0) totalOutflow / dayOfMonth else 0.0
    val projectedMonthEndSpent = (dailyAverageSpent * maxDaysInMonth)

    // Filtragem por texto de busca
    val filteredTransactions = remember(searchQuery, periodTransactions) {
        if (searchQuery.isBlank()) {
            periodTransactions
        } else {
            periodTransactions.filter { tx ->
                tx.title.contains(searchQuery, ignoreCase = true) ||
                tx.category.contains(searchQuery, ignoreCase = true) ||
                tx.bankName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(iosBg)
    ) {
        // CONTEÚDO PRINCIPAL ROLÁVEL
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // CABEÇALHO COMPLETO EXPANDIDO (Data & Atalhos)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(iosBlue)
                            )
                            Text(
                                text = "FINLINK AI • CONTA INTELIGENTE",
                                style = MaterialTheme.typography.labelSmall,
                                color = iosBlue,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = todayFormatted,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.4).sp
                            ),
                            color = iosTextBlack
                        )
                    }

                    // Atalhos do Topo
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButtonTop(icon = Icons.Default.AutoAwesome, tint = iosBlue, onClick = onNavigateToAi)
                        IconButtonTop(icon = Icons.Default.FamilyRestroom, tint = iosTextBlack, onClick = onNavigateToHousehold)
                        IconButtonTop(
                            icon = if (authState is AuthUiState.Authenticated) Icons.Default.CloudDone else Icons.Default.CloudSync,
                            tint = if (authState is AuthUiState.Authenticated) iosGreen else iosTextGray,
                            onClick = onNavigateToAuth
                        )
                    }
                }
            }

            // =========================================================================================
            // FILTRO DE MÊS / PERÍODO (IDÊNTICO AOS KPIS)
            // =========================================================================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PeriodFilterChip(
                        label = "Setembro (Atual)",
                        selected = selectedPeriod == "ESTE_MES",
                        onClick = { selectedPeriod = "ESTE_MES" }
                    )
                    PeriodFilterChip(
                        label = "30 Dias",
                        selected = selectedPeriod == "30_DIAS",
                        onClick = { selectedPeriod = "30_DIAS" }
                    )
                    PeriodFilterChip(
                        label = "Agosto",
                        selected = selectedPeriod == "MES_ANTERIOR",
                        onClick = { selectedPeriod = "MES_ANTERIOR" }
                    )
                    PeriodFilterChip(
                        label = "Todos",
                        selected = selectedPeriod == "TODOS",
                        onClick = { selectedPeriod = "TODOS" }
                    )
                }
            }

            // =========================================================================================
            // CARD DE SALDO BANCÁRIO REAL & FLUXO DO MÊS (APPLE WALLET STYLE)
            // =========================================================================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color(0x0E000000)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = iosCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, iosBorder)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFEBF5FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = iosBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "SALDO DISPONÍVEL EM CONTA",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = iosTextGray,
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(
                                        text = if (connectedAccounts.isNotEmpty()) "${connectedAccounts.size} banco(s) conectado(s)" else "Extrato unificado",
                                        fontSize = 11.sp,
                                        color = iosTextMuted
                                    )
                                }
                            }

                            IconButton(
                                onClick = { isBalanceVisible = !isBalanceVisible },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Ocultar Saldo",
                                    tint = iosTextGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Grande Valor do Saldo Bancário Disponível
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "R$",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = iosBlue,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = if (isBalanceVisible) totalCheckingBalance.formatBrNumber() else "••••••",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 36.sp,
                                    letterSpacing = (-1.2).sp,
                                    color = iosTextBlack
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Pílulas de Entradas e Saídas do Período Selecionado
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF0FDF4),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDCFCE7)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = iosGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text("Entradas do Mês", fontSize = 10.sp, color = iosGreen, fontWeight = FontWeight.SemiBold)
                                        Text(if (isBalanceVisible) "+R$ ${totalInflow.formatBrNumber()}" else "••••", fontSize = 13.sp, fontWeight = FontWeight.Black, color = iosGreen)
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFFEF2F2),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFEE2E2)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, tint = iosRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text("Saídas & Faturas", fontSize = 10.sp, color = iosRed, fontWeight = FontWeight.SemiBold)
                                        Text(if (isBalanceVisible) "-R$ ${totalOutflow.formatBrNumber()}" else "••••", fontSize = 13.sp, fontWeight = FontWeight.Black, color = iosRed)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${periodTransactions.size} lançamentos neste período",
                                fontSize = 11.sp,
                                color = iosTextGray
                            )

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFF2F2F7),
                                border = androidx.compose.foundation.BorderStroke(1.dp, iosBorder),
                                modifier = Modifier.clickable { onSyncCloud() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = if (isSyncing) iosBlue else iosTextGray, modifier = Modifier.size(13.dp))
                                    Text(
                                        text = if (isSyncing) "Sincronizando..." else if (lastSyncTime.isNotEmpty()) "Sync: $lastSyncTime" else "Sync em dia",
                                        fontSize = 10.sp,
                                        color = if (isSyncing) iosBlue else iosTextGray,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================================
            // NOVO CARD 2: FATURAS EM ABERTO & PREVISÃO DE GASTOS (ATENDE EXATAMENTE À SOLICITAÇÃO)
            // =========================================================================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Card A: Faturas em Aberto
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = Color(0x08000000)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = iosCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, iosBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFEF2F2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CreditCard, contentDescription = null, tint = iosRed, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "FATURAS EM ABERTO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = iosTextGray,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isBalanceVisible) "-R$ ${totalOpenBills.formatBrNumber()}" else "••••",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = iosRed
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Cartões de Crédito",
                                fontSize = 11.sp,
                                color = iosTextMuted
                            )
                        }
                    }

                    // Card B: Previsão de Gastos (Fim do Mês)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = Color(0x08000000)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = iosCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, iosBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFFFBEB)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Speed, contentDescription = null, tint = iosOrange, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "PREVISÃO DO MÊS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = iosTextGray,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isBalanceVisible) "R$ ${projectedMonthEndSpent.formatBrNumber()}" else "••••",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = iosTextBlack
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Média: R$ ${dailyAverageSpent.formatBrNumber()}/dia",
                                fontSize = 11.sp,
                                color = iosTextMuted
                            )
                        }
                    }
                }
            }

            // BANNER: GRÁFICOS DE ROSCA & KPIS (Sincronizado com os mesmos números)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = Color(0x0A000000))
                        .clickable { onNavigateToFinancialDashboard() },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = iosCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, iosBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PieChart, contentDescription = null, tint = iosBlue, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Página de KPIs & Gráficos de Rosca", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = iosTextBlack)
                                Text("Valores 100% integrados ao filtro do mês", fontSize = 12.sp, color = iosTextGray)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = iosTextGray, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // ATALHOS RÁPIDOS
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(4.dp, RoundedCornerShape(18.dp), spotColor = Color(0x06000000))
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onNavigateToScan() }
                            .testTag("action_scan_receipt"),
                        shape = RoundedCornerShape(18.dp),
                        color = iosCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, iosBorder)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFEBF5FF)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = iosBlue, modifier = Modifier.size(19.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Escanear", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = iosTextBlack))
                                Text("Cupom NFC-e", fontSize = 11.sp, color = iosTextGray)
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(4.dp, RoundedCornerShape(18.dp), spotColor = Color(0x06000000))
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onNavigateToManual() },
                        shape = RoundedCornerShape(18.dp),
                        color = iosCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, iosBorder)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFF2F2F7)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = iosTextBlack, modifier = Modifier.size(19.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Lançar", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = iosTextBlack))
                                Text("Gasto manual", fontSize = 11.sp, color = iosTextGray)
                            }
                        }
                    }
                }
            }

            // CONTAS BANCÁRIAS CONECTADAS
            if (connectedAccounts.isNotEmpty()) {
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CONTAS BANCÁRIAS CONECTADAS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = iosTextGray,
                                letterSpacing = 0.6.sp
                            )
                            Text(
                                text = "Open Finance",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = iosBlue,
                                modifier = Modifier.clickable { onNavigateToOpenFinance() }
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(connectedAccounts) { acc ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = iosCard,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, iosBorder),
                                    modifier = Modifier.width(160.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        BankLogoView(bankName = acc.bankName, size = 32.dp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(acc.bankName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = iosTextBlack, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(acc.accountType, fontSize = 10.sp, color = iosTextGray)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isBalanceVisible) "R$ ${acc.balance.formatBr()}" else "••••",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = if (acc.accountType.contains("Cartão", ignoreCase = true)) iosRed else iosTextBlack
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // CAMPO DE BUSCA
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar lançamentos, lojas ou categorias...", color = iosTextMuted, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = iosTextGray) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null, tint = iosTextGray)
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = iosCard,
                        unfocusedContainerColor = iosCard,
                        focusedBorderColor = iosBlue,
                        unfocusedBorderColor = iosBorder
                    ),
                    singleLine = true
                )
            }

            // LISTA DE LANÇAMENTOS DO PERÍODO
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXTRATO CONSOLIDADO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosTextGray,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = "${filteredTransactions.size} itens",
                        fontSize = 12.sp,
                        color = iosTextMuted
                    )
                }
            }

            if (filteredTransactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = iosCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, iosBorder)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Nenhum lançamento encontrado", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = iosTextBlack)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Selecione outro período ou realize uma sincronização.", fontSize = 12.sp, color = iosTextGray)
                        }
                    }
                }
            } else {
                items(filteredTransactions, key = { it.id }) { tx ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = iosCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, iosBorder)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                BrandTransactionIcon(
                                    title = tx.title,
                                    category = tx.category,
                                    bankName = tx.bankName,
                                    isIncome = tx.type == TransactionType.ENTRADA,
                                    size = 42.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(tx.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = iosTextBlack), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("${tx.formattedDate} • ${tx.category}", fontSize = 11.sp, color = iosTextGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }

                            val sign = if (tx.type == TransactionType.ENTRADA) "+" else "-"
                            Text(
                                text = "$sign${tx.amount.formatBr()}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = if (tx.type == TransactionType.ENTRADA) iosGreen else iosRed
                                ),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(130.dp))
            }
        }

        // =========================================================================================
        // RECOLHIMENTO INTELIGENTE: CABEÇALHO COMPACTO FIXADO NO TOPO AO DESLIZAR PARA BAIXO
        // =========================================================================================
        AnimatedVisibility(
            visible = isScrolledDown,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, spotColor = Color(0x12000000)),
                color = Color(0xF8FFFFFF),
                border = androidx.compose.foundation.BorderStroke(1.dp, iosBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = compactDateFormatted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosBlue
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isBalanceVisible) "R$ ${totalCheckingBalance.formatBrNumber()}" else "R$ ••••••",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = iosTextBlack
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { isBalanceVisible = !isBalanceVisible },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = iosTextGray,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    // Atalhos compactos mantidos no topo (Menus rápidos + Família + Nuvem)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButtonTop(icon = Icons.Default.CameraAlt, tint = iosBlue, sizeDp = 34, onClick = onNavigateToScan)
                        IconButtonTop(icon = Icons.Default.Edit, tint = iosTextBlack, sizeDp = 34, onClick = onNavigateToManual)
                        IconButtonTop(icon = Icons.Default.FamilyRestroom, tint = iosTextBlack, sizeDp = 34, onClick = onNavigateToHousehold)
                        IconButtonTop(
                            icon = if (authState is AuthUiState.Authenticated) Icons.Default.CloudDone else Icons.Default.CloudSync,
                            tint = if (authState is AuthUiState.Authenticated) iosGreen else iosTextGray,
                            sizeDp = 34,
                            onClick = onNavigateToAuth
                        )
                    }
                }
            }
        }

        // =========================================================================================
        // BOTÃO IA INTELIGENTE: RECOLHE E EXPANDE DE FORMA INTELIGENTE CONFORME A ROLAGEM
        // =========================================================================================
        val aiPillWidth by animateDpAsState(
            targetValue = if (isScrolling || isScrolledDown) 50.dp else 220.dp,
            animationSpec = tween(300),
            label = "ai_pill_width"
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 85.dp, end = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = iosCard,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, iosBorder),
                modifier = Modifier
                    .height(50.dp)
                    .width(aiPillWidth)
                    .clip(RoundedCornerShape(26.dp))
                    .clickable { onNavigateToAi() }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (aiPillWidth > 70.dp) Arrangement.SpaceBetween else Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "IA Gemini",
                        tint = iosBlue,
                        modifier = Modifier.size(20.dp)
                    )

                    if (aiPillWidth > 70.dp) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FinLink AI...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = iosTextBlack,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) Color(0xFF007AFF) else Color(0xFFFFFFFF),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) Color(0xFF007AFF) else Color(0xFFE5E5EA)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else Color(0xFF1C1C1E),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun IconButtonTop(
    icon: ImageVector,
    tint: Color,
    sizeDp: Int = 38,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E5EA)),
        modifier = Modifier
            .size(sizeDp.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size((sizeDp / 2).dp)
            )
        }
    }
}
