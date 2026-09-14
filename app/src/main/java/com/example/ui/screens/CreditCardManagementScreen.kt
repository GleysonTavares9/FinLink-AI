package com.example.ui.screens

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
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

    // Filtra cartões de crédito
    val creditCards = connectedAccounts.filter {
        it.accountType.contains("Cartão", ignoreCase = true) ||
        it.accountType.contains("Crédito", ignoreCase = true) ||
        it.accountType == "CREDIT" ||
        it.creditLimit != null ||
        it.brand != null
    }

    // Totais de Limite
    val totalLimit = creditCards.sumOf { it.creditLimit ?: 0.0 }
    val totalAvailable = creditCards.sumOf { it.availableCredit ?: 0.0 }
    val totalOpenBill = creditCards.sumOf { it.balance }
    val limitRatio = if (totalLimit > 0) ((totalLimit - totalAvailable) / totalLimit).coerceIn(0.0, 1.0).toFloat() else 0f

    // Estado do Diálogo de Nova Meta com Agente Educador
    var showNewTargetDialog by remember { mutableStateOf(false) }
    var targetNameInput by remember { mutableStateOf("Compras no Cartão de Crédito") }
    var targetLimitInput by remember { mutableStateOf("1500") }
    var selectedAgentId by remember { mutableStateOf(availableAgents.firstOrNull()?.id ?: "guardiao_cartao") }
    var selectedAgentName by remember { mutableStateOf(availableAgents.firstOrNull()?.name ?: "Eduardo • Guardião do Cartão") }

    var isSyncingNow by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Gestão de Cartões & Faturas",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        }
                        Text(
                            "Open Finance BACEN • Limites, Faturas & Parcelas",
                            fontSize = 11.sp,
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
                    // Botão de Sincronização Manual com cota
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (remainingManualSyncs > 0) Color(0xFF4F46E5).copy(alpha = 0.12f) else Color(0xFF94A3B8).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "$remainingManualSyncs/2 hoje",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (remainingManualSyncs > 0) Color(0xFF4F46E5) else Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (isSyncingNow) return@IconButton
                                isSyncingNow = true
                                onManualSync { allowed, msg ->
                                    isSyncingNow = false
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        ) {
                            if (isSyncingNow) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Atualizar Agora", tint = Color(0xFF4F46E5))
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 100.dp)
        ) {
            // 1. CARD PRINCIPAL CONSOLIDADO DE LIMITES & FATURA EM ABERTO
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(10.dp, RoundedCornerShape(22.dp), spotColor = Color(0x30000000)),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF312E81))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF6366F1).copy(alpha = 0.25f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color(0xFFA5B4FC), modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "${creditCards.size} Cartão(ões)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFA5B4FC)
                                        )
                                    }
                                }
                            }

                            // Barra visual de limite
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Comprometido: ${(limitRatio * 100).toInt()}%", fontSize = 11.sp, color = Color(0xFFCBD5E1))
                                    Text("Disponível: ${formatBr(totalAvailable)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                                }

                                LinearProgressIndicator(
                                    progress = { limitRatio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(9.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    color = if (limitRatio > 0.85f) Color(0xFFEF4444) else if (limitRatio > 0.60f) Color(0xFFF59E0B) else Color(0xFF6366F1),
                                    trackColor = Color(0xFF334155)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Limite Total: ${formatBr(totalLimit)}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("BACEN Open Finance v2", fontSize = 10.sp, color = Color(0xFFA5B4FC))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. SEÇÃO: METAS & LIMITES DEFINIDOS PELO USUÁRIO COM AGENTES EDUCADORES
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Limites dos Agentes Educadores",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Text(
                            text = "Alertas ao aproximar (80%) e ao ultrapassar",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TextButton(onClick = { showNewTargetDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Novo Limite", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            if (agentTargets.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Nenhum limite personalizado ativo",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                "Você pode definir limites customizados (ex: Compras no Cartão até R$ 1.500) e escolher qual agente educador acompanhará e enviará notificações.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { showNewTargetDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Definir Limite com Agente", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isExceeded) Color(0xFFEF4444) else if (isWarning) Color(0xFFF59E0B) else Color(0xFFE2E8F0)
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
                                    fontSize = 15.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Monitorado por: ${target.agentName}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF4F46E5),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            IconButton(onClick = { onDeleteTarget(target.id) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Excluir Meta", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                            }
                        }

                        // Progresso da meta
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Gasto Atual: ${formatBr(target.currentSpent)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isExceeded) Color(0xFFEF4444) else Color(0xFF334155)
                                )
                                Text(
                                    text = "Teto Definido: ${formatBr(target.limitAmount)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF64748B)
                                )
                            }

                            LinearProgressIndicator(
                                progress = { targetRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (isExceeded) Color(0xFFEF4444) else if (isWarning) Color(0xFFF59E0B) else Color(0xFF10B981),
                                trackColor = Color(0xFFF1F5F9)
                            )
                        }

                        // Status educativo
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isExceeded) Color(0xFFFEE2E2) else if (isWarning) Color(0xFFFEF3C7) else Color(0xFFECFDF5)
                        ) {
                            Text(
                                text = when {
                                    isExceeded -> "🚨 Teto ultrapassado! O agente já disparou alerta para readequação."
                                    isWarning -> "⚠️ Atenção: Mais de 80% do limite consumido. Agente monitorando."
                                    else -> "✅ Dentro da meta planejada com segurança financeira."
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    isExceeded -> Color(0xFFB91C1C)
                                    isWarning -> Color(0xFFB45309)
                                    else -> Color(0xFF047857)
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // 3. SEÇÃO: CARTÕES DE CRÉDITO CONECTADOS
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cartões Vinculados",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${creditCards.size} ativos",
                        fontSize = 12.sp,
                        color = Color(0xFF4F46E5),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (creditCards.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Nenhum cartão cadastrado", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Vincule sua instituição bancária no Open Finance para importar faturas e parcelas automaticamente.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            Button(onClick = onNavigateToOpenFinanceConnect, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)), shape = RoundedCornerShape(10.dp)) {
                                Text("Conectar Banco / Cartão", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            items(creditCards) { card ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                BankLogoView(bankName = card.bankName, size = 42.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = card.bankName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(
                                        text = "${card.brand ?: "CARTÃO"} ${card.level ?: ""} • final ${card.accountNumber.takeLast(4)}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF4F46E5).copy(alpha = 0.12f)) {
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Fatura em Aberto", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = formatBr(card.balance), fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Ciclo da Fatura", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "Fecha ${card.closeDate ?: "--"} • Vence ${card.dueDate ?: "--"}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF334155)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Limite Total:", fontSize = 10.sp, color = Color(0xFF64748B))
                                    Text(formatBr(card.creditLimit ?: 0.0), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Disponível:", fontSize = 10.sp, color = Color(0xFF64748B))
                                    Text(formatBr(card.availableCredit ?: 0.0), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Pag. Mínimo:", fontSize = 10.sp, color = Color(0xFF64748B))
                                    Text(formatBr(card.minimumPayment ?: 0.0), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                                }
                            }
                        }
                    }
                }
            }

            // 4. SEÇÃO: LANÇAMENTOS DA FATURA ATUAL (COMPRAS PENDENTES / A VENCER)
            item {
                Spacer(modifier = Modifier.height(4.dp))
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
                        Text(
                            text = "Nenhuma compra pendente não faturada no momento.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(14.dp)
                        )
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
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = tx.description, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = tx.date, fontSize = 11.sp, color = Color(0xFF64748B))
                                if (tx.installmentNumber != null && tx.totalInstallments != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF4F46E5).copy(alpha = 0.1f)) {
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
                            Text(text = formatBr(tx.amount), fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFFEF4444))
                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFF59E0B).copy(alpha = 0.12f)) {
                                Text(text = "A vencer", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFD97706), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
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
                        text = "Parcelas Futuras a Vencer",
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
                        Text(
                            text = "Nenhum parcelamento futuro identificado.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
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
                                    Text(text = formatBr(monthTotal), fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF0F172A))
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
                                        Text(text = formatBr(itemTx.amount), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. SEÇÃO: HISTÓRICO DE FATURAS FECHADAS
            item {
                Spacer(modifier = Modifier.height(4.dp))
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
                        Text(
                            text = "Nenhuma fatura fechada anterior encontrada.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(14.dp)
                        )
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
                                Text(text = "Vencimento: ${bill.dueDate}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            if (!bill.closingDate.isNullOrBlank()) {
                                Text(text = "Fechamento: ${bill.closingDate}", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = formatBr(bill.totalAmount), fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                            Spacer(modifier = Modifier.height(3.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (bill.isPaid) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
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

    // DIÁLOGO PARA O USUÁRIO DEFINIR LIMITE E ESCOLHER O AGENTE
    if (showNewTargetDialog) {
        Dialog(onDismissRequest = { showNewTargetDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Definir Limite com Agente Educador",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )

                    Text(
                        text = "Escolha o agente mais adequado e defina o valor máximo que você deseja monitorar.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 1. O que você quer acompanhar?
                    OutlinedTextField(
                        value = targetNameInput,
                        onValueChange = { targetNameInput = it },
                        label = { Text("O que deseja acompanhar?") },
                        placeholder = { Text("Ex: Compras no Cartão, Supermercado...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // 2. Valor limite
                    OutlinedTextField(
                        value = targetLimitInput,
                        onValueChange = { targetLimitInput = it },
                        label = { Text("Limite Máximo (R$)") },
                        placeholder = { Text("1500") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // 3. Escolher Agente
                    Text("Selecione o Agente Educador:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        availableAgents.take(4).forEach { agent ->
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
                                color = if (isSelected) Color(0xFF4F46E5).copy(alpha = 0.1f) else Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0))
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
                                        Text(text = agent.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                            Text("Cancelar")
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
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
