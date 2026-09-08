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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.BudgetAnalysis
import com.example.domain.model.HouseholdProfile
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosGreen
import com.example.ui.theme.IosIndigo
import com.example.ui.theme.IosOrange
import com.example.ui.theme.IosPurple
import com.example.ui.theme.IosRed
import com.example.ui.theme.IosTeal
import com.example.util.formatBr
import com.example.util.formatBrNumber
import com.example.util.parseBrDouble

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdScreen(
    householdProfile: HouseholdProfile?,
    budgetAnalysis: BudgetAnalysis?,
    onUpdateProfile: (adults: Int, children: Int, monthlyIncome: Double) -> Unit,
    onConsultAiWithHousehold: () -> Unit,
    onBack: () -> Unit
) {
    val initialAdults = householdProfile?.adultCount ?: 1
    val initialChildren = householdProfile?.childCount ?: 0
    val initialIncome = householdProfile?.monthlyIncome ?: 0.0

    var adultCount by remember(householdProfile) { mutableIntStateOf(initialAdults) }
    var childCount by remember(householdProfile) { mutableIntStateOf(initialChildren) }
    var incomeInput by remember(householdProfile) {
        mutableStateOf(if (initialIncome > 0) formatBrNumber(initialIncome) else "")
    }
    var isSavedConfirmation by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F5FA))
    ) {
        // Fundo Aurora Multicolorido Suave
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF6366F1).copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.90f, size.height * 0.10f),
                    radius = size.width * 0.65f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0EA5E9).copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(size.width * 0.10f, size.height * 0.40f),
                    radius = size.width * 0.55f
                )
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Orçamento Familiar",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF1D1D1F)
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Glass Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = Color(0x10000000)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xEEFFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Brush.verticalGradient(listOf(Color.White, Color(0x20000000))), RoundedCornerShape(24.dp))
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(IosIndigo, Color(0xFF4F46E5))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.FamilyRestroom, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    "Composição Familiar",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = Color(0xFF1D1D1F)
                                )
                                Text(
                                    "Cálculo adaptado per capita por adulto e dependente",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B7280)
                                )
                            }
                        }
                    }
                }

                // Configuração da Família
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(14.dp, RoundedCornerShape(24.dp), spotColor = Color(0x12000000)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xF4FFFFFF))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Brush.verticalGradient(listOf(Color.White, Color(0x1E000000))), RoundedCornerShape(24.dp))
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Composição da Residência",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF1D1D1F)
                        )

                        // Adultos
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
                                        .background(IosBlue.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = IosBlue, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Adultos", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1D1D1F))
                                    Text("A partir de 18 anos", fontSize = 11.sp, color = Color(0xFF86868B))
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (adultCount > 1) adultCount-- },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .border(1.dp, Color(0xFFD1D5DB), CircleShape)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Diminuir", tint = IosBlue)
                                }
                                Text(
                                    text = "$adultCount",
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = Color(0xFF1D1D1F)
                                )
                                IconButton(
                                    onClick = { adultCount++ },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .border(1.dp, Color(0xFFD1D5DB), CircleShape)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Aumentar", tint = IosBlue)
                                }
                            }
                        }

                        // Crianças
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
                                        .background(IosOrange.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ChildCare, contentDescription = null, tint = IosOrange, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Crianças / Dependentes", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1D1D1F))
                                    Text("Custos infantis e escola", fontSize = 11.sp, color = Color(0xFF86868B))
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (childCount > 0) childCount-- },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .border(1.dp, Color(0xFFD1D5DB), CircleShape)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Diminuir", tint = IosOrange)
                                }
                                Text(
                                    text = "$childCount",
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = Color(0xFF1D1D1F)
                                )
                                IconButton(
                                    onClick = { childCount++ },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .border(1.dp, Color(0xFFD1D5DB), CircleShape)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Aumentar", tint = IosOrange)
                                }
                            }
                        }

                        // Renda Mensal
                        OutlinedTextField(
                            value = incomeInput,
                            onValueChange = { incomeInput = it },
                            label = { Text("Renda Familiar Mensal (R$)") },
                            placeholder = { Text("Ex: 5.500,00") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IosBlue,
                                unfocusedBorderColor = Color(0xFFD1D5DB),
                                focusedTextColor = Color(0xFF1D1D1F),
                                unfocusedTextColor = Color(0xFF1D1D1F)
                            ),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                val parsed = parseBrDouble(incomeInput)
                                onUpdateProfile(adultCount, childCount, parsed)
                                isSavedConfirmation = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IosBlue)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Salvar Perfil Familiar", fontWeight = FontWeight.Bold)
                        }

                        AnimatedVisibility(visible = isSavedConfirmation) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = IosGreen.copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "✓ Configuração familiar salva no banco com sucesso!",
                                    color = Color(0xFF15803D),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }

                // Se houver análise orçamentária ativa
                if (budgetAnalysis != null) {
                    // Resumo Métricas
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(14.dp, RoundedCornerShape(24.dp), spotColor = Color(0x12000000)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xF4FFFFFF))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Brush.verticalGradient(listOf(Color.White, Color(0x1E000000))), RoundedCornerShape(24.dp))
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                "Resumo do Orçamento Doméstico",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = Color(0xFF1D1D1F)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Teto Mensal Sugerido", fontSize = 12.sp, color = Color(0xFF86868B))
                                    Text(
                                        formatBr(budgetAnalysis.totalRecommendedBudget),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = IosBlue
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Gasto Real Atual", fontSize = 12.sp, color = Color(0xFF86868B))
                                    Text(
                                        formatBr(budgetAnalysis.totalActualSpent),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = if (budgetAnalysis.overallPercentage > 100) IosRed else Color(0xFF15803D)
                                    )
                                }
                            }

                            // Progress bar
                            val progressValue = (budgetAnalysis.overallPercentage / 100.0).toFloat().coerceIn(0f, 1f)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        "Consumo: ${budgetAnalysis.overallPercentage.toInt()}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D1D1F)
                                    )
                                    Text(
                                        "Per Capita: ${formatBr(budgetAnalysis.totalPerCapitaSpent)} / pessoa",
                                        fontSize = 12.sp,
                                        color = Color(0xFF86868B)
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { progressValue },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (budgetAnalysis.overallPercentage > 100) IosRed else IosGreen,
                                    trackColor = Color(0xFFE5E7EB)
                                )
                            }

                            // Projeção do mês
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(IosIndigo.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = IosIndigo, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Projeção para o fim do mês", fontSize = 11.sp, color = Color(0xFF6B7280))
                                    Text(
                                        formatBr(budgetAnalysis.projectedMonthEndSpent),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = IosIndigo
                                    )
                                }
                            }
                        }
                    }

                    // Diagnóstico & Dicas IA
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(14.dp, RoundedCornerShape(24.dp), spotColor = Color(0x12000000)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xF4FFFFFF))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Brush.verticalGradient(listOf(Color.White, Color(0x1E000000))), RoundedCornerShape(24.dp))
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = IosTeal, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Diagnóstico da Família", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1D1D1F))
                            }

                            Text(
                                budgetAnalysis.summaryMessage,
                                fontSize = 13.sp,
                                color = Color(0xFF374151),
                                lineHeight = 18.sp
                            )

                            budgetAnalysis.actionableTips.forEach { tip ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("• ", color = IosBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(tip, fontSize = 12.sp, color = Color(0xFF4B5563))
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = onConsultAiWithHousehold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IosPurple)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Consultar IA sobre a Família", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    // Detalhamento por Categorias
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(14.dp, RoundedCornerShape(24.dp), spotColor = Color(0x12000000)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xF4FFFFFF))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Brush.verticalGradient(listOf(Color.White, Color(0x1E000000))), RoundedCornerShape(24.dp))
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                "Detalhamento por Categorias",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF1D1D1F)
                            )

                            budgetAnalysis.categoryStatuses.forEach { cat ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(cat.category, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF1D1D1F))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            if (cat.isOverBudget) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = IosRed.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        "Acima (+${formatBr(cat.differenceAmount)})",
                                                        color = IosRed,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            "${formatBr(cat.actualSpent)} / ${formatBr(cat.recommendedBudget)}",
                                            fontSize = 12.sp,
                                            color = Color(0xFF6B7280)
                                        )
                                    }

                                    val catPercent = (cat.percentageUsed / 100.0).toFloat().coerceIn(0f, 1f)
                                    LinearProgressIndicator(
                                        progress = { catPercent },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = if (cat.isOverBudget) IosRed else IosBlue,
                                        trackColor = Color(0xFFE5E7EB)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Empty State quando ainda não configurado
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(10.dp, RoundedCornerShape(22.dp), spotColor = Color(0x10000000)),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xF4FFFFFF))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Brush.verticalGradient(listOf(Color.White, Color(0x1E000000))), RoundedCornerShape(22.dp))
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Nenhum Dado Fictício",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF1D1D1F)
                            )
                            Text(
                                "Defina a quantidade de adultos, crianças e sua renda acima para ativar as análises de gastos e metas personalizadas da família.",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280),
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}
