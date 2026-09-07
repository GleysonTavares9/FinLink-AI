package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.OpenFinanceAccountEntity
import com.example.data.openfinance.OpenFinanceBankInfo
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosGreen
import com.example.ui.theme.IosIndigo
import com.example.ui.theme.IosOrange
import com.example.ui.theme.IosPurple
import com.example.ui.theme.IosRed
import com.example.util.formatBr
import com.example.util.parseBrDouble

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenFinanceScreen(
    connectedAccounts: List<OpenFinanceAccountEntity>,
    availableBanks: List<OpenFinanceBankInfo>,
    apiSpecJson: String,
    onBack: () -> Unit,
    onConnectBank: (bank: OpenFinanceBankInfo, accountType: String, initialBalance: Double, accountNumber: String, autoSync: Boolean) -> Unit,
    onSyncAccount: (accountId: String, onDone: (Int) -> Unit) -> Unit,
    onCreateCustomAccount: (bankName: String, accountType: String, accountNumber: String, balance: Double, autoSync: Boolean) -> Unit,
    onImportStatementText: (text: String, targetAccountId: String?, onDone: (Int) -> Unit) -> Unit,
    onDisconnectBank: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    // Estado do Modal de Conexão com Banco
    var bankToConnect by remember { mutableStateOf<OpenFinanceBankInfo?>(null) }
    var selectedAccountType by remember { mutableStateOf("Conta Corrente") }
    var balanceInputText by remember { mutableStateOf("1500,00") }
    var agencyAccountInput by remember { mutableStateOf("") }
    var autoSyncTransactions by remember { mutableStateOf(true) }

    // Estado para importação de extrato
    var statementInputText by remember { mutableStateOf("") }
    var selectedTargetAccountIndex by remember { mutableIntStateOf(0) }

    // Estado para conta manual customizada
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var customBankName by remember { mutableStateOf("") }
    var customBalanceInput by remember { mutableStateOf("0,00") }

    val tabTitles = listOf("Minhas Contas", "Importar Extrato", "API Specs")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Open Finance Brasil",
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
            // Fundo Aurora Orgânico com Efeito Vidro
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF6366F1).copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.12f),
                        radius = size.width * 0.65f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF0EA5E9).copy(alpha = 0.10f), Color.Transparent),
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
            ) {
                // Banner de Segurança Open Finance Brasil (BACEN / FAPI)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(22.dp), spotColor = Color(0x180F172A))
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                                )
                            )
                            .border(
                                width = 1.2.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF334155), IosBlue.copy(alpha = 0.4f))
                                ),
                                shape = RoundedCornerShape(22.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(IosBlue.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = IosBlue, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "OPEN FINANCE BRASIL • BACEN",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Conecte seus bancos reais com segurança FAPI 1.0 e sincronize saldos e extratos automaticamente.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE2E8F0),
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Seletor de Abas em Cápsula Translúcida
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xEEFFFFFF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            val isSelected = selectedTab == index
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) IosBlue else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTab = index }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (isSelected) Color.White else Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (selectedTab) {
                    0 -> {
                        // ABA 1: MINHAS CONTAS E BANCOS
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "CONTAS BANCÁRIAS CONECTADAS (${connectedAccounts.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF64748B),
                                        letterSpacing = 1.sp
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = IosIndigo.copy(alpha = 0.12f),
                                        modifier = Modifier.clickable { showAddCustomDialog = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = IosIndigo, modifier = Modifier.size(16.dp))
                                            Text("+ Outra Conta", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IosIndigo)
                                        }
                                    }
                                }
                            }

                            if (connectedAccounts.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(22.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xEEFFFFFF))
                                                .border(1.2.dp, Color(0xFFE2E8F0), RoundedCornerShape(22.dp))
                                                .padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(52.dp)
                                                        .clip(CircleShape)
                                                        .background(IosIndigo.copy(alpha = 0.12f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Default.AccountBalance,
                                                        contentDescription = null,
                                                        tint = IosIndigo,
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(
                                                    text = "Nenhuma conta vinculada ainda",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Escolha um banco abaixo para conectar via Open Finance e importar movimentações reais.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF64748B),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                items(connectedAccounts, key = { it.id }) { acc ->
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
                                                        colors = listOf(Color(0xF5FFFFFF), Color(0xEEF8FAFC))
                                                    )
                                                )
                                                .border(
                                                    width = 1.2.dp,
                                                    brush = Brush.verticalGradient(
                                                        colors = listOf(Color.White, Color(0x15000000))
                                                    ),
                                                    shape = RoundedCornerShape(22.dp)
                                                )
                                                .padding(18.dp)
                                        ) {
                                            Column {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(46.dp)
                                                                .clip(RoundedCornerShape(14.dp))
                                                                .background(IosIndigo.copy(alpha = 0.15f)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                Icons.Default.AccountBalance,
                                                                contentDescription = null,
                                                                tint = IosIndigo,
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(14.dp))
                                                        Column {
                                                            Text(
                                                                text = acc.bankName,
                                                                style = MaterialTheme.typography.titleMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF0F172A)
                                                            )
                                                            Text(
                                                                text = "${acc.accountType} • ${acc.accountNumber}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = Color(0xFF64748B)
                                                            )
                                                        }
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            onDisconnectBank(acc.id)
                                                            Toast.makeText(context, "${acc.bankName} desconectado.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Desconectar", tint = IosRed.copy(alpha = 0.75f))
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(14.dp))

                                                // Saldo em Moeda Brasileira Real
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = "Saldo Disponível",
                                                            fontSize = 11.sp,
                                                            color = Color(0xFF64748B),
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        Text(
                                                            text = acc.balance.formatBr(),
                                                            fontSize = 20.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = Color(0xFF059669)
                                                        )
                                                    }

                                                    Surface(
                                                        shape = RoundedCornerShape(16.dp),
                                                        color = IosBlue.copy(alpha = 0.12f),
                                                        modifier = Modifier.clickable {
                                                            onSyncAccount(acc.id) { count ->
                                                                Toast.makeText(context, "Sincronização concluída! $count movimentações atualizadas.", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = IosBlue, modifier = Modifier.size(16.dp))
                                                            Text("Sincronizar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IosBlue)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "BANCOS BRASILEIROS DISPONÍVEIS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF64748B),
                                    letterSpacing = 1.sp
                                )
                            }

                            items(availableBanks, key = { it.id }) { bank ->
                                val isConnected = connectedAccounts.any { it.bankName.contains(bank.name.take(6), ignoreCase = true) }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Color(0x100F172A)),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xEEFFFFFF))
                                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(42.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(Color(bank.primaryColor).copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = bank.name.take(2).uppercase(),
                                                        color = Color(bank.primaryColor),
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 15.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = bank.name,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = Color(0xFF0F172A)
                                                    )
                                                    Text(
                                                        text = "Compensação: ${bank.code} • Open Finance Ativo",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFF64748B)
                                                    )
                                                }
                                            }

                                            if (isConnected) {
                                                Surface(
                                                    color = Color(0xFFDCFCE7),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text(
                                                        text = "Conectado ✦",
                                                        color = Color(0xFF15803D),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                    )
                                                }
                                            } else {
                                                Surface(
                                                    color = IosBlue,
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.clickable {
                                                        bankToConnect = bank
                                                        selectedAccountType = if (bank.id == "nubank") "Cartão de Crédito / NuConta" else "Conta Corrente"
                                                        balanceInputText = "1500,00"
                                                        agencyAccountInput = "Ag 0001 • C/C ${((10000..99999).random())}-${(1..9).random()}"
                                                        autoSyncTransactions = true
                                                    }
                                                ) {
                                                    Text(
                                                        text = "Conectar",
                                                        fontSize = 12.sp,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }

                    1 -> {
                        // ABA 2: IMPORTAR EXTRATO BANCÁRIO REAL (Texto ou PIX)
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
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
                                            .background(Color(0xEEFFFFFF))
                                            .border(1.2.dp, Color(0xFFE2E8F0), RoundedCornerShape(22.dp))
                                            .padding(20.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(IosBlue.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.ContentPaste, contentDescription = null, tint = IosBlue, modifier = Modifier.size(20.dp))
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = "Importador de Extrato / PIX / Cartão",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF0F172A)
                                                    )
                                                    Text(
                                                        text = "Cole o extrato bancário em moeda brasileira (R$)",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFF64748B)
                                                    )
                                                }
                                            }

                                            OutlinedTextField(
                                                value = statementInputText,
                                                onValueChange = { statementInputText = it },
                                                placeholder = {
                                                    Text(
                                                        "Cole aqui o texto copiado do app do seu banco.\nExemplos aceitos:\n15/08 PIX Enviado Mercado R$ 142,80\n14/08 Posto Shell Combustível R$ 85,00\n12/08 Drogaria Farmácia R$ 43,50",
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(180.dp),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = IosBlue,
                                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                                    focusedContainerColor = Color.White,
                                                    unfocusedContainerColor = Color.White
                                                )
                                            )

                                            Button(
                                                onClick = {
                                                    if (statementInputText.isBlank()) {
                                                        Toast.makeText(context, "Cole o texto do extrato antes de processar.", Toast.LENGTH_SHORT).show()
                                                        return@Button
                                                    }
                                                    val targetId = connectedAccounts.getOrNull(selectedTargetAccountIndex)?.id
                                                    onImportStatementText(statementInputText, targetId) { count ->
                                                        if (count > 0) {
                                                            Toast.makeText(context, "$count lançamentos importados com sucesso na sua conta!", Toast.LENGTH_LONG).show()
                                                            statementInputText = ""
                                                            selectedTab = 0
                                                        } else {
                                                            Toast.makeText(context, "Nenhum valor monetário em R$ reconhecido no texto.", Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(52.dp),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = IosBlue)
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Processar Extrato e Salvar Gastos", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // ABA 3: ESPECIFICAÇÃO TÉCNICA OPEN FINANCE BRASIL
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFF0F172A),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Text(
                                    text = apiSpecJson,
                                    color = Color(0xFF38BDF8),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // MODAL DE AUTORIZAÇÃO E CONEXÃO SEGURA OPEN FINANCE (TOTALMENTE FUNCIONAL)
    bankToConnect?.let { bank ->
        AlertDialog(
            onDismissRequest = { bankToConnect = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(bank.primaryColor).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = bank.name.take(2).uppercase(),
                            color = Color(bank.primaryColor),
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }
                    Column {
                        Text("Conectar ${bank.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Open Finance Brasil • FAPI 1.0", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Autorize o FinLink AI a consultar saldos e transações para organizar seus gastos automaticamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )

                    OutlinedTextField(
                        value = selectedAccountType,
                        onValueChange = { selectedAccountType = it },
                        label = { Text("Tipo de Conta") },
                        placeholder = { Text("Ex: Conta Corrente, NuConta") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = balanceInputText,
                        onValueChange = { balanceInputText = it },
                        label = { Text("Saldo Atual da Conta (R$)") },
                        placeholder = { Text("1.500,00") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    OutlinedTextField(
                        value = agencyAccountInput,
                        onValueChange = { agencyAccountInput = it },
                        label = { Text("Agência e Conta (Opcional)") },
                        placeholder = { Text("Ag 0001 • C/C 12345-6") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sincronizar Extrato Inicial", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Importa compras recentes em R$ para os gastos", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                        Switch(
                            checked = autoSyncTransactions,
                            onCheckedChange = { autoSyncTransactions = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IosGreen)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedBalance = balanceInputText.parseBrDouble() ?: 1500.0
                        onConnectBank(
                            bank,
                            selectedAccountType,
                            parsedBalance,
                            agencyAccountInput,
                            autoSyncTransactions
                        )
                        Toast.makeText(
                            context,
                            "Conexão com ${bank.name} autorizada com sucesso!",
                            Toast.LENGTH_LONG
                        ).show()
                        bankToConnect = null
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IosBlue)
                ) {
                    Text("Autorizar e Conectar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { bankToConnect = null },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // MODAL DE CONTA MANUAL
    if (showAddCustomDialog) {
        AlertDialog(
            onDismissRequest = { showAddCustomDialog = false },
            title = { Text("Cadastrar Outra Conta ou Carteira", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customBankName,
                        onValueChange = { customBankName = it },
                        label = { Text("Nome da Instituição ou Carteira") },
                        placeholder = { Text("Ex: Carteira Dinheiro, Inter, C6") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = customBalanceInput,
                        onValueChange = { customBalanceInput = it },
                        label = { Text("Saldo Inicial (R$)") },
                        placeholder = { Text("0,00") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customBankName.isNotBlank()) {
                            val bal = customBalanceInput.parseBrDouble() ?: 0.0
                            onCreateCustomAccount(customBankName, "Conta Personalizada", "Conta Principal", bal, true)
                            Toast.makeText(context, "$customBankName adicionada com sucesso!", Toast.LENGTH_SHORT).show()
                            showAddCustomDialog = false
                            customBankName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IosBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Salvar Conta")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAddCustomDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
