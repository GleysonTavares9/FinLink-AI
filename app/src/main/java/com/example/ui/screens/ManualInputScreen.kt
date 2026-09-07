package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosGreen
import com.example.ui.theme.IosIndigo
import com.example.ui.theme.IosOrange
import com.example.ui.theme.IosPurple
import com.example.ui.theme.IosRed
import com.example.util.formatBr
import com.example.util.parseBrDouble
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManualInputScreen(
    onBack: () -> Unit,
    onSaveExpense: (storeName: String, amount: Double, category: String, date: String, notes: String?) -> Unit
) {
    val context = LocalContext.current
    var storeName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Alimentação") }
    var dateText by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.forLanguageTag("pt-BR")).format(Date()))
    }
    var notesText by remember { mutableStateOf("") }

    val categories = listOf("Alimentação", "Transporte", "Mercado", "Lazer", "Saúde", "Moradia", "Outros")
    val quickAddValues = listOf(10.0, 20.0, 50.0, 100.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Novo Lançamento",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = IosBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF3F5FA))
        ) {
            // Fundo Aurora Orgânico
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF0EA5E9).copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.10f),
                        radius = size.width * 0.65f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF10B981).copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.35f),
                        radius = size.width * 0.55f
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card Principal: Valor em Moeda Brasileira Real
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(26.dp), spotColor = Color(0x180F172A)),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xF8FFFFFF), Color(0xEEF8FAFD))
                                )
                            )
                            .border(
                                width = 1.5.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.White, Color(0x15000000))
                                ),
                                shape = RoundedCornerShape(26.dp)
                            )
                            .padding(22.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "VALOR DA TRANSAÇÃO",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = amountText,
                                onValueChange = { amountText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_amount"),
                                placeholder = { Text("0,00", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color(0xFF94A3B8)) },
                                prefix = {
                                    Text(
                                        "R$ ",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF059669),
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                },
                                shape = RoundedCornerShape(18.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFFF1F5F9),
                                    unfocusedContainerColor = Color(0xFFF8FAFC),
                                    focusedBorderColor = Color(0xFF10B981),
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Pílulas de Incremento Rápido
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                quickAddValues.forEach { quickVal ->
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFFF1F5F9),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                val current = amountText.parseBrDouble() ?: 0.0
                                                amountText = String.format(Locale.forLanguageTag("pt-BR"), "%.2f", current + quickVal)
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "+ R$ ${quickVal.toInt()}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF334155)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Card de Detalhes da Despesa
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
                            .background(Color(0xEEFFFFFF))
                            .border(1.2.dp, Color(0xFFE2E8F0), RoundedCornerShape(22.dp))
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Estabelecimento
                            OutlinedTextField(
                                value = storeName,
                                onValueChange = { storeName = it },
                                label = { Text("Nome do Estabelecimento / Loja") },
                                placeholder = { Text("Ex: Supermercado, Farmácia, Restaurante") },
                                leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, tint = IosBlue) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IosBlue,
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )

                            // Categoria
                            Text(
                                text = "Categoria",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B)
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                categories.forEach { cat ->
                                    val isSelected = selectedCategory == cat
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) IosBlue else Color(0xFFF1F5F9),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) Color.Transparent else Color(0xFFE2E8F0)
                                        ),
                                        modifier = Modifier.clickable { selectedCategory = cat }
                                    ) {
                                        Text(
                                            text = cat,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Color(0xFF334155)
                                        )
                                    }
                                }
                            }

                            // Data da Compra
                            OutlinedTextField(
                                value = dateText,
                                onValueChange = { dateText = it },
                                label = { Text("Data da Transação") },
                                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = IosOrange) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IosOrange,
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )

                            // Observações Opcionais
                            OutlinedTextField(
                                value = notesText,
                                onValueChange = { notesText = it },
                                label = { Text("Observações (Opcional)") },
                                placeholder = { Text("Ex: Compra do mês, PIX pessoal") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IosIndigo,
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )
                        }
                    }
                }

                // Botão Principal de Confirmação
                Button(
                    onClick = {
                        val amount = amountText.parseBrDouble() ?: 0.0
                        if (amount <= 0.0) {
                            Toast.makeText(context, "Informe um valor monetário em R$ válido.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (storeName.isBlank()) {
                            Toast.makeText(context, "Informe o nome do estabelecimento.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        onSaveExpense(storeName.trim(), amount, selectedCategory, dateText, notesText.ifBlank { null })
                        Toast.makeText(context, "Despesa registrada com sucesso!", Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IosBlue)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salvar Despesa no Banco", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
