package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionType
import com.example.data.model.UnifiedTransaction
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosGreen
import com.example.ui.theme.IosOrange
import com.example.ui.theme.IosPurple
import com.example.ui.theme.IosRed
import com.example.util.formatBr

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
    var selectedFilter by remember { mutableStateOf("TODOS") } // TODOS, ENTRADAS, SAIDAS, ASSINATURAS, PAGAMENTOS
    var selectedTransaction by remember { mutableStateOf<UnifiedTransaction?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Filtragem de transações
    val filteredTransactions = remember(transactions, searchQuery, selectedFilter) {
        transactions.filter { tx ->
            val matchesSearch = searchQuery.isBlank() ||
                    tx.title.contains(searchQuery, ignoreCase = true) ||
                    tx.category.contains(searchQuery, ignoreCase = true) ||
                    tx.bankName.contains(searchQuery, ignoreCase = true) ||
                    tx.notes?.contains(searchQuery, ignoreCase = true) == true

            val matchesType = when (selectedFilter) {
                "ENTRADAS" -> tx.type == TransactionType.ENTRADA
                "SAIDAS" -> tx.type == TransactionType.SAIDA || tx.type == TransactionType.CUPOM_FISCAL
                "ASSINATURAS" -> tx.isSubscription || tx.type == TransactionType.ASSINATURA
                "PAGAMENTOS" -> tx.type == TransactionType.PAGAMENTO || tx.paymentMethod.contains("PIX", ignoreCase = true)
                else -> true
            }

            matchesSearch && matchesType
        }
    }

    // Totais Consolidados
    val totalEntradas = transactions.filter { it.type == TransactionType.ENTRADA }.sumOf { it.amount }
    val totalSaidas = transactions.filter { it.type != TransactionType.ENTRADA }.sumOf { it.amount }
    val totalAssinaturas = transactions.filter { it.isSubscription || it.type == TransactionType.ASSINATURA }.sumOf { it.amount }

    // Agrupamento por data (Hoje, Ontem, Setembro 2026, etc.)
    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions.groupBy { it.dateGroup }
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF4F6FB))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                // TOPO: ÍCONE DE OLHO (PRIVACIDADE) + BOTÃO (+)
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
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Ocultar Valores",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Botão (+) Flutuante no topo direito (estilo Pierre / Atividades)
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                            .shadow(8.dp, CircleShape, spotColor = Color(0xFF10B981).copy(alpha = 0.4f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Novo Lançamento",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // TÍTULO DA TELA
                Text(
                    text = "Atividades",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // BARRA DE BUSCA
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar lançamentos, lojas ou assinaturas...", fontSize = 14.sp, color = Color(0xFF94A3B8)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = IosBlue,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // CHIPS DE FILTRO (Filtros, Entradas, Saídas, Assinaturas, Pagamentos)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActivityFilterChip(
                        label = "Todos",
                        icon = Icons.Default.Tune,
                        isSelected = selectedFilter == "TODOS",
                        onClick = { selectedFilter = "TODOS" }
                    )
                    ActivityFilterChip(
                        label = "Entradas",
                        icon = null,
                        isSelected = selectedFilter == "ENTRADAS",
                        badgeColor = Color(0xFF10B981),
                        onClick = { selectedFilter = "ENTRADAS" }
                    )
                    ActivityFilterChip(
                        label = "Saídas",
                        icon = null,
                        isSelected = selectedFilter == "SAIDAS",
                        badgeColor = IosRed,
                        onClick = { selectedFilter = "SAIDAS" }
                    )
                    ActivityFilterChip(
                        label = "Assinaturas",
                        icon = Icons.Default.Subscriptions,
                        isSelected = selectedFilter == "ASSINATURAS",
                        badgeColor = IosPurple,
                        onClick = { selectedFilter = "ASSINATURAS" }
                    )
                    ActivityFilterChip(
                        label = "Pagamentos",
                        icon = Icons.Default.CreditCard,
                        isSelected = selectedFilter == "PAGAMENTOS",
                        onClick = { selectedFilter = "PAGAMENTOS" }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // CARD RESUMO FINANCEIRO COMPACTO
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1))))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Entradas (+)", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                            Text(
                                if (isBalanceVisible) "+${totalEntradas.formatBr()}" else "+R$ •••••",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF10B981)
                            )
                        }
                        Box(modifier = Modifier.width(1.dp).height(28.dp).background(Color(0xFFE2E8F0)))
                        Column {
                            Text("Saídas (-)", fontSize = 11.sp, color = IosRed, fontWeight = FontWeight.Bold)
                            Text(
                                if (isBalanceVisible) "-${totalSaidas.formatBr()}" else "-R$ •••••",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                        }
                        Box(modifier = Modifier.width(1.dp).height(28.dp).background(Color(0xFFE2E8F0)))
                        Column {
                            Text("Assinaturas", fontSize = 11.sp, color = IosPurple, fontWeight = FontWeight.Bold)
                            Text(
                                if (isBalanceVisible) totalAssinaturas.formatBr() else "R$ •••••",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = IosPurple
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // LISTA DE TRANSAÇÕES AGRUPADAS
                if (filteredTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Nenhum lançamento encontrado", fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                            Text("Ajuste os filtros ou registre um novo gasto/cupom.", fontSize = 13.sp, color = Color(0xFF94A3B8))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 90.dp)
                    ) {
                        groupedTransactions.forEach { (dateGroup, itemsInGroup) ->
                            item(key = "header_$dateGroup") {
                                Text(
                                    text = dateGroup,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                )
                            }

                            items(itemsInGroup, key = { it.id }) { tx ->
                                ActivityTransactionRow(
                                    transaction = tx,
                                    isBalanceVisible = isBalanceVisible,
                                    onClick = { selectedTransaction = tx }
                                )
                            }
                        }
                    }
                }
            }

            // PÍLULA FLUTUANTE INFERIOR: "Pergunte ao FinLink AI" (Idêntico ao Screenshot 2)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp, start = 16.dp, end = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Color.White,
                    shadowElevation = 14.dp,
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1)))),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToAi() }
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = IosPurple, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Pergunte ao FinLink AI...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF64748B)
                            )
                        }

                        IconButton(
                            onClick = onNavigateToScan,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9))
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear", tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }

    // MODAL DE DETALHES DA TRANSAÇÃO AO TOCAR
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

    // MODAL DE AÇÕES RÁPIDAS (+)
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
                Text("Novo Lançamento", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text("Escolha como deseja adicionar seu lançamento:", fontSize = 13.sp, color = Color(0xFF64748B))

                Button(
                    onClick = {
                        showAddDialog = false
                        onNavigateToScan()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
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
                    colors = ButtonDefaults.buttonColors(containerColor = IosBlue)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lançamento Manual (Entrada/Saída)", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun ActivityFilterChip(
    label: String,
    icon: ImageVector?,
    isSelected: Boolean,
    badgeColor: Color? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Color(0xFF0F172A) else Color.White,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(
                if (isSelected) listOf(Color(0xFF0F172A), Color(0xFF0F172A))
                else listOf(Color(0xFFE2E8F0), Color(0xFFE2E8F0))
            )
        ),
        modifier = Modifier
            .height(38.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else Color(0xFF475569),
                    modifier = Modifier.size(16.dp)
                )
            }
            if (badgeColor != null) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(badgeColor))
            }
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFF334155)
            )
        }
    }
}

