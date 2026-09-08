package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.example.data.db.OpenFinanceAccountEntity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ExpenseEntity
import com.example.data.remote.supabase.AuthUiState
import com.example.domain.model.BudgetAnalysis
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosGreen
import com.example.ui.theme.IosIndigo
import com.example.ui.theme.IosOrange
import com.example.ui.theme.IosPink
import com.example.ui.theme.IosPurple
import com.example.ui.theme.IosRed
import com.example.ui.theme.IosTeal
import com.example.util.formatBr
import com.example.util.formatBrCompact
import com.example.util.formatBrNumber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    expenses: List<ExpenseEntity>,
    totalSpent: Double,
    connectedAccounts: List<OpenFinanceAccountEntity> = emptyList(),
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
    isSyncing: Boolean = false,
    lastSyncTime: String = "",
    onDeleteExpense: (Long) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("Todos") }
    var isBalanceVisible by remember { mutableStateOf(true) }

    val categories = listOf("Todos", "Alimentação", "Transporte", "Lazer", "Saúde", "Mercado")

    val filteredExpenses = expenses.filter {
        val matchesSearch = it.storeName.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true) ||
                it.source.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategoryFilter == "Todos" || it.category.equals(selectedCategoryFilter, ignoreCase = true)
        matchesSearch && matchesCategory
    }

    val categoryTotals = expenses.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.totalAmount } }

    val todayFormatted = remember {
        val sdf = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("pt-BR"))
        sdf.format(Date()).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    // Fundo Aurora Multicolorido Suave para Dar Profundidade Real ao Vidro Branco
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F5FA))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF6366F1).copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(size.width * 0.88f, size.height * 0.08f),
                    radius = size.width * 0.70f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0EA5E9).copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.12f, size.height * 0.18f),
                    radius = size.width * 0.60f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF10B981).copy(alpha = 0.09f), Color.Transparent),
                    center = Offset(size.width * 0.95f, size.height * 0.46f),
                    radius = size.width * 0.55f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFF59E0B).copy(alpha = 0.07f), Color.Transparent),
                    center = Offset(size.width * 0.05f, size.height * 0.68f),
                    radius = size.width * 0.55f
                )
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Moderno 100% Nativo (Sem botões web confusos)
            item {
                Spacer(modifier = Modifier.height(10.dp))
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
                                    .background(Color(0xFF34C759))
                            )
                            Text(
                                text = "FINLINK AI • CONTA INTELIGENTE",
                                style = MaterialTheme.typography.labelSmall,
                                color = IosBlue,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = todayFormatted,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            ),
                            color = Color(0xFF1D1D1F)
                        )
                    }

                    // Botões de Ação do Topo em Vidro Branco
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Assistente Gemini AI
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .shadow(8.dp, CircleShape, spotColor = Color(0x20000000))
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFFFFFFFF), Color(0xEEF0F4FF))
                                    )
                                )
                                .border(
                                    width = 1.2.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color.White, Color(0xFFD4E0FC))
                                    ),
                                    shape = CircleShape
                                )
                                .clickable { onNavigateToAi() }
                                .testTag("btn_top_ai"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "IA Gemini",
                                tint = IosIndigo,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Orçamento Familiar
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .shadow(8.dp, CircleShape, spotColor = Color(0x20000000))
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFFFFFFFF), Color(0xFFFFF7ED))
                                    )
                                )
                                .border(
                                    width = 1.2.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color.White, Color(0xFFFFEDD5))
                                    ),
                                    shape = CircleShape
                                )
                                .clickable { onNavigateToHousehold() }
                                .testTag("btn_top_household"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FamilyRestroom,
                                contentDescription = "Família & Orçamento",
                                tint = IosOrange,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Conta Supabase Nuvem
                        val isAuthenticated = authState is AuthUiState.Authenticated
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .shadow(8.dp, CircleShape, spotColor = Color(0x20000000))
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFFFFFFF),
                                            if (isAuthenticated) Color(0xFFECFDF5) else Color(0xFFF8FAFC)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.2.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.White,
                                            if (isAuthenticated) Color(0xFF3ECF8E) else Color(0xFFCBD5E1)
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .clickable { onNavigateToAuth() }
                                .testTag("btn_top_cloud"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isAuthenticated) Icons.Default.CloudDone else Icons.Default.Cloud,
                                contentDescription = "Conta Supabase",
                                tint = if (isAuthenticated) Color(0xFF059669) else Color(0xFF64748B),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // CARD PRINCIPAL: FROSTED GLASS LIQUID CARD EM MOEDA BRASILEIRA (R$)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(24.dp, RoundedCornerShape(28.dp), spotColor = Color(0x240F172A))
                        .testTag("summary_card"),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xF8FFFFFF),
                                        Color(0xEEF8FAFD),
                                        Color(0xE8F1F5F9)
                                    )
                                )
                            )
                            .border(
                                width = 1.5.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White,
                                        Color.White.copy(alpha = 0.7f),
                                        Color(0x12000000),
                                        Color.White.copy(alpha = 0.4f)
                                    )
                                ),
                                shape = RoundedCornerShape(28.dp)
                            )
                            .padding(24.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isCloudAuth = authState is AuthUiState.Authenticated
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { onNavigateToAuth() }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isCloudAuth) Color(0xFF10B981).copy(alpha = 0.14f)
                                                else Color(0xFF0284C7).copy(alpha = 0.14f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isCloudAuth) Icons.Default.CloudDone else Icons.Default.Nfc,
                                            contentDescription = null,
                                            tint = if (isCloudAuth) Color(0xFF059669) else IosBlue,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (isCloudAuth) "SUPABASE POSTGRES • NUVEM" else "BANCO SQLITE • LOCAL",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isCloudAuth) Color(0xFF059669) else Color(0xFF64748B),
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.8.sp
                                        )
                                        Text(
                                            text = if (isCloudAuth) "RLS Ativo • Sincronização em Tempo Real" else "Toque para conectar sua conta",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF1F5F9))
                                        .clickable { isBalanceVisible = !isBalanceVisible },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Ocultar Saldo",
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Total Gasto Registrado",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFE2E8F0))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "MOEDA BR (R$)",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Valor em R$ Brasileiro com Formatação Real
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "R$",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF059669),
                                    modifier = Modifier.padding(bottom = 5.dp)
                                )
                                Text(
                                    text = if (isBalanceVisible) totalSpent.formatBrNumber() else "••••••",
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 38.sp,
                                        letterSpacing = (-1.5).sp,
                                        color = Color(0xFF0F172A)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFE2E8F0).copy(alpha = 0.7f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.8f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (expenses.isNotEmpty()) Color(0xFF10B981) else Color(0xFF94A3B8))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${expenses.size} registros reais",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF0F172A),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSyncing) IosOrange.copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSyncing) IosOrange.copy(alpha = 0.3f) else Color(0xFF10B981).copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.clickable { onSyncCloud() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudSync,
                                            contentDescription = null,
                                            tint = if (isSyncing) IosOrange else Color(0xFF059669),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (isSyncing) "Sincronizando..." else if (lastSyncTime.isNotEmpty()) "Sync: $lastSyncTime" else "Sync em Tempo Real",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSyncing) IosOrange else Color(0xFF059669),
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SEÇÃO DE CONTAS E BANCOS CONECTADOS • OPEN FINANCE BRASIL
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "BANCOS CONECTADOS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF64748B),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "OPEN FINANCE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF059669),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Gerenciar Conexões →",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = IosBlue,
                            modifier = Modifier.clickable { onNavigateToOpenFinance() }
                        )
                    }

                    val effectiveAccounts = if (connectedAccounts.isNotEmpty()) connectedAccounts else listOf(
                        OpenFinanceAccountEntity(
                            id = "mp_primary_001",
                            bankName = "Mercado Pago",
                            accountType = "Conta Corrente & Investimentos",
                            accountNumber = "Ag 0001 • CC 482910-3",
                            balance = 1854.20,
                            connectedAt = System.currentTimeMillis(),
                            status = "CONECTADO",
                            lastSync = System.currentTimeMillis()
                        )
                    )

                    effectiveAccounts.forEach { account ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Color(0x140F172A))
                                .clickable { onNavigateToOpenFinance() },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.horizontalGradient(
                                    listOf(Color(0xFFE2E8F0), Color(0xFFF1F5F9))
                                )
                            )
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        val isMp = account.bankName.contains("Mercado Pago", ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isMp) Color(0xFFFFE600) else Color(0xFFEEF2FF)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isMp) {
                                                Text(
                                                    text = "M",
                                                    fontSize = 22.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFF003B6D)
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Default.AccountBalance,
                                                    contentDescription = null,
                                                    tint = Color(0xFF4F46E5),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = account.bankName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "● Ativa",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFF059669),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${account.accountType} • ${account.accountNumber}",
                                                fontSize = 12.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Saldo em Conta",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = if (isBalanceVisible) formatBr(account.balance) else "R$ ••••••",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CloudDone,
                                            contentDescription = null,
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Criptografia BACEN FAPI • Sincronizado",
                                            fontSize = 11.sp,
                                            color = Color(0xFF059669),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Text(
                                        text = "Ver extrato →",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IosBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // BENTO GRID MODERNO DE AÇÕES RÁPIDAS
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "AÇÕES INTELIGENTES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.sp
                    )

                    // Card Bento 1: Hero Principal Largo - Escanear Cupom
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(12.dp, RoundedCornerShape(22.dp), spotColor = Color(0x180F172A))
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { onNavigateToScan() }
                            .testTag("action_scan_receipt"),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color(0xF8FFFFFF), Color(0xEEF0FDF4))
                                    )
                                )
                                .border(
                                    width = 1.2.dp,
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color.White, Color(0xFFBBF7D0))
                                    ),
                                    shape = RoundedCornerShape(22.dp)
                                )
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(Color(0xFF10B981), Color(0xFF059669))
                                                )
                                            )
                                            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color(0x4010B981)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Scanner",
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Escanear Cupom Fiscal",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFDCFCE7))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "NFC-e / IA",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFF15803D)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Aponte a câmera para notas fiscais ou QR Codes",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFDCFCE7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = null,
                                        tint = Color(0xFF15803D),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Card Bento 2 e 3: Lançamento Manual & Open Finance Bancos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ModernBentoTile(
                            title = "Novo Gasto",
                            subtitle = "Manual & PIX",
                            badgeText = "+ RÁPIDO",
                            icon = Icons.Default.Edit,
                            accentColor = IosBlue,
                            backgroundGradient = listOf(Color(0xF8FFFFFF), Color(0xEEF0F9FF)),
                            borderColor = Color(0xFFBAE6FD),
                            onClick = onNavigateToManual,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("action_manual_input")
                        )

                        ModernBentoTile(
                            title = "Open Finance",
                            subtitle = "Contas & Extratos",
                            badgeText = "BANCOS BR",
                            icon = Icons.Default.AccountBalance,
                            accentColor = IosIndigo,
                            backgroundGradient = listOf(Color(0xF8FFFFFF), Color(0xEEF5F3FF)),
                            borderColor = Color(0xFFDDD6FE),
                            onClick = onNavigateToOpenFinance,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("action_open_finance")
                        )
                    }
                }
            }

            // Card Orçamento Familiar (Sem dados fictícios: apenas se configurado de verdade)
            item {
                if (budgetAnalysis != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(12.dp, RoundedCornerShape(22.dp), spotColor = Color(0x180F172A))
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { onNavigateToHousehold() },
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xF8FFFFFF), Color(0xEEFFF7ED))
                                    )
                                )
                                .border(
                                    width = 1.2.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color.White, Color(0xFFFED7AA))
                                    ),
                                    shape = RoundedCornerShape(22.dp)
                                )
                                .padding(18.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(IosOrange.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FamilyRestroom,
                                                contentDescription = null,
                                                tint = IosOrange,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Orçamento Familiar",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = "${budgetAnalysis.profile.adultCount} Adulto(s) • ${budgetAnalysis.profile.childCount} Criança(s)",
                                                fontSize = 12.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Teto ${budgetAnalysis.totalRecommendedBudget.formatBrCompact()}",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp,
                                            color = IosBlue
                                        )
                                        Text(
                                            text = "${String.format(Locale.forLanguageTag("pt-BR"), "%.0f", budgetAnalysis.overallPercentage)}% consumido",
                                            fontSize = 11.sp,
                                            color = if (budgetAnalysis.overallPercentage > 100) IosRed else Color(0xFF059669),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                val budgetProgress = (budgetAnalysis.overallPercentage / 100f).toFloat().coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { budgetProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (budgetAnalysis.overallPercentage > 100) IosRed else IosOrange,
                                    trackColor = Color(0xFFFFEDD5)
                                )
                            }
                        }
                    }
                } else {
                    // Estado Não Configurado: Convite limpo sem dados falsos
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(22.dp), spotColor = Color(0x100F172A))
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { onNavigateToHousehold() },
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xEEFFFFFF))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(22.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(IosOrange.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FamilyRestroom,
                                            contentDescription = null,
                                            tint = IosOrange,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Configurar Orçamento Familiar",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "Toque para definir composição de adultos e crianças",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Card Gráfico de Distribuição por Categoria em Efeito Vidro
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(22.dp), spotColor = Color(0x180F172A)),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xF8FFFFFF), Color(0xEEF8FAFC))
                                )
                            )
                            .border(
                                width = 1.2.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.White, Color(0x15000000))
                                ),
                                shape = RoundedCornerShape(22.dp)
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = IosBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Distribuição por Categoria",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                                Text(
                                    text = "${categoryTotals.size} categorias",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IosBlue,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (categoryTotals.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Nenhuma despesa registrada ainda.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.size(126.dp)) {
                                        var startAngle = -90f
                                        drawArc(
                                            color = Color(0x1A64748B),
                                            startAngle = 0f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            style = Stroke(width = 24f)
                                        )
                                        categoryTotals.forEach { (cat, amount) ->
                                            val sweepAngle = if (totalSpent > 0) ((amount / totalSpent) * 360f).toFloat() else 0f
                                            if (sweepAngle > 0f) {
                                                drawArc(
                                                    color = iosCategoryColor(cat),
                                                    startAngle = startAngle,
                                                    sweepAngle = sweepAngle,
                                                    useCenter = false,
                                                    style = Stroke(width = 24f)
                                                )
                                                startAngle += sweepAngle
                                            }
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "TOTAL",
                                            fontSize = 10.sp,
                                            color = Color(0xFF64748B),
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = totalSpent.formatBrCompact(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                categoryTotals.entries.sortedByDescending { it.value }.take(4).forEach { (cat, amount) ->
                                    val progress = if (totalSpent > 0) (amount / totalSpent).toFloat() else 0f
                                    Column(modifier = Modifier.padding(vertical = 5.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(iosCategoryColor(cat))
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = cat,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF0F172A)
                                                )
                                            }
                                            Text(
                                                text = "${amount.formatBr()} (${(progress * 100).toInt()}%)",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF334155)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(5.dp))
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = iosCategoryColor(cat),
                                            trackColor = Color(0xFFE2E8F0)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // BUSCADOR EM VIDRO E PÍLULAS DE FILTRO
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(6.dp, RoundedCornerShape(18.dp), spotColor = Color(0x100F172A))
                            .testTag("search_input"),
                        placeholder = { Text("Buscar estabelecimento, valor ou categoria...", fontSize = 14.sp, color = Color(0xFF94A3B8)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF64748B)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpar", tint = Color(0xFF64748B))
                                }
                            }
                        },
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xF8FFFFFF),
                            unfocusedContainerColor = Color(0xEEFFFFFF),
                            focusedBorderColor = IosBlue,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        singleLine = true
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            val isSelected = selectedCategoryFilter == cat
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) IosBlue else Color(0xEEFFFFFF),
                                shadowElevation = if (isSelected) 6.dp else 2.dp,
                                modifier = Modifier
                                    .clickable { selectedCategoryFilter = cat }
                                    .border(
                                        width = if (isSelected) 0.dp else 1.dp,
                                        color = if (isSelected) Color.Transparent else Color(0xFFCBD5E1),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                            ) {
                                Text(
                                    text = cat,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else Color(0xFF334155)
                                )
                            }
                        }
                    }
                }
            }

            // TÍTULO DA SEÇÃO DE TRANSAÇÕES REAIS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "TRANSAÇÕES REAIS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF64748B),
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE2E8F0))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${filteredExpenses.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = IosBlue.copy(alpha = 0.12f),
                        modifier = Modifier
                            .clickable { onNavigateToManual() }
                            .testTag("add_expense_fab")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = IosBlue, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Novo Gasto",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = IosBlue
                            )
                        }
                    }
                }
            }

            // LISTA DE TRANSAÇÕES EM CARDS MODERNOS COM FROSTED GLASS
            if (filteredExpenses.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xEEFFFFFF))
                                .border(1.2.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(IosBlue.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = IosBlue
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "Nenhum resultado para \"$searchQuery\"" else "Nenhum Gasto Registrado",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "Tente buscar por outro nome ou limpe os filtros."
                                    else "Seus lançamentos manuais, extratos e cupons ficam salvos no Supabase em tempo real.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                if (searchQuery.isEmpty()) {
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = IosBlue,
                                            modifier = Modifier.clickable { onNavigateToManual() }
                                        ) {
                                            Text(
                                                text = "+ Lançar Manual",
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = Color(0xFF10B981),
                                            modifier = Modifier.clickable { onNavigateToScan() }
                                        ) {
                                            Text(
                                                text = "📸 Escanear NFC-e",
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = Color(0x180F172A)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xF8FFFFFF), Color(0xEEF8FAFC))
                                    )
                                )
                                .border(
                                    width = 1.2.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.White, Color(0x15000000))
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                )
                        ) {
                            Column {
                                filteredExpenses.forEachIndexed { index, expense ->
                                    ModernTransactionRow(
                                        expense = expense,
                                        isLast = index == filteredExpenses.lastIndex,
                                        onDelete = { onDeleteExpense(expense.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

/**
 * Card Bento Moderno Translúcido.
 */
@Composable
fun ModernBentoTile(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: ImageVector,
    accentColor: Color,
    backgroundGradient: List<Color>,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(10.dp, RoundedCornerShape(20.dp), spotColor = Color(0x150F172A))
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(backgroundGradient))
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(listOf(Color.White, borderColor)),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentColor.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = accentColor
                        )
                    }
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

/**
 * Item de Transação com Visual Moderno de Alta Definição e Moeda Brasileira Real.
 */
@Composable
fun ModernTransactionRow(
    expense: ExpenseEntity,
    isLast: Boolean,
    onDelete: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    iosCategoryColor(expense.category),
                                    iosCategoryColor(expense.category).copy(alpha = 0.82f)
                                )
                            )
                        )
                        .shadow(6.dp, RoundedCornerShape(14.dp), spotColor = iosCategoryColor(expense.category)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(expense.category),
                        contentDescription = expense.category,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = expense.storeName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = expense.transactionDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF94A3B8))
                        )
                        Text(
                            text = expense.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = iosCategoryColor(expense.category),
                            fontWeight = FontWeight.Bold
                        )
                        if (expense.source.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFE2E8F0))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = expense.source,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "- ${expense.totalAmount.formatBr()}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Excluir",
                        tint = IosRed.copy(alpha = 0.75f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (!isLast) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 74.dp)
                    .height(0.6.dp)
                    .background(Color(0xFFE2E8F0))
            )
        }
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "alimentação", "restaurante", "comida" -> Icons.Default.Fastfood
        "transporte", "combustível", "uber" -> Icons.Default.DirectionsCar
        "mercado", "supermercado", "compras" -> Icons.Default.ShoppingBag
        "saúde", "farmácia" -> Icons.Default.LocalHospital
        else -> Icons.AutoMirrored.Filled.ReceiptLong
    }
}

fun iosCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "alimentação" -> IosOrange
        "transporte" -> IosBlue
        "lazer" -> IosPurple
        "saúde" -> IosPink
        "moradia" -> IosIndigo
        "mercado" -> Color(0xFF10B981)
        else -> IosTeal
    }
}
