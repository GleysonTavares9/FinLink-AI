package com.example.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.db.OpenFinanceAccountEntity
import com.example.data.db.OpenFinanceBillEntity
import com.example.data.db.OpenFinanceTransactionEntity
import com.example.data.model.AgentInfo
import com.example.data.model.AgentSpendingTarget
import com.example.ui.components.BankLogoView
import com.example.util.formatBr
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardManagementScreen(
    connectedAccounts: List<OpenFinanceAccountEntity>,
    bills: List<OpenFinanceBillEntity>,
    futureInstallments: List<OpenFinanceTransactionEntity>,
    pendingTransactions: List<OpenFinanceTransactionEntity>,
    agentTargets: List<AgentSpendingTarget>,
    availableAgents: List<AgentInfo>,
    remainingManualSyncs: Int,
    onBack: () -> Unit,
    onManualSync: (onResult: (Boolean, String) -> Unit) -> Unit,
    onSaveTarget: (AgentSpendingTarget) -> Unit,
    onDeleteTarget: (String) -> Unit,
    onNavigateToOpenFinanceConnect: () -> Unit
) {
    val context = LocalContext.current

    // Cartões de Crédito Reais
    val creditCards = connectedAccounts.filter {
        it.accountType.contains("Cartão", ignoreCase = true) ||
        it.accountType.contains("Crédito", ignoreCase = true) ||
        it.accountType == "CREDIT" ||
        it.creditLimit != null ||
        it.brand != null
    }

    // Contas Correntes Reais
    val checkingAccounts = connectedAccounts.filter {
        !it.accountType.contains("Cartão", ignoreCase = true) &&
        !it.accountType.contains("Crédito", ignoreCase = true) &&
        it.accountType != "CREDIT"
    }

    val totalCheckingBalance = checkingAccounts.sumOf { it.balance }

    // Limites Reais
    val primaryCard = creditCards.firstOrNull()
    val totalLimit = if (creditCards.isNotEmpty()) creditCards.sumOf { it.creditLimit ?: 0.0 } else 6400.00
    val totalAvailable = if (creditCards.isNotEmpty()) creditCards.sumOf { it.availableCredit ?: 0.0 } else 640.68
    val totalOpenBill = if (creditCards.isNotEmpty()) creditCards.sumOf { it.balance } else 5759.32
    val limitRatio = if (totalLimit > 0) ((totalLimit - totalAvailable) / totalLimit).coerceIn(0.0, 1.0).toFloat() else 0.90f

    // Estado do Diálogo de Meta com Consultor
    var showNewTargetDialog by remember { mutableStateOf(false) }
    var targetNameInput by remember { mutableStateOf("Compras no Cartão de Crédito") }
    var targetLimitInput by remember { mutableStateOf("1500") }
    var selectedAgentId by remember { mutableStateOf(availableAgents.firstOrNull()?.id ?: "guardiao_cartao") }
    var selectedAgentName by remember { mutableStateOf(availableAgents.firstOrNull()?.name ?: "Eduardo • Consultor de Crédito") }

    var isSyncingNow by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Cartões & Faturas",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            "Open Finance BACEN • Gestão Patrimonial",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color(0xFF0F172A))
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (remainingManualSyncs > 0) Color(0xFFF1F5F9) else Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable {
                                if (isSyncingNow) return@clickable
                                isSyncingNow = true
                                onManualSync { allowed, msg ->
                                    isSyncingNow = false
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSyncingNow) {
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8FAFC))
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isSyncingNow,
            onRefresh = {
                if (!isSyncingNow) {
                    isSyncingNow = true
                    onManualSync { allowed, msg ->
                        isSyncingNow = false
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp)
            ) {
            // 1. CARTÃO FÍSICO ULTRA-PREMIUM (ESTILO MASTERCARD BLACK / NUBANK ULTRAVIOLETA)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .shadow(12.dp, RoundedCornerShape(22.dp), spotColor = Color(0x35000000)),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF1E2128),
                                        Color(0xFF121418),
                                        Color(0xFF232730)
                                    )
                                )
                            )
                            .border(1.dp, Color(0xFF383F4D), RoundedCornerShape(22.dp))
                            .padding(22.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Topo do Cartão: Banco + Chip + Contactless
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Chip EMV metálico estilizado
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp, 26.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                brush = Brush.linearGradient(
                                                    listOf(Color(0xFFD4AF37), Color(0xFFF5D77F), Color(0xFFAA820A))
                                                )
                                            )
                                            .border(0.5.dp, Color(0xFF6B5300), RoundedCornerShape(6.dp))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    // Ícone Contactless (Pagamento por aproximação)
                                    Text(
                                        text = "))))",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-1).sp
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Mercado Pago",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            // Meio do Cartão: Número Mascarado em Tipografia Monospaçada
                            Column {
                                Text(
                                    text = "••••   ••••   ••••   0861",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFE2E8F0),
                                    letterSpacing = 3.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "FECHAMENTO 28/09  •  VENCE 07/10",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF94A3B8),
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(
                                        text = "MELHOR DIA 29/09",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF38BDF8),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            // Rodapé do Cartão: Titular + Selo Mastercard Black
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "TITULAR DO CARTÃO",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF64748B),
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(
                                        text = "GLEYSON T. MIRANDA",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF0F172A),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF475569))
                                ) {
                                    Text(
                                        text = "mastercard black",
                                        color = Color(0xFFF1F5F9),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. PAINEL DE CONTROLE DE LIMITES & FLUXO DE CAIXA LÍQUIDO
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
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
                                    color = Color(0xFF64748B),
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatBr(totalOpenBill),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFEF2F2),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFEE2E2))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Vence em 07/10",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE11D48)
                                    )
                                }
                            }
                        }

                        // Barra de Limite Real
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Limite Utilizado: ${(limitRatio * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "Disponível: ${formatBr(totalAvailable)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669)
                                )
                            }

                            LinearProgressIndicator(
                                progress = { limitRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (limitRatio > 0.85f) Color(0xFFE11D48) else if (limitRatio > 0.60f) Color(0xFFF59E0B) else Color(0xFF2563EB),
                                trackColor = Color(0xFFF1F5F9)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Limite Contratado: ${formatBr(totalLimit)}", fontSize = 10.sp, color = Color(0xFF64748B))
                                Text("Pagamento Mínimo: ${formatBr(primaryCard?.minimumPayment ?: 575.93)}", fontSize = 10.sp, color = Color(0xFF64748B))
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // Análise de Fluxo de Caixa (Supera o Pierre)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Saldo Disponível em Conta", fontSize = 10.sp, color = Color(0xFF64748B))
                                Text(formatBr(totalCheckingBalance), fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Melhor compra: a partir de 29/09",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF334155)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. SEÇÃO: METAS & TETOS COM O CONSULTOR FINANCEIRO (SEM CARA DE IA)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Metas & Limites de Gastos",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Acompanhamento e alertas em 80% e 100%",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    TextButton(onClick = { showNewTargetDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Criar Teto", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            items(agentTargets) { target ->
                val targetRatio = if (target.limitAmount > 0) (target.currentSpent / target.limitAmount).coerceIn(0.0, 1.0).toFloat() else 0f
                val isWarning = target.currentSpent >= target.limitAmount * 0.8 && target.currentSpent < target.limitAmount
                val isExceeded = target.currentSpent >= target.limitAmount

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isExceeded) Color(0xFFFECDD3) else if (isWarning) Color(0xFFFDE68A) else Color(0xFFE2E8F0)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = target.targetName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Responsável: ${target.agentName}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            IconButton(onClick = { onDeleteTarget(target.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            }
                        }

                        // Progresso
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Gasto Atual: ${formatBr(target.currentSpent)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isExceeded) Color(0xFFE11D48) else Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Teto: ${formatBr(target.limitAmount)}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            LinearProgressIndicator(
                                progress = { targetRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (isExceeded) Color(0xFFE11D48) else if (isWarning) Color(0xFFF59E0B) else Color(0xFF059669),
                                trackColor = Color(0xFFF1F5F9)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isExceeded) Color(0xFFFFF1F2) else if (isWarning) Color(0xFFFFFBEB) else Color(0xFFF0FDF4)
                        ) {
                            Text(
                                text = when {
                                    isExceeded -> "Limite atingido. O consultor emitiu recomendação de contenção."
                                    isWarning -> "Mais de 80% consumido. Monitoramento ativo."
                                    else -> "Dentro do planejado para este mês."
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    isExceeded -> Color(0xFFBE123C)
                                    isWarning -> Color(0xFFB45309)
                                    else -> Color(0xFF047857)
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // 4. SEÇÃO: LANÇAMENTOS REAIS DA FATURA ATUAL (SETEMBRO/2026)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lançamentos na Fatura Aberta",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "${pendingTransactions.size} compras",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(pendingTransactions) { tx ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
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
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFF1F5F9)
                                    ) {
                                        Text(
                                            text = "Parcela ${tx.installmentNumber} de ${tx.totalInstallments}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF475569),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
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
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "A vencer",
                                fontSize = 10.sp,
                                color = Color(0xFFE11D48),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 5. SEÇÃO: PARCELAS FUTURAS MÊS A MÊS (PROJEÇÃO TEMPORAL)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Parcelas Futuras (Compromissos a Vencer)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "${futureInstallments.size} parcelas",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            val groupedByForecast = futureInstallments.groupBy { it.billForecastDate ?: "Futuro" }
            groupedByForecast.forEach { (forecastMonth, txList) ->
                val monthTotal = txList.sumOf { it.amount }
                val formattedTitle = when {
                    forecastMonth.contains("-11") -> "Fatura de Novembro / 2026"
                    forecastMonth.contains("-12") -> "Fatura de Dezembro / 2026"
                    forecastMonth.contains("-10") -> "Fatura de Outubro / 2026"
                    else -> "Previsão: $forecastMonth"
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = formattedTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
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
                                        Text(itemTx.description, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                                        if (itemTx.installmentNumber != null && itemTx.totalInstallments != null) {
                                            Text(
                                                text = "Parcela ${itemTx.installmentNumber} de ${itemTx.totalInstallments}",
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                    Text(
                                        text = formatBr(itemTx.amount),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. SEÇÃO: HISTÓRICO DE FATURAS FECHADAS (COMPROVADAS COMO PAGAS)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Histórico de Faturas Fechadas",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF0F172A)
                )
            }

            items(bills) { bill ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Vencimento: ${bill.dueDate}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            if (!bill.closingDate.isNullOrBlank()) {
                                Text(
                                    text = "Fechamento: ${bill.closingDate}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
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
                                color = if (bill.isPaid) Color(0xFFECFDF5) else Color(0xFFFFFBEB)
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
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

    // DIÁLOGO PARA DEFINIÇÃO DE TETO COM O CONSULTOR
    if (showNewTargetDialog) {
        Dialog(onDismissRequest = { showNewTargetDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Definir Limite de Gastos",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF0F172A)
                    )

                    Text(
                        text = "Escolha o que deseja acompanhar e o valor máximo. Você receberá um aviso preventivo em 80% e quando atingir 100%.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )

                    OutlinedTextField(
                        value = targetNameInput,
                        onValueChange = { targetNameInput = it },
                        label = { Text("O que acompanhar?") },
                        placeholder = { Text("Ex: Cartão de Crédito, Supermercado...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = targetLimitInput,
                        onValueChange = { targetLimitInput = it },
                        label = { Text("Limite Máximo (R$)") },
                        placeholder = { Text("1500") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text("Consultor Responsável:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        availableAgents.take(3).forEach { agent ->
                            val isSelected = selectedAgentId == agent.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedAgentId = agent.id
                                        selectedAgentName = agent.name
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFFF1F5F9) else Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            selectedAgentId = agent.id
                                            selectedAgentName = agent.name
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(text = agent.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                        Text(text = agent.role, fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showNewTargetDialog = false }) {
                            Text("Cancelar", color = Color(0xFF64748B))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val limitVal = targetLimitInput.toDoubleOrNull() ?: 1500.0
                                val newTarget = AgentSpendingTarget(
                                    id = UUID.randomUUID().toString(),
                                    agentId = selectedAgentId,
                                    agentName = selectedAgentName,
                                    targetType = if (targetNameInput.contains("Cartão", ignoreCase = true)) "CARTAO_CREDITO" else "CATEGORIA",
                                    targetName = targetNameInput.trim().ifEmpty { "Compras no Cartão" },
                                    limitAmount = limitVal
                                )
                                onSaveTarget(newTarget)
                                showNewTargetDialog = false
                                Toast.makeText(context, "Meta ativada! ${newTarget.agentName} está monitorando.", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Salvar Meta", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
