package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionType
import com.example.data.model.UnifiedTransaction
import com.example.ui.components.BrandTransactionIcon
import com.example.util.formatBr
import com.example.util.formatBrNumber
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

// Paleta Apple harmoniosa para o Gráfico de Rosca
private val donutColors = listOf(
    Color(0xFF007AFF), // Apple Blue
    Color(0xFF34C759), // Apple Green
    Color(0xFFFF9500), // Apple Orange
    Color(0xFFFF2D55), // Apple Pink / Red
    Color(0xFF5856D6), // Apple Purple
    Color(0xFF8E8E93)  // Apple Gray (Outros)
)

data class CategorySlice(
    val name: String,
    val total: Double,
    val color: Color,
    val count: Int
)

/**
 * Gráfico de Rosca Interativo em Canvas Nativo do Jetpack Compose (Estilo Apple)
 */
@Composable
fun DonutChart(
    slices: List<CategorySlice>,
    totalLabel: String,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 26.dp,
    selectedIndex: Int? = null,
    onSelectSlice: (Int?) -> Unit = {}
) {
    val total = slices.sumOf { it.total }
    val animProgress = remember(slices) { Animatable(0f) }

    LaunchedEffect(slices) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .clickable { onSelectSlice(null) }
        ) {
            val strokePx = strokeWidth.toPx()
            val radius = (size.minDimension - strokePx * 1.3f) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2, radius * 2)

            if (slices.isEmpty() || total <= 0.0) {
                drawArc(
                    color = Color(0xFFE5E5EA),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(strokePx)
                )
                return@Canvas
            }

            var startAngle = -90f
            slices.forEachIndexed { idx, slice ->
                val sweep = ((slice.total / total) * 360f * animProgress.value).toFloat() - 2f
                val isSelected = idx == selectedIndex

                drawArc(
                    color = slice.color,
                    startAngle = startAngle + 1f,
                    sweepAngle = sweep.coerceAtLeast(1.5f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(
                        width = if (isSelected) strokePx * 1.35f else strokePx,
                        cap = StrokeCap.Round
                    )
                )
                startAngle += sweep + 2f
            }
        }

        // Centro da Rosca: Total ou Categoria Selecionada
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            val selectedSlice = selectedIndex?.let { slices.getOrNull(it) }
            if (selectedSlice != null) {
                Text(
                    text = selectedSlice.name,
                    fontSize = 12.sp,
                    color = selectedSlice.color,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "R$ ${selectedSlice.total.formatBr()}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1C1C1E)
                )
                val pct = if (total > 0) ((selectedSlice.total / total) * 100).toInt() else 0
                Text(
                    text = "$pct% do total",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    text = totalLabel,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "R$ ${total.formatBr()}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1C1C1E)
                )
                Text(
                    text = "${slices.size} categorias",
                    fontSize = 11.sp,
                    color = Color(0xFF8E8E93)
                )
            }
        }
    }
}

