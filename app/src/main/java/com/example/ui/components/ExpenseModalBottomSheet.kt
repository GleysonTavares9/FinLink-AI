package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Store
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosGreen
import com.example.ui.theme.IosRed
import com.example.util.formatBr
import com.example.util.parseBrDouble
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        notes: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var transactionType by remember { mutableStateOf("Despesa") } // "Despesa", "Receita", "PIX"
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Alimentação") }
    var selectedAccount by remember {
        mutableStateOf(connectedAccounts.firstOrNull()?.bankName ?: "Carteira / Dinheiro")
    }
    var notes by remember { mutableStateOf("") }
    val todayDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.forLanguageTag("pt-BR")).format(Date())
    }

    val categories = listOf(
        ExpenseCategoryItem("Alimentação", Icons.Default.Restaurant, Color(0xFFF97316)),
        ExpenseCategoryItem("Transporte", Icons.Default.DirectionsCar, Color(0xFF3B82F6)),
        ExpenseCategoryItem("Mercado", Icons.Default.ShoppingCart, Color(0xFF10B981)),
        ExpenseCategoryItem("Moradia", Icons.Default.Home, Color(0xFF8B5CF6)),
        ExpenseCategoryItem("Lazer", Icons.Default.SportsEsports, Color(0xFFEC4899)),
        ExpenseCategoryItem("Saúde", Icons.Default.LocalHospital, Color(0xFFEF4444)),
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
            // HEADER COM TÍTULO E BOTíO FECHAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Novo Lançamento",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Registre suas transações manuais rapidamente",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color(0xFF64748B))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SELETOR DE TIPO (Despesa / Receita / PIX)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE2E8F0).copy(alpha = 0.7f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Despesa", "Receita", "PIX").forEach { type ->
                    val isSelected = transactionType == type
                    val activeBg = when (type) {
                        "Despesa" -> Color(0xFFEF4444)
                        "Receita" -> Color(0xFF10B981)
                        else -> Color(0xFF0D9488)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) activeBg else Color.Transparent)
                            .clickable { transactionType = type }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp,
                            color = if (isSelected) Color.White else Color(0xFF475569)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CARD DO VALOR EM DESTAQUE
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "VALOR DA TRANSAÇíO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "R$",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = when (transactionType) {
                                "Despesa" -> Color(0xFFEF4444)
                                "Receita" -> Color(0xFF10B981)
                                else -> Color(0xFF0D9488)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() || it == ',' || it == '.' }) {
                                    amountText = input
                                }
                            },
                            placeholder = { Text("0,00", fontSize = 32.sp, color = Color(0xFFCBD5E1)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent
                            ),
                            modifier = Modifier.width(180.dp)
                        )
                    }

                    // CHIPS DE ACRÉSCIMO RÁPIDO (+10, +20, +50, +100)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10.0, 20.0, 50.0, 100.0).forEach { value ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.clickable {
                                    val cur = parseBrDouble(amountText)
                                    val updated = cur + value
                                    amountText = String.format(Locale.forLanguageTag("pt-BR"), "%.2f", updated)
                                }
                            ) {
                                Text(
                                    text = "+R$ ${value.toInt()}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // DESCRIÇíO / ESTABELECIMENTO
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrição / Estabelecimento") },
                placeholder = { Text("Ex: Supermercado, Almoço, Posto...") },
                leadingIcon = {
                    Icon(Icons.Default.Store, contentDescription = null, tint = Color(0xFF64748B))
                },
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

            Spacer(modifier = Modifier.height(14.dp))

            // SELETOR DE CONTA / BANCO
            Text(
                text = "CONTA OU MEIO DE PAGAMENTO",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF64748B),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Opção 1: Dinheiro / Carteira
                val isCashSelected = selectedAccount == "Carteira / Dinheiro"
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isCashSelected) Color(0xFF4F46E5).copy(alpha = 0.1f) else Color.White,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            if (isCashSelected) listOf(Color(0xFF4F46E5), Color(0xFF6366F1))
                            else listOf(Color(0xFFE2E8F0), Color(0xFFE2E8F0))
                        )
                    ),
                    modifier = Modifier.clickable { selectedAccount = "Carteira / Dinheiro" }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Payment,
                            contentDescription = null,
                            tint = if (isCashSelected) Color(0xFF4F46E5) else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Carteira / Dinheiro",
                            fontWeight = if (isCashSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (isCashSelected) Color(0xFF4F46E5) else Color(0xFF0F172A)
                        )
                    }
                }

                // Opções de Contas Bancárias Conectadas
                connectedAccounts.forEach { acc ->
                    val isAccSelected = selectedAccount == acc.bankName
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isAccSelected) Color(0xFF4F46E5).copy(alpha = 0.1f) else Color.White,
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.horizontalGradient(
                                if (isAccSelected) listOf(Color(0xFF4F46E5), Color(0xFF6366F1))
                                else listOf(Color(0xFFE2E8F0), Color(0xFFE2E8F0))
                            )
                        ),
                        modifier = Modifier.clickable { selectedAccount = acc.bankName }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BankLogoView(bankName = acc.bankName, size = 22.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = acc.bankName,
                                fontWeight = if (isAccSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (isAccSelected) Color(0xFF4F46E5) else Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // CATEGORIAS COM ÍCONES COLORIDOS
            Text(
                text = "CATEGORIA",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
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
                placeholder = { Text("Ex: Pago via PIX, parcelado...") },
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

            // BOTíO SALVAR LANÇAMENTO
            Button(
                onClick = {
                    val amountVal = parseBrDouble(amountText)
                    if (amountVal <= 0.0) {
                        Toast.makeText(context, "Informe um valor válido maior que zero.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val finalStore = if (description.isNotBlank()) description.trim() else "Lançamento $transactionType"
                    val finalAmount = if (transactionType == "Despesa") amountVal else -amountVal

                    onSaveExpense(
                        finalStore,
                        amountVal,
                        selectedCategory,
                        todayDate,
                        if (transactionType == "PIX") "PIX" else selectedAccount,
                        notes.ifBlank { null }
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