@Composable
fun ActivityTransactionRow(
    transaction: UnifiedTransaction,
    isBalanceVisible: Boolean,
    onClick: () -> Unit
) {
    val isIncome = transaction.type == TransactionType.ENTRADA
    val isSub = transaction.isSubscription || transaction.type == TransactionType.ASSINATURA

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0)))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ÍCONE COM BADGE DO BANCO / SERVIÇO
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            transaction.title.contains("NETFLIX", ignoreCase = true) -> Color(0xFF1E1E1E)
                            transaction.title.contains("SPOTIFY", ignoreCase = true) -> Color(0xFF1DB954).copy(alpha = 0.15f)
                            transaction.title.contains("Farmácia", ignoreCase = true) || transaction.category.contains("Farmácia", ignoreCase = true) -> Color(0xFF0EA5E9).copy(alpha = 0.15f)
                            isIncome -> Color(0xFF10B981).copy(alpha = 0.15f)
                            isSub -> Color(0xFFAF52DE).copy(alpha = 0.15f)
                            else -> Color(0xFF64748B).copy(alpha = 0.12f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (transaction.title.contains("NETFLIX", ignoreCase = true)) {
                    Text("N", color = Color(0xFFE50914), fontWeight = FontWeight.Black, fontSize = 20.sp)
                } else {
                    Icon(
                        imageVector = when {
                            transaction.title.contains("Farmácia", ignoreCase = true) -> Icons.Default.LocalPharmacy
                            isSub -> Icons.Default.Subscriptions
                            isIncome -> Icons.Default.AccountBalance
                            transaction.type == TransactionType.CUPOM_FISCAL -> Icons.AutoMirrored.Filled.ReceiptLong
                            else -> Icons.Default.CreditCard
                        },
                        contentDescription = null,
                        tint = when {
                            transaction.title.contains("Farmácia", ignoreCase = true) -> Color(0xFF0284C7)
                            isIncome -> Color(0xFF10B981)
                            isSub -> IosPurple
                            else -> Color(0xFF475569)
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Badge miniatura do banco (ex: Mercado Pago, Nubank) no canto inferior
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (transaction.bankName.contains("Mercado", ignoreCase = true)) Color(0xFFFFE600) else Color(0xFF820AD1))
                        .border(1.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (transaction.bankName.contains("Mercado", ignoreCase = true)) "M" else "N",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (transaction.bankName.contains("Mercado", ignoreCase = true)) Color(0xFF003B6D) else Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // TÍTULO E SUBTÍTULO
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = transaction.subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // VALOR COM DESTAQUE
            val formattedValue = if (isBalanceVisible) {
                val prefix = if (isIncome) "+" else "-"
                "$prefix${transaction.amount.formatBr()}"
            } else {
                if (isIncome) "+R$ •••••" else "-R$ •••••"
            }

            Text(
                text = formattedValue,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = if (isIncome) Color(0xFF10B981) else Color(0xFF0F172A)
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Detalhes do Lançamento",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF0F172A)
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Fechar")
            }
        }

        // VALOR GRANDE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(if (isIncome) Color(0xFFECFDF5) else Color(0xFFF8FAFC))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isIncome) "VALOR RECEBIDO" else "VALOR PAGO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncome) Color(0xFF059669) else Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isBalanceVisible) {
                        (if (isIncome) "+" else "-") + transaction.amount.formatBr()
                    } else "R$ •••••",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isIncome) Color(0xFF059669) else Color(0xFF0F172A)
                )
            }
        }

        // CAMPOS DETALHADOS
        DetailItemRow(label = "Estabelecimento / Título", value = transaction.title)
        DetailItemRow(label = "Categoria", value = transaction.category)
        DetailItemRow(label = "Conta / Banco", value = transaction.bankName)
        DetailItemRow(label = "Forma de Pagamento", value = transaction.paymentMethod)
        DetailItemRow(label = "Data da Operação", value = transaction.date)
        DetailItemRow(label = "Tipo", value = when (transaction.type) {
            TransactionType.ENTRADA -> "Entrada / Receita"
            TransactionType.ASSINATURA -> "Assinatura Recorrente"
            TransactionType.CUPOM_FISCAL -> "Cupom Fiscal NFC-e"
            TransactionType.PAGAMENTO -> "Pagamento / Transferência"
            else -> "Despesa / Saída"
        })

        // Se for cupom com itens
        if (transaction.items.isNotEmpty()) {
            Text("Itens do Cupom Fiscal (${transaction.items.size}):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                transaction.items.forEach { item ->
                    Text("• $item", fontSize = 12.sp, color = Color(0xFF334155))
                }
            }
        }

        if (!transaction.notes.isNullOrBlank()) {
            DetailItemRow(label = "Observações", value = transaction.notes)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun DetailItemRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = Color(0xFF64748B))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
    }
}
