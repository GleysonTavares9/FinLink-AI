package com.example.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.OpenFinanceAccountEntity
import com.example.data.db.OpenFinanceBillEntity
import com.example.data.db.OpenFinanceTransactionEntity
import com.example.data.openfinance.OpenFinanceBankInfo
import com.example.ui.components.BankLogoView
import com.example.util.formatBr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenFinanceScreen(
    connectedAccounts: List<OpenFinanceAccountEntity>,
    bills: List<OpenFinanceBillEntity> = emptyList(),
    futureInstallments: List<OpenFinanceTransactionEntity> = emptyList(),
    pendingTransactions: List<OpenFinanceTransactionEntity> = emptyList(),
    availableBanks: List<OpenFinanceBankInfo>,
    apiSpecJson: String,
    onBack: () -> Unit,
    onConnectBank: (bank: OpenFinanceBankInfo, accountType: String, initialBalance: Double, accountNumber: String, autoSync: Boolean) -> Unit,
    onSyncAccount: (accountId: String, onDone: (Int) -> Unit) -> Unit,
    onCreateCustomAccount: (bankName: String, accountType: String, accountNumber: String, initialBalance: Double, autoSync: Boolean) -> Unit,
    onImportStatementText: (rawText: String, targetAccountId: String?, onDone: (Int) -> Unit) -> Unit,
    onDisconnectBank: (id: String) -> Unit,
    onFetchToken: ((String?) -> Unit) -> Unit = {},
    isSyncing: Boolean = false,
    remainingManualSyncs: Int = 2,
    onManualSync: ((Boolean, String) -> Unit) -> Unit = {}
) {
    val context = LocalContext.current

    // Abas: 0 = Contas & Saldos, 1 = Cartões & Faturas
    var selectedTab by remember { mutableStateOf(0) }

    // Estados do Pluggy Connect
    var showPluggyWebView by remember { mutableStateOf(false) }
    var activeConnectToken by remember { mutableStateOf<String?>(null) }
    var selectedConnectorId by remember { mutableStateOf<String?>(null) }
    var isGeneratingToken by remember { mutableStateOf(false) }
    var isSyncingNow by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    val triggerSync: () -> Unit = {
        if (!isRefreshing && !isSyncingNow && !isSyncing) {
            isRefreshing = true
            isSyncingNow = true
            onManualSync { allowed, msg ->
                isRefreshing = false
                isSyncingNow = false
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    val effectiveAccounts = connectedAccounts

    // Saldo Total Consolidado (Contas Correntes e Investimentos)
    val depositAccounts = effectiveAccounts.filter {
        !it.accountType.contains("Cartão", ignoreCase = true) &&
        !it.accountType.contains("Cartao", ignoreCase = true) &&
        !it.accountType.contains("Crédito", ignoreCase = true) &&
        !it.accountType.contains("Credito", ignoreCase = true) &&
        it.accountType != "CREDIT" &&
        it.creditLimit == null &&
        it.brand == null
    }
    val totalBalance = depositAccounts.sumOf { it.balance }

    // Cartões de Crédito
    val creditAccounts = effectiveAccounts.filter {
        it.accountType.contains("Cartão", ignoreCase = true) ||
        it.accountType.contains("Cartao", ignoreCase = true) ||
        it.accountType.contains("Crédito", ignoreCase = true) ||
        it.accountType.contains("Credito", ignoreCase = true) ||
        it.accountType == "CREDIT" ||
        it.creditLimit != null ||
        it.brand != null
    }

    // Totais de Crédito
    val totalCreditLimit = creditAccounts.sumOf { it.creditLimit ?: 0.0 }
    val totalAvailableCredit = creditAccounts.sumOf { it.availableCredit ?: 0.0 }
    val totalOpenBill = creditAccounts.sumOf { it.balance }
    val creditUsedRatio = if (totalCreditLimit > 0) {
        ((totalCreditLimit - totalAvailableCredit) / totalCreditLimit).coerceIn(0.0, 1.0).toFloat()
    } else 0f

    // Iniciar fluxo do Pluggy Connect
        // Iniciar fluxo do Pluggy Connect
    fun launchPluggyFlow(connectorId: String? = null) {
        isGeneratingToken = true
        selectedConnectorId = connectorId
        activeConnectToken = null
        Toast.makeText(context, "Conectando aos servidores do Open Finance...", Toast.LENGTH_SHORT).show()
        onFetchToken { token ->
            isGeneratingToken = false
            if (!token.isNullOrBlank()) {
                activeConnectToken = token
                showPluggyWebView = true
            } else {
                Toast.makeText(context, "Erro ao obter token seguro. Verifique a conexão.", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Open Finance Brasil",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Text(
                            "BACEN FAPI • Contas, Cartões & Faturas",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (remainingManualSyncs > 0) Color(0xFFF1F5F9) else Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { triggerSync() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSyncingNow || isSyncing || isRefreshing) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color(0xFF0F172A))
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$remainingManualSyncs/2 hoje",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing || isSyncing || isSyncingNow,
            onRefresh = triggerSync,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp)
            ) {
            // SELETOR DE ABAS ESTILO IOS
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFE2E8F0).copy(alpha = 0.6f)
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        val tab1Selected = selectedTab == 0
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedTab = 0 },
                            color = if (tab1Selected) Color.White else Color.Transparent,
                            shadowElevation = if (tab1Selected) 3.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (tab1Selected) Color(0xFF4F46E5) else Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Contas & Saldos",
                                    fontSize = 13.sp,
                                    fontWeight = if (tab1Selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (tab1Selected) Color(0xFF1E293B) else Color(0xFF64748B)
                                )
                            }
                        }

                        val tab2Selected = selectedTab == 1
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedTab = 1 },
                            color = if (tab2Selected) Color.White else Color.Transparent,
                            shadowElevation = if (tab2Selected) 3.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CreditCard,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (tab2Selected) Color(0xFF4F46E5) else Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Cartões & Faturas",
                                    fontSize = 13.sp,
                                    fontWeight = if (tab2Selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (tab2Selected) Color(0xFF1E293B) else Color(0xFF64748B)
                                )
                                if (creditAccounts.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = if (tab2Selected) Color(0xFF4F46E5) else Color(0xFF94A3B8)
                                    ) {
                                        Text(
                                            text = "${creditAccounts.size}",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // ABA 0: CONTAS & SALDOS
            // ==========================================
            if (selectedTab == 0) {
                // 1. CARD DE SALDO CONSOLIDADO
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Color(0x18000000)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFF1E1B4B), Color(0xFF312E81))
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "SALDO EM CONTA CONSOLIDADO",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF818CF8),
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = formatBr(totalBalance),
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.2f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF10B981))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${depositAccounts.size} Contas",
                                                color = Color(0xFF34D399),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Security,
                                            contentDescription = null,
                                            tint = Color(0xFF818CF8),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Criptografia BACEN Open Finance",
                                            fontSize = 11.sp,
                                            color = Color(0xFFC7D2FE)
                                        )
                                    }

                                    Text(
                                        text = "API Pluggy v2",
                                        fontSize = 11.sp,
                                        color = Color(0xFFA5B4FC),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. BOTÃO DE CONEXÃO
                item {
                    Button(
                        onClick = { launchPluggyFlow() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(6.dp, RoundedCornerShape(14.dp), spotColor = Color(0x304F46E5)),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        enabled = !isGeneratingToken
                    ) {
                        if (isGeneratingToken) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Abrindo Open Finance...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Conectar Nova Instituição Bancária",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 3. CARROSSEL DE BANCOS DISPONÍVEIS
                item {
                    Text(
                        text = "Instituições Suportadas (Brasil)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(availableBanks) { bank ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { launchPluggyFlow(bank.id) },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                shadowElevation = 2.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(bank.primaryColor))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = bank.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. LISTA DE CONTAS CORRENTES E INVESTIMENTOS
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Contas Bancárias Ativas",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${depositAccounts.size} ativas",
                            fontSize = 12.sp,
                            color = Color(0xFF4F46E5),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (depositAccounts.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(36.dp))
                                Text("Nenhuma Conta Conectada", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Conecte sua conta bancária oficial para sincronizar saldos e extratos em tempo real.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }

                items(depositAccounts) { acc ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    BankLogoView(bankName = acc.bankName, size = 42.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = acc.bankName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(text = "${acc.accountType} • ${acc.accountNumber}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = {
                                        onSyncAccount(acc.id) {
                                            Toast.makeText(context, "${acc.bankName} sincronizado!", Toast.LENGTH_SHORT).show()
                                        }
                                    }) {
                                        Icon(Icons.Default.Sync, contentDescription = "Sincronizar", tint = Color(0xFF4F46E5))
                                    }
                                    IconButton(onClick = {
                                        onDisconnectBank(acc.id)
                                        Toast.makeText(context, "${acc.bankName} desconectado.", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Desconectar", tint = Color(0xFFEF4444))
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Saldo Disponível", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = formatBr(acc.balance),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (acc.balance >= 0) Color(0xFF0F172A) else Color(0xFFEF4444)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "BACEN OFICIAL",
                                        color = Color(0xFF059669),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // ABA 1: CARTÕES & FATURAS (PLUGGY CREDIT ENGINE)
            // ==========================================
            if (selectedTab == 1) {
                // 1. CARD RESUMO DO LIMITE TOTAL
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Color(0x20000000)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "FATURA ATUAL EM ABERTO",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF94A3B8),
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = formatBr(totalOpenBill),
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF6366F1).copy(alpha = 0.25f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color(0xFFA5B4FC), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "${creditAccounts.size} Cartão(ões)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFA5B4FC)
                                            )
                                        }
                                    }
                                }

                                // Barra de Progresso de Limite
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Limite Comprometido: ${(creditUsedRatio * 100).toInt()}%", fontSize = 11.sp, color = Color(0xFFCBD5E1))
                                        Text("Disponível: ${formatBr(totalAvailableCredit)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                                    }

                                    LinearProgressIndicator(
                                        progress = { creditUsedRatio },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = if (creditUsedRatio > 0.85f) Color(0xFFEF4444) else if (creditUsedRatio > 0.60f) Color(0xFFF59E0B) else Color(0xFF6366F1),
                                        trackColor = Color(0xFF334155)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Total Contratado: ${formatBr(totalCreditLimit)}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                        Text("Open Finance v2", fontSize = 10.sp, color = Color(0xFF64748B))
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. DETALHE DOS CARTÕES CONECTADOS
                item {
                    Text(
                        text = "Cartões Conectados",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                if (creditAccounts.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(36.dp))
                                Text("Nenhum Cartão Conectado", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Conecte seu banco para visualizar faturas em aberto, parcelamentos e limites automáticos.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                Button(
                                    onClick = { launchPluggyFlow() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Conectar Cartão", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                items(creditAccounts) { card ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    BankLogoView(bankName = card.bankName, size = 44.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = card.bankName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(
                                            text = "${card.brand ?: "CARTÃO"} ${card.level ?: ""} • final ${card.accountNumber.takeLast(4)}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF4F46E5).copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = card.brand ?: "CRÉDITO",
                                        color = Color(0xFF4F46E5),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            // Informações de Ciclo da Fatura
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Fatura em Aberto", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = formatBr(card.balance),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F172A)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Fechamento & Vencimento", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Fecha ${card.closeDate ?: "--"} • Vence ${card.dueDate ?: "--"}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF334155)
                                        )
                                    }
                                }
                            }

                            // Limites e Pagamento Mínimo
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Limite Total:", fontSize = 10.sp, color = Color(0xFF64748B))
                                        Text(formatBr(card.creditLimit ?: 0.0), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("Disponível:", fontSize = 10.sp, color = Color(0xFF64748B))
                                        Text(formatBr(card.availableCredit ?: 0.0), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Pag. Mínimo:", fontSize = 10.sp, color = Color(0xFF64748B))
                                        Text(formatBr(card.minimumPayment ?: 0.0), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. SEÇÃO: FATURA ATUAL EM ABERTO (COMPRAS PENDENTES / A VENCER)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lançamentos da Fatura Atual",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${pendingTransactions.size} compras",
                            fontSize = 12.sp,
                            color = Color(0xFF4F46E5),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (pendingTransactions.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Nenhuma compra pendente não faturada no momento.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                items(pendingTransactions) { tx ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.description,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = tx.date,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    if (tx.installmentNumber != null && tx.totalInstallments != null) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF4F46E5).copy(alpha = 0.1f)
                                        ) {
                                            Text(
                                                text = "Parcela ${tx.installmentNumber}/${tx.totalInstallments}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF4F46E5),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formatBr(tx.amount),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = Color(0xFFEF4444)
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF59E0B).copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "A vencer",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFD97706),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. SEÇÃO: PARCELAS FUTURAS MÊS A MÊS (PROJEÇÃO DO GUIA TÉCNICO)
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Parcelas Futuras (Previsão de Faturas)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${futureInstallments.size} parcelas",
                            fontSize = 12.sp,
                            color = Color(0xFF4F46E5),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (futureInstallments.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Nenhum parcelamento futuro identificado nesta conta.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                } else {
                    // Agrupamento por billForecastDate (YYYY-MM)
                    val groupedByForecast = futureInstallments.groupBy { it.billForecastDate ?: "Futuro" }
                    groupedByForecast.forEach { (forecastMonth, txList) ->
                        val monthTotal = txList.sumOf { it.amount }
                        val formattedTitle = when {
                            forecastMonth.contains("-11") -> "Fatura de Novembro / 2026"
                            forecastMonth.contains("-12") -> "Fatura de Dezembro / 2026"
                            forecastMonth.contains("-10") -> "Fatura de Outubro / 2026"
                            forecastMonth.contains("-01") -> "Fatura de Janeiro / 2027"
                            else -> "Previsão de Fatura: $forecastMonth"
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = formattedTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Text(
                                            text = formatBr(monthTotal),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                    }

                                    HorizontalDivider(color = Color(0xFFF1F5F9))

                                    txList.forEach { itemTx ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(itemTx.description, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                if (itemTx.installmentNumber != null && itemTx.totalInstallments != null) {
                                                    Text(
                                                        text = "Parcela ${itemTx.installmentNumber} de ${itemTx.totalInstallments}",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF4F46E5),
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                            Text(
                                                text = formatBr(itemTx.amount),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFEF4444)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. SEÇÃO: HISTÓRICO DE FATURAS FECHADAS (BILLS PLUGGY)
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Histórico de Faturas Fechadas",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${bills.size} faturas",
                            fontSize = 12.sp,
                            color = Color(0xFF4F46E5),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (bills.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Nenhuma fatura fechada anterior encontrada.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                items(bills) { bill ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Vencimento: ${bill.dueDate}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                if (!bill.closingDate.isNullOrBlank()) {
                                    Text(
                                        text = "Fechamento: ${bill.closingDate}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                if (bill.minimumPaymentAmount > 0) {
                                    Text(
                                        text = "Pagamento Mínimo: ${formatBr(bill.minimumPaymentAmount)}",
                                        fontSize = 11.sp,
                                        color = Color(0xFFF59E0B),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formatBr(bill.totalAmount),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (bill.isPaid) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (bill.isPaid) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("PAGA", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF059669))
                                        } else {
                                            Text("EM ABERTO", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFD97706))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }

    // Abertura do Pluggy Connect via Chrome Custom Tabs
    val customTabsIntent = remember {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
            .setColorScheme(CustomTabsIntent.COLOR_SCHEME_DARK)
            .build()
    }

    LaunchedEffect(showPluggyWebView, activeConnectToken) {
        if (showPluggyWebView && !activeConnectToken.isNullOrBlank()) {
            val connectorParam = if (!selectedConnectorId.isNullOrBlank()) {
                val numId = when (selectedConnectorId!!.lowercase()) {
                    "mercadopago" -> "206"
                    "nubank" -> "201"
                    "itau" -> "2"
                    "bradesco" -> "203"
                    "bb" -> "1"
                    "santander" -> "208"
                    "inter" -> "205"
                    "c6" -> "207"
                    "caixa" -> "204"
                    else -> selectedConnectorId
                }
                "&connector_id=$numId&selected_connector_id=$numId&connectorId=$numId"
            } else ""
            val pluggyUrl = "https://connect.pluggy.ai/?connect_token=$activeConnectToken&connectToken=$activeConnectToken&includeSandbox=false&with_sandbox=false$connectorParam&redirectUrl=finlink://pluggy-callback"
            try {
                customTabsIntent.launchUrl(context, Uri.parse(pluggyUrl))
            } catch (e: Exception) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pluggyUrl))
                    context.startActivity(intent)
                } catch (e2: Exception) {
                    Toast.makeText(context, "Erro ao abrir o navegador: ${e2.message}", Toast.LENGTH_LONG).show()
                }
            }
            showPluggyWebView = false
        }
    }
}