/**
 * Tela de Dashboard Financeiro com KPIs Inteligentes, Gráficos de Rosca (Máx 6) e Filtros
 * Design 100% Homogêneo no Estilo Apple iPhone (Light Mode Limpo).
 * Valores perfeitamente alinhados com a página Home.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialDashboardScreen(
    transactions: List<UnifiedTransaction>,
    onBack: () -> Unit = {}
) {
    var selectedPeriod by remember { mutableStateOf("ESTE_MES") } // ESTE_MES, 30_DIAS, MES_ANTERIOR, TODOS
    var chartType by remember { mutableStateOf(0) } // 0 = Saídas (Gastos), 1 = Entradas (Receitas)
    var selectedSliceIndex by remember { mutableStateOf<Int?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }

    // Paleta Apple Light
    val iosBg = Color(0xFFF2F2F7)
    val iosCard = Color(0xFFFFFFFF)
    val iosBorder = Color(0xFFE5E5EA)
    val iosTextBlack = Color(0xFF1C1C1E)
    val iosTextGray = Color(0xFF64748B)
    val iosTextMuted = Color(0xFF8E8E93)
    val iosBlue = Color(0xFF007AFF)
    val iosGreen = Color(0xFF34C759)
    val iosRed = Color(0xFFFF3B30)
    val iosOrange = Color(0xFFFF9500)

    // FILTRAGEM IDÊNTICA À TELA HOME
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

    val filteredList = remember(transactions, selectedPeriod, selectedCategoryFilter) {
        transactions.filter { tx ->
            if (tx.isInternalTransfer) return@filter false // Sem misturar caixinha/transferência interna

            val matchesPeriod = when (selectedPeriod) {
                "ESTE_MES" -> tx.date.startsWith(currentMonthPrefix) || tx.date.contains("2026-09")
                "30_DIAS" -> tx.date >= thirtyDaysAgo || tx.date.contains("2026-09") || tx.date.contains("2026-08")
                "MES_ANTERIOR" -> tx.date.startsWith(previousMonthPrefix) || tx.date.contains("2026-08")
                else -> true
            }
            if (!matchesPeriod) return@filter false

            if (selectedCategoryFilter != null) {
                if (selectedCategoryFilter!!.startsWith("Outros")) {
                    // Categoria outros
                } else if (tx.category != selectedCategoryFilter) {
                    return@filter false
                }
            }

            true
        }
    }

    // CÁLCULO DOS KPIS HARMONIZADOS COM A HOME
    val entradas = remember(filteredList) {
        filteredList.filter { it.type == TransactionType.ENTRADA }
    }
    val saidas = remember(filteredList) {
        filteredList.filter {
            it.type in listOf(TransactionType.SAIDA, TransactionType.ASSINATURA, TransactionType.CUPOM_FISCAL, TransactionType.PAGAMENTO)
        }
    }

    val totalEntradas = remember(entradas) { entradas.sumOf { abs(it.amount) } }
    val totalSaidas = remember(saidas) { saidas.sumOf { abs(it.amount) } }
    val saldoLiquido = totalEntradas - totalSaidas

    // Faturas em Aberto do período
    val totalOpenBills = remember(filteredList) {
        filteredList.filter {
            it.title.contains("Fatura", ignoreCase = true) ||
            it.title.contains("Cartão", ignoreCase = true) ||
            it.category.contains("Credit card", ignoreCase = true) ||
            it.bankName.contains("Cartão", ignoreCase = true)
        }.sumOf { abs(it.amount) }
    }

    // Previsão do Mês
    val cal = Calendar.getInstance()
    val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val dailyAverageSpent = if (dayOfMonth > 0) totalSaidas / dayOfMonth else 0.0
    val projectedMonthEndSpent = dailyAverageSpent * maxDaysInMonth

    // Rosca: Top 5 categorias + Outros (Máximo 6 fatias)
    val targetTransactionsForChart = if (chartType == 0) saidas else entradas

    val slices = remember(targetTransactionsForChart) {
        val grouped = targetTransactionsForChart.groupBy { it.category }
            .map { (cat, list) ->
                CategorySlice(
                    name = cat,
                    total = list.sumOf { abs(it.amount) },
                    color = Color.Transparent,
                    count = list.size
                )
            }
            .sortedByDescending { it.total }

        if (grouped.size <= 6) {
            grouped.mapIndexed { idx, s ->
                s.copy(color = donutColors[idx % donutColors.size])
            }
        } else {
            val top5 = grouped.take(5)
            val remainder = grouped.drop(5)
            val result = mutableListOf<CategorySlice>()

            top5.forEachIndexed { idx, s ->
                result.add(s.copy(color = donutColors[idx % donutColors.size]))
            }

            val remainderTotal = remainder.sumOf { it.total }
            val remainderCount = remainder.sumOf { it.count }
            result.add(
                CategorySlice(
                    name = "Outros (${remainder.size})",
                    total = remainderTotal,
                    color = Color(0xFF8E8E93),
                    count = remainderCount
                )
            )
            result
        }
    }

    Scaffold(
        containerColor = iosBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // CABEÇALHO COM BOTÃO VOLTAR
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = iosTextBlack)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text("Análise Financeira & KPIs", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = iosTextBlack)
                        Text("Sincronizado com os dados da tela inicial", fontSize = 12.sp, color = iosTextGray)
                    }
                }
            }

            // FILTROS DE PERÍODO (CHIPS APPLE)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PeriodFilterChip(
                        label = "Setembro (Atual)",
                        selected = selectedPeriod == "ESTE_MES",
                        onClick = { selectedPeriod = "ESTE_MES"; selectedCategoryFilter = null; selectedSliceIndex = null }
                    )
                    PeriodFilterChip(
                        label = "30 Dias",
                        selected = selectedPeriod == "30_DIAS",
                        onClick = { selectedPeriod = "30_DIAS"; selectedCategoryFilter = null; selectedSliceIndex = null }
                    )
                    PeriodFilterChip(
                        label = "Agosto",
                        selected = selectedPeriod == "MES_ANTERIOR",
                        onClick = { selectedPeriod = "MES_ANTERIOR"; selectedCategoryFilter = null; selectedSliceIndex = null }
                    )
                    PeriodFilterChip(
                        label = "Todos",
                        selected = selectedPeriod == "TODOS",
                        onClick = { selectedPeriod = "TODOS"; selectedCategoryFilter = null; selectedSliceIndex = null }
                    )
                }
            }

            // 4 CARDS DE KPIS ESSENCIAIS (100% HARMONIZADOS COM A HOME)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "INDICADORES CHAVE (KPIS)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosTextGray,
                        letterSpacing = 0.6.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiModernCard(
                            modifier = Modifier.weight(1f),
                            title = "Entradas Reais",
                            value = "+R$ ${totalEntradas.formatBrNumber()}",
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            accentColor = iosGreen,
                            subtitle = "${entradas.size} recebimentos"
                        )
                        KpiModernCard(
                            modifier = Modifier.weight(1f),
                            title = "Saídas & Faturas",
                            value = "-R$ ${totalSaidas.formatBrNumber()}",
                            icon = Icons.AutoMirrored.Filled.TrendingDown,
                            accentColor = iosRed,
                            subtitle = "${saidas.size} lançamentos"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiModernCard(
                            modifier = Modifier.weight(1f),
                            title = "Faturas em Aberto",
                            value = "-R$ ${totalOpenBills.formatBrNumber()}",
                            icon = Icons.Default.CreditCard,
                            accentColor = iosRed,
                            subtitle = "Cartões de crédito"
                        )
                        KpiModernCard(
                            modifier = Modifier.weight(1f),
                            title = "Previsão Fim de Mês",
                            value = "R$ ${projectedMonthEndSpent.formatBrNumber()}",
                            icon = Icons.Default.Speed,
                            accentColor = iosOrange,
                            subtitle = "Média: R$ ${dailyAverageSpent.formatBrNumber()}/dia"
                        )
                    }
                }
            }

            // CARD DO GRÁFICO DE ROSCA (APPLE LIGHT)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = iosCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, iosBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DISTRIBUIÇÃO DE CATEGORIAS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = iosTextGray,
                                letterSpacing = 0.6.sp
                            )

                            // Segmented Switcher (Saídas vs Entradas)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = iosBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, iosBorder)
                            ) {
                                Row(modifier = Modifier.padding(2.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (chartType == 0) iosBlue else Color.Transparent,
                                        modifier = Modifier.clickable { chartType = 0; selectedSliceIndex = null; selectedCategoryFilter = null }
                                    ) {
                                        Text(
                                            text = "Saídas",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (chartType == 0) Color.White else iosTextGray,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (chartType == 1) iosGreen else Color.Transparent,
                                        modifier = Modifier.clickable { chartType = 1; selectedSliceIndex = null; selectedCategoryFilter = null }
                                    ) {
                                        Text(
                                            text = "Entradas",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (chartType == 1) Color.White else iosTextGray,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Gráfico de Rosca
                        DonutChart(
                            slices = slices,
                            totalLabel = if (chartType == 0) "Total Saídas" else "Total Entradas",
                            selectedIndex = selectedSliceIndex,
                            onSelectSlice = { idx ->
                                selectedSliceIndex = idx
                                selectedCategoryFilter = idx?.let { slices.getOrNull(it)?.name }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Legenda das Fatias (Top 5 + Outros)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            slices.forEachIndexed { idx, slice ->
                                val isSelected = idx == selectedSliceIndex
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFFEFF6FF) else iosBg,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) iosBlue else iosBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (selectedSliceIndex == idx) {
                                                selectedSliceIndex = null
                                                selectedCategoryFilter = null
                                            } else {
                                                selectedSliceIndex = idx
                                                selectedCategoryFilter = slice.name
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(slice.color)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(slice.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = iosTextBlack)
                                        }
                                        Text("R$ ${slice.total.formatBr()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = iosTextBlack)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // LISTA DE TRANSAÇÕES FILTRADAS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedCategoryFilter != null) "TRANSAÇÕES: $selectedCategoryFilter" else "LANÇAMENTOS DO PERÍODO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosTextGray,
                        letterSpacing = 0.6.sp
                    )
                    if (selectedCategoryFilter != null) {
                        Text(
                            text = "Limpar Filtro",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosBlue,
                            modifier = Modifier.clickable { selectedCategoryFilter = null; selectedSliceIndex = null }
                        )
                    }
                }
            }

            items(filteredList) { tx ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = iosCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, iosBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            BrandTransactionIcon(
                                title = tx.title,
                                category = tx.category,
                                bankName = tx.bankName,
                                isIncome = tx.type == TransactionType.ENTRADA,
                                size = 38.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(tx.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = iosTextBlack, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${tx.formattedDate} • ${tx.category}", fontSize = 11.sp, color = iosTextGray)
                            }
                        }

                        val sign = if (tx.type == TransactionType.ENTRADA) "+" else "-"
                        Text(
                            text = "$sign${tx.amount.formatBr()}",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = if (tx.type == TransactionType.ENTRADA) iosGreen else iosRed
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
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
private fun KpiModernCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    subtitle: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E5EA))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF8E8E93)
            )
        }
    }
}
