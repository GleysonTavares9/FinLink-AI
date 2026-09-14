package com.example.ui.screens

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
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
    onBack: () -> Unit = {},
    onDeleteExpense: (Long) -> Unit = {},
    onEditExpense: (Long, String, Double, String, String, String, String?) -> Unit = { _, _, _, _, _, _, _ -> },
    isSyncing: Boolean = false,
    remainingManualSyncs: Int = 2,
    onManualSync: ((Boolean, String) -> Unit) -> Unit = {}
) {
    var isBalanceVisible by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPeriod by remember { mutableStateOf("ESTE_MES") } // ESTE_MES, 30_DIAS, MES_ANTERIOR, TODOS
    var selectedFilter by remember { mutableStateOf("TODOS") }   // TODOS, ENTRADAS, SAIDAS, ASSINATURAS, PAGAMENTOS, INTERNAS
    var selectedTransaction by remember { mutableStateOf<UnifiedTransaction?>(null) }
    var editingTransaction by remember { mutableStateOf<UnifiedTransaction?>(null) }
    var transactionToDelete by remember { mutableStateOf<UnifiedTransaction?>(null) }
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
    val nextMonthPrefix = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, 1)
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
    }
    val nextMonthName = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, 1)
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
                "PROXIMO_MES" -> tx.date.startsWith(nextMonthPrefix) || tx.date.contains("2026-10")
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
    var isInlineSearchOpen by remember { mutableStateOf(false) }
    val isScrolling by remember { derivedStateOf { listState.isScrollInProgress } }
    val isScrolledDown by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 || (listState.firstVisibleItemIndex == 2 && listState.firstVisibleItemScrollOffset > 40) } }
    val aiPillWidth by animateDpAsState(
        targetValue = if (isScrolling || isScrolledDown) 50.dp else 220.dp,
        animationSpec = tween(300),
        label = "ai_pill_width_act"
    )

    val currentPeriodLabel = when (selectedPeriod) {
        "ESTE_MES" -> currentMonthName
        "PROXIMO_MES" -> "$nextMonthName (Parcelas)"
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

    val context = LocalContext.current
    var isRefreshing by remember { mutableStateOf(false) }
    val onRefreshAction: () -> Unit = {
        isRefreshing = true
        onManualSync { allowed, msg ->
            isRefreshing = false
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(iosBg)
        ) {
            PullToRefreshBox(
                isRefreshing = isRefreshing || isSyncing,
                onRefresh = onRefreshAction,
                modifier = Modifier.fillMaxSize()
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


                                                    Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {


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
                                label = "$nextMonthName (Parcelas)",
                                isSelected = selectedPeriod == "PROXIMO_MES",
                                onClick = { selectedPeriod = "PROXIMO_MES" },
                                activeColor = Color(0xFF8B5CF6)
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
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.ui.graphics.RectangleShape,
                    color = Color(0xF8FFFFFF),
                    shadowElevation = 4.dp,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE5E5EA))
                ) {
                                        if (isInlineSearchOpen || searchQuery.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Buscar lançamentos...", fontSize = 13.sp, color = iosTextMuted) },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = iosBlue, modifier = Modifier.size(18.dp))
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "Limpar busca", tint = iosTextMuted, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFFF2F2F7),
                                    unfocusedContainerColor = Color(0xFFF2F2F7),
                                    focusedBorderColor = iosBlue,
                                    unfocusedBorderColor = Color(0xFFE5E5EA),
                                    focusedTextColor = iosTextBlack,
                                    unfocusedTextColor = iosTextBlack
                                ),
                                singleLine = true
                            )
                            IconButton(
                                onClick = {
                                    isInlineSearchOpen = false
                                    searchQuery = ""
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF2F2F7))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar Busca", tint = iosTextGray, modifier = Modifier.size(18.dp))
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Título limpo e elegante com período e contagem
                            Column {
                                Text(
                                    text = "Atividades",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = iosTextBlack,
                                    letterSpacing = (-0.4).sp
                                )
                                Text(
                                    text = "$currentPeriodLabel • ${filteredTransactions.size} registros",
                                    fontSize = 11.sp,
                                    color = iosTextGray,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Ações rápidas à direita (Busca instantânea + Novo lançamento)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { isInlineSearchOpen = true },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF2F2F7))
                                        .border(1.dp, Color(0xFFE5E5EA), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Buscar",
                                        tint = iosBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { showAddDialog = true },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(iosBlue)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Novo Lançamento",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }                }
            }

            // BOTÃO FLUTUANTE DE IA (FAB APPLE STYLE)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 108.dp, end = 18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .height(54.dp)
                        .width(aiPillWidth)
                        .shadow(16.dp, RoundedCornerShape(27.dp), spotColor = Color(0x33007AFF))
                        .clip(RoundedCornerShape(27.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(Color(0xFF007AFF), Color(0xFF0EA5E9))
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(Color(0x80FFFFFF), Color(0x33FFFFFF))
                            ),
                            shape = RoundedCornerShape(27.dp)
                        )
                        .clickable { onNavigateToAi() }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (aiPillWidth > 70.dp) Arrangement.SpaceBetween else Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "IA Gemini",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )

                        if (aiPillWidth > 70.dp) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Assistente IA",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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
                onClose = { selectedTransaction = null },
                onEditClick = {
                    val target = tx
                    selectedTransaction = null
                    editingTransaction = target
                },
                onDeleteClick = {
                    val target = tx
                    selectedTransaction = null
                    transactionToDelete = target
                }
            )
        }
    }

    
    // DIÁLOGO DE CONFIRMAÇÃO DE EXCLUSÃO DE DESPESA MANUAL
    if (transactionToDelete != null) {
        val tx = transactionToDelete!!
        val localId = tx.id.removePrefix("exp_").toLongOrNull()
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            title = {
                Text(
                    text = "Excluir Despesa Manual",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1C1C1E)
                )
            },
            text = {
                Text(
                    text = "Deseja realmente excluir '${tx.title}' no valor de ${abs(tx.amount).formatBr()}?\n\nEsta despesa será removida do seu controle financeiro e sincronização.",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (localId != null) {
                            onDeleteExpense(localId)
                        }
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Excluir", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { transactionToDelete = null }
                ) {
                    Text("Cancelar", color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                }
            }
        )
    }

    // MODAL DE EDIÇÃO DE DESPESA MANUAL
    if (editingTransaction != null) {
        val tx = editingTransaction!!
        val localId = tx.id.removePrefix("exp_").toLongOrNull()

        var editStoreName by remember(tx) { mutableStateOf(tx.title) }
        var editAmount by remember(tx) { mutableStateOf(String.format(Locale.US, "%.2f", abs(tx.amount))) }
        var editCategory by remember(tx) { mutableStateOf(tx.category) }
        var editDate by remember(tx) { mutableStateOf(tx.date) }
        var editPaymentMethod by remember(tx) { mutableStateOf(tx.paymentMethod.ifEmpty { "Pix" }) }
        var editNotes by remember(tx) { mutableStateOf(tx.notes ?: "") }

        val categories = listOf("Alimentação", "Moradia", "Transporte", "Saúde", "Lazer", "Educação", "Serviços", "Compras", "Outros")
        val paymentMethods = listOf("Pix", "Cartão de Crédito", "Cartão de Débito", "Dinheiro", "Boleto", "Conta Bancária")

        ModalBottomSheet(
            onDismissRequest = { editingTransaction = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Editar Despesa Manual",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E)
                    )
                    IconButton(onClick = { editingTransaction = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color(0xFF8E8E93))
                    }
                }

                // Nome / Estabelecimento
                OutlinedTextField(
                    value = editStoreName,
                    onValueChange = { editStoreName = it },
                    label = { Text("Nome do Estabelecimento / Descrição") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = iosBlue,
                        unfocusedBorderColor = iosBorder
                    ),
                    singleLine = true
                )

                // Valor
                OutlinedTextField(
                    value = editAmount,
                    onValueChange = { editAmount = it.replace(',', '.') },
                    label = { Text("Valor (R$)") },
                    leadingIcon = {
                        Text("R$", fontWeight = FontWeight.Bold, color = iosBlue, modifier = Modifier.padding(start = 12.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = iosBlue,
                        unfocusedBorderColor = iosBorder
                    ),
                    singleLine = true
                )

                // Categoria
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Categoria", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = iosTextGray)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            AppleFilterChip(
                                label = cat,
                                isSelected = editCategory.equals(cat, ignoreCase = true),
                                onClick = { editCategory = cat },
                                activeColor = iosBlue
                            )
                        }
                    }
                }

                // Data
                OutlinedTextField(
                    value = editDate,
                    onValueChange = { editDate = it },
                    label = { Text("Data (AAAA-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = iosBlue,
                        unfocusedBorderColor = iosBorder
                    ),
                    singleLine = true
                )

                // Método de Pagamento
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Método de Pagamento", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = iosTextGray)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        paymentMethods.forEach { method ->
                            AppleFilterChip(
                                label = method,
                                isSelected = editPaymentMethod.equals(method, ignoreCase = true),
                                onClick = { editPaymentMethod = method },
                                activeColor = Color(0xFF0284C7)
                            )
                        }
                    }
                }

                // Observações
                OutlinedTextField(
                    value = editNotes,
                    onValueChange = { editNotes = it },
                    label = { Text("Observações (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = iosBlue,
                        unfocusedBorderColor = iosBorder
                    ),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val parsedAmount = editAmount.toDoubleOrNull() ?: abs(tx.amount)
                        if (localId != null && editStoreName.isNotBlank()) {
                            onEditExpense(
                                localId,
                                editStoreName.trim(),
                                parsedAmount,
                                editCategory,
                                editDate.trim(),
                                editPaymentMethod,
                                editNotes.ifBlank { null }
                            )
                        }
                        editingTransaction = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = iosBlue)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salvar Alterações", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
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
                        // Título e Subtítulos
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = iosTextBlack,
                        letterSpacing = (-0.2).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    val isInst = transaction.title.contains("Parcela", ignoreCase = true) || transaction.notes?.contains("Parcela", ignoreCase = true) == true
                    if (isInst) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFEDE9FE)
                        ) {
                            Text(
                                text = "💳 Parcela",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C3AED),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
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
    onClose: () -> Unit,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
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
                DetailRow("Método / Conta", transaction.paymentMethod.ifEmpty { "Manual" })
                DetailRow("Tipo", if (isIncome) "Entrada / Receita" else if (isTransfer) "Transferência Interna" else "Saída / Despesa")
                if (!transaction.notes.isNullOrBlank()) {
                    DetailRow("Observações", transaction.notes!!)
                }
            }
        }

        val isManualExpense = transaction.id.startsWith("exp_")
        if (isManualExpense) {
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Botão Editar
                Button(
                    onClick = onEditClick,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEBF5FF),
                        contentColor = Color(0xFF007AFF)
                    )
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Editar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                // Botão Excluir
                Button(
                    onClick = onDeleteClick,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFEE2E2),
                        contentColor = Color(0xFFDC2626)
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Excluir", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Excluir", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
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
