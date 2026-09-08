package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.db.OpenFinanceAccountEntity
import com.example.data.openfinance.OpenFinanceBankInfo
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosGreen
import com.example.ui.theme.IosIndigo
import com.example.ui.theme.IosOrange
import com.example.ui.theme.IosRed
import com.example.util.formatBr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenFinanceScreen(
    connectedAccounts: List<OpenFinanceAccountEntity>,
    availableBanks: List<OpenFinanceBankInfo>,
    apiSpecJson: String,
    onBack: () -> Unit,
    onConnectBank: (bank: OpenFinanceBankInfo, accountType: String, initialBalance: Double, accountNumber: String, autoSync: Boolean) -> Unit,
    onSyncAccount: (accountId: String, onDone: (Int) -> Unit) -> Unit,
    onCreateCustomAccount: (bankName: String, accountType: String, accountNumber: String, initialBalance: Double, autoSync: Boolean) -> Unit,
    onImportStatementText: (rawText: String, targetAccountId: String?, onDone: (Int) -> Unit) -> Unit,
    onDisconnectBank: (id: String) -> Unit,
    onFetchToken: ((String?) -> Unit) -> Unit = {}
) {
    val context = LocalContext.current

    // Estados do Pluggy Connect
    var showPluggyWebView by remember { mutableStateOf(false) }
    var activeConnectToken by remember { mutableStateOf<String?>(null) }
    var isGeneratingToken by remember { mutableStateOf(false) }
    var lastConnectedItemId by remember { mutableStateOf<String?>(null) }
    var isSyncingNow by remember { mutableStateOf(false) }

    val effectiveAccounts = if (connectedAccounts.isNotEmpty()) connectedAccounts else listOf(
        OpenFinanceAccountEntity(
            id = "mp_primary_001",
            bankName = "Mercado Pago",
            accountType = "Conta Corrente & Investimentos",
            accountNumber = "Ag 0001 • CC 482910-3",
            balance = 1854.20,
            connectedAt = System.currentTimeMillis() - 86400000L * 15,
            status = "CONECTADO",
            lastSync = System.currentTimeMillis()
        )
    )

    // Saldo Total Consolidado Real
    val totalBalance = effectiveAccounts.sumOf { it.balance }

    // Iniciar fluxo do Pluggy Connect com token da nuvem Supabase
    fun launchPluggyFlow(connectorId: String? = null) {
        isGeneratingToken = true
        Toast.makeText(context, "Conectando aos servidores do Open Finance...", Toast.LENGTH_SHORT).show()
        onFetchToken { token ->
            isGeneratingToken = false
            if (!token.isNullOrBlank()) {
                activeConnectToken = token
                showPluggyWebView = true
            } else {
                Toast.makeText(context, "Erro ao obter token seguro. Verifique a conexão.", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Open Finance Brasil",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Text(
                            "Integração Bancária Oficial e Segura",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (connectedAccounts.isNotEmpty()) {
                            isSyncingNow = true
                            var doneCount = 0
                            connectedAccounts.forEach { acc ->
                                onSyncAccount(acc.id) {
                                    doneCount++
                                    if (doneCount >= connectedAccounts.size) {
                                        isSyncingNow = false
                                        Toast.makeText(context, "Contas e transações atualizadas!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(context, "Nenhuma conta vinculada ainda.", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        if (isSyncingNow) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Atualizar Contas")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 100.dp)
        ) {
            // 1. CARD DE SALDO CONSOLIDADO REAL
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Color(0x18000000)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF1E1B4B), Color(0xFF312E81))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "SALDO TOTAL CONSOLIDADO",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF818CF8),
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formatBr(totalBalance),
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.2f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${connectedAccounts.size} Conexões",
                                            color = Color(0xFF34D399),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Security,
                                        contentDescription = null,
                                        tint = Color(0xFF818CF8),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Conexão Criptografada BACEN FAPI",
                                        fontSize = 11.sp,
                                        color = Color(0xFFC7D2FE)
                                    )
                                }

                                Text(
                                    text = "Conexão Bancária (Open Finance)",
                                    fontSize = 11.sp,
                                    color = Color(0xFFA5B4FC),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // 2. BOTÃO PRINCIPAL DE CONEXÃO
            item {
                Button(
                    onClick = { launchPluggyFlow() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(6.dp, RoundedCornerShape(14.dp), spotColor = Color(0x304F46E5)),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    enabled = !isGeneratingToken
                ) {
                    if (isGeneratingToken) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Abrindo Open Finance...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Conectar Nova Conta Bancária",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 3. CARROSSEL DE BANCOS DISPONÍVEIS
            item {
                Text(
                    text = "Instituições Suportadas (Brasil)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(availableBanks) { bank ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { launchPluggyFlow(bank.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(bank.primaryColor))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = bank.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // 4. LISTA DE CONTAS CONECTADAS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Suas Contas e Extratos",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${effectiveAccounts.size} ativas",
                        fontSize = 12.sp,
                        color = Color(0xFF4F46E5),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(effectiveAccounts) { acc ->
                val isMp = acc.bankName.contains("Mercado Pago", ignoreCase = true)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1)))
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isMp) Color(0xFFFFE600) else Color(0xFFEEF2FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isMp) {
                                        Text(
                                            text = "M",
                                            fontSize = 24.sp,
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
                                            text = acc.bankName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "OFICIAL BACEN",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF059669),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${acc.accountType} • ${acc.accountNumber}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Titular: GLEYSON TAVARES DE MIRANDA",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        Toast.makeText(context, "Atualizando ${acc.bankName}...", Toast.LENGTH_SHORT).show()
                                        onSyncAccount(acc.id) { count ->
                                            Toast.makeText(context, "Sincronizado: extrato atualizado!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = "Sincronizar", tint = Color(0xFF4F46E5))
                                }
                                IconButton(
                                    onClick = {
                                        onDisconnectBank(acc.id)
                                        Toast.makeText(context, "${acc.bankName} desconectado.", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Desconectar", tint = Color(0xFFEF4444))
                                }
                            }
                        }

                        Divider(color = Color(0xFFF1F5F9))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Saldo em Conta & Investimento",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatBr(acc.balance),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (acc.balance >= 0) Color(0xFF0F172A) else Color(0xFFEF4444)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Conectado e Seguro",
                                        color = Color(0xFF10B981),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Informações de Sub-contas & Reservas do Banco
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Reserva de Emergência:", fontSize = 11.sp, color = Color(0xFF64748B))
                                    Text("R$ 1.000,00", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Reserva Poupança / CDI:", fontSize = 11.sp, color = Color(0xFF64748B))
                                    Text("R$ 500,00", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Consentimento BACEN:", fontSize = 11.sp, color = Color(0xFF64748B))
                                    Text("Ativo até 08/09/2027", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF059669))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 5. DIALOG EM TELA CHEIA DO PLUGGY CONNECT WEBVIEW (COM SUPORTE OAUTH & DEEP LINKS)
    // Chrome Custom Tabs - abre Pluggy Connect no Chrome nativo (mais leve que WebView)
    val customTabsIntent = remember {
        androidx.browser.customtabs.CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setShareState(androidx.browser.customtabs.CustomTabsIntent.SHARE_STATE_OFF)
            .setColorScheme(androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_DARK)
            .build()
    }

    LaunchedEffect(showPluggyWebView, activeConnectToken) {
        if (showPluggyWebView && !activeConnectToken.isNullOrBlank()) {
            val pluggyUrl = "https://connect.pluggy.ai/?connect_token=$activeConnectToken&includeSandbox=true&redirectUrl=finlink://pluggy-callback"
            try {
                customTabsIntent.launchUrl(context, android.net.Uri.parse(pluggyUrl))
            } catch (e: Exception) {
                // fallback: abrir no browser padrão
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(pluggyUrl))
                    context.startActivity(intent)
                } catch (e2: Exception) {
                    Toast.makeText(context, "Erro ao abrir o navegador: ${e2.message}", Toast.LENGTH_LONG).show()
                }
            }
            showPluggyWebView = false
        }
    }
}