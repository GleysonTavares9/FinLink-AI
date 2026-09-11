package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.OpenFinanceAccountEntity
import com.example.data.util.SmartCategoryClassifier
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

val IosBlue = Color(0xFF007AFF)

data class ExpenseCategoryItem(
    val name: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpenseModalBottomSheet(
    onDismissRequest: () -> Unit,
    connectedAccounts: List<OpenFinanceAccountEntity> = emptyList(),
    onSaveExpense: (
        storeName: String,
        amount: Double,
        category: String,
        date: String,
        paymentMethod: String,
        notes: String?,
        installments: Int
    ) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var transactionType by remember { mutableStateOf("Despesa") } // "Despesa", "Receita", "PIX"
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Moradia") }
    var autoSuggestedCategory by remember { mutableStateOf<String?>(null) }

    // Contas bancárias disponíveis
    val availableAccounts = remember(connectedAccounts) {
        val list = mutableListOf<String>()
        if (connectedAccounts.isNotEmpty()) {
            list.addAll(connectedAccounts.map { it.bankName }.distinct())
        }
        // Opções padrão sempre presentes
        listOf("Nubank", "Banco Inter", "Itaú", "Santander", "Carteira / Dinheiro", "Cartão de Crédito").forEach {
            if (!list.contains(it)) list.add(it)
        }
        list
    }

    var selectedAccount by remember {
        mutableStateOf(availableAccounts.firstOrNull() ?: "Carteira / Dinheiro")
    }

    var selectedInstallments by remember { mutableIntStateOf(1) }
    var notes by remember { mutableStateOf("") }
    val todayDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.forLanguageTag("pt-BR")).format(Date())
    }

    val categories = listOf(
        ExpenseCategoryItem("Moradia", Icons.Default.Home, Color(0xFF0284C7)),
        ExpenseCategoryItem("Mercado", Icons.Default.ShoppingCart, Color(0xFF10B981)),
        ExpenseCategoryItem("Alimentação", Icons.Default.Restaurant, Color(0xFFF97316)),
        ExpenseCategoryItem("Transporte", Icons.Default.DirectionsCar, Color(0xFF3B82F6)),
        ExpenseCategoryItem("Saúde", Icons.Default.LocalHospital, Color(0xFFEF4444)),
        ExpenseCategoryItem("Lazer", Icons.Default.SportsEsports, Color(0xFFEC4899)),
        ExpenseCategoryItem("Compras", Icons.Default.ShoppingBag, Color(0xFF8B5CF6)),
        ExpenseCategoryItem("Salário", Icons.Default.AttachMoney, Color(0xFF059669)),
        ExpenseCategoryItem("Outros", Icons.Default.Category, Color(0xFF64748B))
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color(0xFFF8FAFC),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            // HEADER COM TÍTULO E BOTÃO FECHAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Novo Lançamento",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text = "Defina a conta, valor e parcelas recorrentes",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color(0xFF64748B))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SELETOR DE TIPO (DESPESA, RECEITA, PIX)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFE2E8F0))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Despesa", "Receita", "PIX").forEach { type ->
                    val isSelected = transactionType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(
                                if (isSelected) {
                                    when (type) {
                                        "Despesa" -> Color(0xFFEF4444)
                                        "Receita" -> Color(0xFF10B981)
                                        else -> Color(0xFF0D9488)
                                    }
                                } else Color.Transparent
                            )
                            .clickable { transactionType = type }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type,
                            color = if (isSelected) Color.White else Color(0xFF475569),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // CAMPO VALOR (R$)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFE2E8F0)))
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "VALOR DO LANÇAMENTO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "R$",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (transactionType) {
                                "Despesa" -> Color(0xFFEF4444)
                                "Receita" -> Color(0xFF10B981)
                                else -> Color(0xFF0D9488)
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { input ->
                                val clean = input.filter { it.isDigit() || it == ',' || it == '.' }
                                amountText = clean
                            },
                            placeholder = { Text("0,00", fontSize = 28.sp, color = Color(0xFF94A3B8)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.width(180.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A)
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // DESCRIÇÃO DO LANÇAMENTO COM AUTO-AGRUPAMENTO
            OutlinedTextField(
                value = description,
                onValueChange = { newDesc ->
                    description = newDesc
                    // AUTO-AGRUPAMENTO INTELIGENTE DA CATEGORIA
                    val autoCat = SmartCategoryClassifier.classifyCategory(newDesc)
                    if (autoCat != "Outros") {
                        selectedCategory = autoCat
                        autoSuggestedCategory = autoCat
                    }
                },
                label = { Text("Nome da Despesa / Loja / Conta") },
                placeholder = { Text("Ex: Conta de Luz, Copasa, Internet, EPA...") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = IosBlue,
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // FeedbacK de Auto-agrupamento de Categoria
            if (autoSuggestedCategory != null && description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(IosBlue.copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✨ Categoria agrupada automaticamente: ",
                        fontSize = 11.sp,
                        color = Color(0xFF475569)
                    )
                    Text(
                        text = selectedCategory,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = IosBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // DEFINIÇÃO DA CONTA / MEIO DE PAGAMENTO
            Text(
                text = "DEFINIR CONTA OU MEIO DE PAGAMENTO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableAccounts.forEach { accName ->
                    val isAccSelected = selectedAccount == accName
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isAccSelected) IosBlue.copy(alpha = 0.12f) else Color.White,
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.horizontalGradient(
                                if (isAccSelected) listOf(IosBlue, IosBlue)
                                else listOf(Color(0xFFE2E8F0), Color(0xFFE2E8F0))
                            )
                        ),
                        modifier = Modifier.clickable { selectedAccount = accName }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when {
                                    accName.contains("Cartão", ignoreCase = true) -> Icons.Default.CreditCard
                                    accName.contains("Carteira", ignoreCase = true) -> Icons.Default.Payment
                                    else -> Icons.Default.AccountBalance
                                },
                                contentDescription = null,
                                tint = if (isAccSelected) IosBlue else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = accName,
                                fontWeight = if (isAccSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (isAccSelected) IosBlue else Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }

            // SEÇÃO DE PARCELAMENTO RECORRENTE (SEPARAR PARA O MÊS RECORRENTE)
            if (transactionType == "Despesa") {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PARCELAS RECORRENTES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )
                    if (selectedInstallments > 1) {
                        Text(
                            text = "$selectedInstallments meses recorrentes",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = IosBlue
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                val installmentOptions = listOf(1, 2, 3, 4, 5, 6, 10, 12)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    installmentOptions.forEach { count ->
                        val isInstSelected = selectedInstallments == count
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isInstSelected) Color(0xFF8B5CF6).copy(alpha = 0.15f) else Color.White,
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.horizontalGradient(
                                    if (isInstSelected) listOf(Color(0xFF8B5CF6), Color(0xFF8B5CF6))
                                    else listOf(Color(0xFFE2E8F0), Color(0xFFE2E8F0))
                                )
                            ),
                            modifier = Modifier.clickable { selectedInstallments = count }
                        ) {
                            Text(
                                text = if (count == 1) "À vista (1x)" else "${count}x",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontWeight = if (isInstSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (isInstSelected) Color(0xFF7C3AED) else Color(0xFF334155)
                            )
                        }
                    }
                }

                // Resumo do Parcelamento Mês a Mês
                if (selectedInstallments > 1) {
                    val parsedAmount = parseBrDouble(amountText)
                    val instVal = if (parsedAmount > 0.0) parsedAmount / selectedInstallments else 0.0

                    // Calcula meses recorrentes
                    val monthsList = remember(selectedInstallments) {
                        val cal = Calendar.getInstance()
                        val sdf = SimpleDateFormat("MMM/yy", Locale.forLanguageTag("pt-BR"))
                        (0 until selectedInstallments).map { i ->
                            val c = Calendar.getInstance()
                            c.add(Calendar.MONTH, i)
                            sdf.format(c.time).replaceFirstChar { it.uppercase() }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDD6FE))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Distribuição nos meses recorrentes:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6D28D9)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${selectedInstallments}x de R$ ${String.format(Locale.forLanguageTag("pt-BR"), "%,.2f", instVal)} em: ${monthsList.joinToString(", ")}.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF4C1D95)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Cada parcela será lançada separadamente no seu respectivo mês recorrente.",
                                fontSize = 10.sp,
                                color = Color(0xFF7C3AED)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // CATEGORIAS COM ÍCONES COLORIDOS
            Text(
                text = "CATEGORIA OU GRUPO ORÇAMENTÁRIO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isCatSelected = selectedCategory == cat.name
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCatSelected) cat.color.copy(alpha = 0.15f) else Color.White,
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.horizontalGradient(
                                if (isCatSelected) listOf(cat.color, cat.color)
                                else listOf(Color(0xFFE2E8F0), Color(0xFFE2E8F0))
                            )
                        ),
                        modifier = Modifier.clickable { selectedCategory = cat.name }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                cat.icon,
                                contentDescription = null,
                                tint = if (isCatSelected) cat.color else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cat.name,
                                fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (isCatSelected) cat.color else Color(0xFF334155)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // OBSERVAÇÕES OPCIONAIS
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Observações (Opcional)") },
                placeholder = { Text("Ex: Parcela, lembrete, código de barras...") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = IosBlue,
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // BOTÃO SALVAR LANÇAMENTO
            Button(
                onClick = {
                    val amountVal = parseBrDouble(amountText)
                    if (amountVal <= 0.0) {
                        Toast.makeText(context, "Informe um valor válido maior que zero.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val finalStore = if (description.isNotBlank()) description.trim() else "Lançamento $transactionType"
                    val finalMethod = if (transactionType == "PIX") "$selectedAccount (PIX)" else selectedAccount

                    onSaveExpense(
                        finalStore,
                        amountVal,
                        selectedCategory,
                        todayDate,
                        finalMethod,
                        notes.ifBlank { null },
                        if (transactionType == "Despesa") selectedInstallments else 1
                    )
                    Toast.makeText(context, "Lançamento salvo com sucesso!", Toast.LENGTH_SHORT).show()
                    onDismissRequest()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (transactionType) {
                        "Despesa" -> Color(0xFFEF4444)
                        "Receita" -> Color(0xFF10B981)
                        else -> Color(0xFF0D9488)
                    }
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Confirmar $transactionType",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

fun parseBrDouble(text: String): Double {
    return try {
        val clean = text.trim().replace(".", "").replace(",", ".")
        clean.toDoubleOrNull() ?: 0.0
    } catch (e: Exception) {
        0.0
    }
}
