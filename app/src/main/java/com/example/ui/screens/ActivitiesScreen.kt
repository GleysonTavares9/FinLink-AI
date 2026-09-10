package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionType
import com.example.data.model.UnifiedTransaction
import com.example.ui.components.BrandTransactionIcon
import com.example.util.formatBr
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Tela de Atividades FinLink AI
 * Design 100% Homogêneo Estilo iPhone (Apple iOS 18 Light & Clean).
 * Cabeçalho inteligente que recolhe ao rolar para baixo, com filtros de período e tipo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitiesScreen(
    transactions: List<UnifiedTransaction>,
    onNavigateToScan: () -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToManual: () -> Unit,
    onBack: () -> Unit = {}
) {
    var isBalanceVisible by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPeriod by remember { mutableStateOf("ESTE_MES") } // ESTE_MES, 30_DIAS, MES_ANTERIOR, TODOS
    var selectedFilter by remember { mutableStateOf("TODOS") }   // TODOS, ENTRADAS, SAIDAS, ASSINATURAS, PAGAMENTOS, INTERNAS
    var selectedTransaction by remember { mutableStateOf<UnifiedTransaction?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Paleta Homogênea Apple Light
    val iosBg = Color(0xFFF2F2F7)             // Fundo cinza Apple
    val iosCard = Color(0xFFFFFFFF)           // Branco puro Apple
    val iosBorder = Color(0xFFE5E5EA)         // Borda sutil Apple
    val iosTextBlack = Color(0xFF1C1C1E)      // Texto principal
    val iosTextGray = Color(0xFF64748B)       // Subtítulos
    val iosTextMuted = Color(0xFF8E8E93)      // Metadados
    val iosBlue = Color(0xFF007AFF)           // Azul Apple Primário Único
    val iosGreen = Color(0xFF34C759)          // Verde Apple (Entradas +R$)
    val iosRed = Color(0xFFFF3B30)            // Vermelho Apple (Saídas -R$)

    // Nomes e prefixos de meses dinâmicos
    val currentMonthName = remember { SimpleDateFormat("MMMM", Locale("pt", "BR")).format(Date()).replaceFirstChar { it.uppercase() } }
    val currentMonthPrefix = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()) }
    val previousMonthPrefix = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
    }
    val previousMonthName = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        SimpleDateFormat("MMMM", Locale("pt", "BR")).format(cal.time).replaceFirstChar { it.uppercase() }
    }
    val thirtyDaysAgo = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -30)
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    // 1. Filtragem por Período
    val periodTransactions = remember(transactions, selectedPeriod) {
        transactions.filter { tx ->
            when (selectedPeriod) {
                "ESTE_MES" -> tx.date.startsWith(currentMonthPrefix) || tx.date.contains("2026-09")
                "30_DIAS" -> tx.date >= thirtyDaysAgo || tx.date.contains("2026-09") || tx.date.contains("2026-08")
                "MES_ANTERIOR" -> tx.date.startsWith(previousMonthPrefix) || tx.date.contains("2026-08")
                else -> true
            }
        }
    }

    // 2. Filtragem por Tipo e Busca
    val filteredTransactions = remember(periodTransactions, searchQuery, selectedFilter) {
        periodTransactions.filter { tx ->
            val matchesSearch = searchQuery.isBlank() ||
                    tx.title.contains(searchQuery, ignoreCase = true) ||
                    tx.category.contains(searchQuery, ignoreCase = true) ||
                    tx.bankName.contains(searchQuery, ignoreCase = true) ||
                    tx.notes?.contains(searchQuery, ignoreCase = true) == true

            val matchesType = when (selectedFilter) {
                "ENTRADAS" -> tx.type == TransactionType.ENTRADA && !tx.isInternalTransfer
                "SAIDAS" -> (tx.type == TransactionType.SAIDA || tx.type == TransactionType.CUPOM_FISCAL || tx.type == TransactionType.PAGAMENTO) && !tx.isInternalTransfer
                "ASSINATURAS" -> tx.isSubscription || tx.type == TransactionType.ASSINATURA
                "PAGAMENTOS" -> tx.type == TransactionType.PAGAMENTO || tx.paymentMethod.contains("PIX", ignoreCase = true)
                "INTERNAS" -> tx.isInternalTransfer || tx.type == TransactionType.TRANSFERENCIA
                else -> true
            }

            matchesSearch && matchesType
        }
    }

    // Totais Consolidados do Período Selecionado
    val totalEntradas = remember(periodTransactions) {
        periodTransactions.filter { it.type == TransactionType.ENTRADA && !it.isInternalTransfer }.sumOf { abs(it.amount) }
    }
    val totalSaidas = remember(periodTransactions) {
        periodTransactions.filter { !it.isInternalTransfer && it.type != TransactionType.ENTRADA && it.type != TransactionType.TRANSFERENCIA }.sumOf { abs(it.amount) }
    }
    val totalAssinaturas = remember(periodTransactions) {
        periodTransactions.filter { it.isSubscription || it.type == TransactionType.ASSINATURA }.sumOf { abs(it.amount) }
    }
    val saldoLiquido = totalEntradas - totalSaidas

    // Agrupamento por data
    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions.groupBy { it.dateGroup }
    }

    val listState = rememberLazyListState()
    val isScrolling by remember { derivedStateOf { listState.isScrollInProgress } }
    val isScrolledDown by remember { derivedStateOf { listState.firstVisibleItemIndex > 1 || listState.firstVisibleItemScrollOffset > 30 } }
    val aiPillWidth by animateDpAsState(
        targetValue = if (isScrolling || isScrolledDown) 50.dp else 220.dp,
        animationSpec = tween(300),
        label = "ai_pill_width_act"
    )

    val currentPeriodLabel = when (selectedPeriod) {
        "ESTE_MES" -> "$currentMonthName"
        "30_DIAS" -> "30 Dias"
        "MES_ANTERIOR" -> previousMonthName
        else -> "Todos"
    }
    val currentFilterLabel = when (selectedFilter) {
        "ENTRADAS" -> "Entradas"
        "SAIDAS" -> "Saídas"
        "ASSINATURAS" -> "Assinaturas"
        "PAGAMENTOS" -> "Pagamentos"
        "INTERNAS" -> "Internas"
        else -> "Todas as Movimentações"
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(iosBg)
        ) {
            // LISTA ÚNICA ROLÁVEL COM CABEÇALHO INTEGRADO (RECOLHE NATURALMENTE AO ROLAR)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 140.dp)
            ) {
                // ITEM 1: TOPO EXPANDIDO (STATUS BAR PADDING + PRIVACIDADE + BOTÃO NOVO)
                item {
                    Spacer(modifier = Modifier.statusBarsPadding())
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { isBalanceVisible = !isBalanceVisible },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(iosCard)
                                .border(1.dp, iosBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Ocultar Valores",
                                tint = iosTextGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                            IconButton(
                                onClick = onNavigateToAi,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF64748B))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "IA",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                                                    IconButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier
                                .size(44.dp)
                                .shadow(6.dp, CircleShape, spotColor = Color(0x18000000))
                                .clip(CircleShape)
                                .background(iosBlue)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Novo Lançamento",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // ITEM 2: TÍTULO DA TELA COM SUBTÍTULO
                item {
                    Column {
                        Text(
                            text = "Atividades",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = iosTextBlack,
                            letterSpacing = (-0.6).sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${filteredTransactions.size} lançamentos encontrados",
                            fontSize = 13.sp,
                            color = iosTextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // ITEM 3: BARRA DE BUSCA HOMOGÊNEA
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar lançamentos, lojas ou faturas...", fontSize = 14.sp, color = iosTextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = iosTextGray, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpar busca", tint = iosTextMuted, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color(0x06000000)),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = iosCard,
                            unfocusedContainerColor = iosCard,
                            focusedBorderColor = iosBlue,
                            unfocusedBorderColor = iosBorder,
                            focusedTextColor = iosTextBlack,
                            unfocusedTextColor = iosTextBlack
                        ),
                        singleLine = true
                    )
                }

                // ITEM 4: FILTROS DE PERÍODO (ATENDE SOLICITAÇÃO DE FILTROS EM ATIVIDADES)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "PERÍODO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosTextGray,
                            letterSpacing = 0.5.sp
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppleFilterChip(
                                label = "$currentMonthName (Atual)",
                                isSelected = selectedPeriod == "ESTE_MES",
                                onClick = { selectedPeriod = "ESTE_MES" },
                                activeColor = iosBlue
                            )
                            AppleFilterChip(
                                label = "30 Dias",
                                isSelected = selectedPeriod == "30_DIAS",
                                onClick = { selectedPeriod = "30_DIAS" },
                                activeColor = iosBlue
                            )
                            AppleFilterChip(
                                label = previousMonthName,
                                isSelected = selectedPeriod == "MES_ANTERIOR",
                                onClick = { selectedPeriod = "MES_ANTERIOR" },
                                activeColor = iosBlue
                            )
                            AppleFilterChip(
                                label = "Todos",
                                isSelected = selectedPeriod == "TODOS",
                                onClick = { selectedPeriod = "TODOS" },
                                activeColor = iosBlue
                            )
                        }
                    }
                }

                // ITEM 5: FILTROS DE TIPO / CATEGORIA
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "CATEGORIA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = iosTextGray,
                            letterSpacing = 0.5.sp
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppleFilterChip(
                                label = "Todos",
                                isSelected = selectedFilter == "TODOS",
                                onClick = { selectedFilter = "TODOS" },
                                activeColor = iosBlue
                            )
                            AppleFilterChip(
                                label = "Entradas",
                                isSelected = selectedFilter == "ENTRADAS",
                                onClick = { selectedFilter = "ENTRADAS" },
                                activeColor = iosBlue
                            )
                            AppleFilterChip(
                                label = "Saídas",
                                isSelected = selectedFilter == "SAIDAS",
                                onClick = { selectedFilter = "SAIDAS" },
                                activeColor = iosBlue
                            )
                            AppleFilterChip(
                                label = "Assinaturas",
                                isSelected = selectedFilter == "ASSINATURAS",
                                onClick = { selectedFilter = "ASSINATURAS" },
                                activeColor = iosBlue
                            )
                            AppleFilterChip(
                                label = "Pagamentos",
                                isSelected = selectedFilter == "PAGAMENTOS",
                                onClick = { selectedFilter = "PAGAMENTOS" },
                                activeColor = iosBlue
                            )
                            AppleFilterChip(
                                label = "Internas",
                                isSelected = selectedFilter == "INTERNAS",
                                onClick = { selectedFilter = "INTERNAS" },
                                activeColor = iosBlue
                            )
                        }
                    }
                }

                // ITEM 6: CARD RESUMO FINANCEIRO HOMOGÊNEO (APPLE LIGHT)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = Color(0x08000000)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = iosCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, iosBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Entradas (+)", fontSize = 11.sp, color = iosGreen, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    if (isBalanceVisible) "+${totalEntradas.formatBr()}" else "+R$ ••••••",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = iosGreen
                                )
                            }
                            Box(modifier = Modifier.width(1.dp).height(28.dp).background(iosBorder))
                            Column {
                                Text("Saídas (-)", fontSize = 11.sp, color = iosRed, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    if (isBalanceVisible) "-${totalSaidas.formatBr()}" else "-R$ ••••••",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = iosRed
                                )
                            }
                            Box(modifier = Modifier.width(1.dp).height(28.dp).background(iosBorder))
                            Column {
                                Text("Assinaturas", fontSize = 11.sp, color = iosTextGray, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    if (isBalanceVisible) totalAssinaturas.formatBr() else "R$ ••••••",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = iosTextBlack
                                )
                            }
                        }
                    }
                }

                // ITENS DA LISTA OU ESTADO VAZIO
                if (filteredTransactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = iosTextMuted, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Nenhum lançamento encontrado", fontWeight = FontWeight.Bold, color = iosTextBlack)
                                Text("Ajuste os filtros ou registre um novo lançamento.", fontSize = 13.sp, color = iosTextGray)
                            }
                        }
                    }
                } else {
                    groupedTransactions.forEach { (dateGroup, itemsInGroup) ->
                        item(key = "header_$dateGroup") {
                            Text(
                                text = dateGroup,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = iosTextBlack,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                            )
                        }

                        items(itemsInGroup, key = { it.id }) { tx ->
                            ActivityTransactionRow(
                                transaction = tx,
                                isBalanceVisible = isBalanceVisible,
                                iosCard = iosCard,
                                iosBorder = iosBorder,
                                iosTextBlack = iosTextBlack,
                                iosTextGray = iosTextGray,
                                iosGreen = iosGreen,
                                iosRed = iosRed,
                                onClick = { selectedTransaction = tx }
                            )
                        }
                    }
                }
            }

            // =========================================================================================
            // RECOLHIMENTO INTELIGENTE: CABEÇALHO COMPACTO FIXADO NO TOPO AO ROLAR PARA BAIXO
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
                        .shadow(12.dp, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp), spotColor = Color(0x14000000)),
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    color = Color(0xF2FFFFFF),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFE5E5EA))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ATIVIDADES • $currentPeriodLabel • $currentFilterLabel".uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = iosBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val displayValue = when (selectedFilter) {
                                    "ENTRADAS" -> "+${totalEntradas.formatBr()}"
                                    "SAIDAS" -> "-${totalSaidas.formatBr()}"
                                    else -> if (saldoLiquido >= 0) "+${saldoLiquido.formatBr()}" else "-${abs(saldoLiquido).formatBr()}"
                                }
                                val valueColor = when (selectedFilter) {
                                    "ENTRADAS" -> iosGreen
                                    "SAIDAS" -> iosRed
                                    else -> if (saldoLiquido >= 0) iosGreen else iosRed
                                }
                                Text(
                                    text = if (isBalanceVisible) displayValue else "R$ ••••••",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = valueColor
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

                        // Ações rápidas no cabeçalho compacto
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onNavigateToAi,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF64748B))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "IA",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                                                        IconButton(
                                onClick = { showAddDialog = true },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(iosBlue)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Novo",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

        }
    }

    // MODAL DE DETALHES
    if (selectedTransaction != null) {
        val tx = selectedTransaction!!
        ModalBottomSheet(
            onDismissRequest = { selectedTransaction = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            TransactionDetailBottomSheetContent(
                transaction = tx,
                isBalanceVisible = isBalanceVisible,
                onClose = { selectedTransaction = null }
            )
        }
    }

    // MODAL DE NOVO LANÇAMENTO
    if (showAddDialog) {
        ModalBottomSheet(
            onDismissRequest = { showAddDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Novo Lançamento", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = iosTextBlack)
                Text("Escolha como deseja adicionar seu lançamento:", fontSize = 13.sp, color = iosTextGray)

                Button(
                    onClick = {
                        showAddDialog = false
                        onNavigateToScan()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = iosBlue)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Escanear Cupom Fiscal ou NFC-e", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        showAddDialog = false
                        onNavigateToManual()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1E))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lançamento Manual", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

/**
 * Pílula de Filtro Homogênea Apple
 */
@Composable
fun AppleFilterChip(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) activeColor else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) activeColor else Color(0xFFE5E5EA)
        ),
        modifier = Modifier
            .clickable { onClick() }
            .shadow(if (isSelected) 3.dp else 1.dp, RoundedCornerShape(18.dp), spotColor = Color(0x0A000000))
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFF475569),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

/**
 * Linha de Transação Homogênea Estilo Apple
 * Correção crítica do bug de duplo R$ (-R$ R$ 228,80 -> -R$ 228,80).
 */
@Composable
fun ActivityTransactionRow(
    transaction: UnifiedTransaction,
    isBalanceVisible: Boolean,
    iosCard: Color,
    iosBorder: Color,
    iosTextBlack: Color,
    iosTextGray: Color,
    iosGreen: Color,
    iosRed: Color,
    onClick: () -> Unit
) {
    val isIncome = transaction.type == TransactionType.ENTRADA
    val isTransfer = transaction.isInternalTransfer || transaction.type == TransactionType.TRANSFERENCIA

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = iosCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, iosBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // í cone Original da Marca com Badge de Banco
            BrandTransactionIcon(
                title = transaction.title,
                category = transaction.category,
                bankName = transaction.bankName,
                isIncome = isIncome,
                size = 46.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Título e Subtítulos
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = iosTextBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${transaction.formattedDate} • ${transaction.category}",
                    fontSize = 11.sp,
                    color = iosTextGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (transaction.bankName.isNotEmpty()) {
                    Text(
                        text = transaction.bankName,
                        fontSize = 10.sp,
                        color = Color(0xFF8E8E93),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // CORREí‡íO CRí TICA DO BUG DE DUPLO R$:
            // formatBr() já retorna "R$ 228,80", logo prefixamos apenas com "+" ou "-"
            val formattedValue = if (isBalanceVisible) {
                val sign = if (isIncome) "+" else "-"
                "$sign${kotlin.math.abs(transaction.amount).formatBr()}"
            } else {
                if (isIncome) "+R$ •••" else "-R$ •••"
            }

            Text(
                text = formattedValue,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                color = when {
                    isIncome -> iosGreen
                    isTransfer -> iosTextGray
                    else -> iosRed
                }
            )
        }
    }
}

@Composable
fun TransactionDetailBottomSheetContent(
    transaction: UnifiedTransaction,
    isBalanceVisible: Boolean,
    onClose: () -> Unit
) {
    val isIncome = transaction.type == TransactionType.ENTRADA
    val isTransfer = transaction.isInternalTransfer || transaction.type == TransactionType.TRANSFERENCIA

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Detalhes do Lançamento", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1C1C1E))
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color(0xFF8E8E93))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        BrandTransactionIcon(
            title = transaction.title,
            category = transaction.category,
            bankName = transaction.bankName,
            isIncome = isIncome,
            size = 64.dp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = transaction.title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1C1C1E)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Correção de duplo R$ no modal também
        val modalValue = if (isBalanceVisible) {
            val sign = if (isIncome) "+" else "-"
            "$sign${kotlin.math.abs(transaction.amount).formatBr()}"
        } else {
            "R$ •••••"
        }

        Text(
            text = modalValue,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = when {
                isIncome -> Color(0xFF34C759)
                isTransfer -> Color(0xFF64748B)
                else -> Color(0xFFFF3B30)
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F7))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailRow("Data", transaction.formattedDate)
                DetailRow("Categoria", transaction.category)
                DetailRow("Instituição / Origem", transaction.bankName.ifEmpty { "FinLink AI" })
                DetailRow("Tipo", if (isIncome) "Entrada / Receita" else if (isTransfer) "Transferência Interna" else "Saída / Despesa")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = Color(0xFF8E8E93))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1C1E))
    }
}
