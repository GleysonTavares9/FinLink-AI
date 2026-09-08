package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppOfferingFeature
import com.example.data.model.SubscriptionItem
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosGreen
import com.example.ui.theme.IosOrange
import com.example.ui.theme.IosPurple
import com.example.ui.theme.IosRed
import com.example.util.formatBr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAndOfferingsScreen(
    userFullName: String = "GLEYSON TAVARES DE MIRANDA",
    userEmail: String = "gleyson@finlink.ai",
    connectedBankCount: Int = 1,
    appOfferings: List<AppOfferingFeature>,
    subscriptions: List<SubscriptionItem>,
    onToggleSubscription: (String) -> Unit,
    onNavigateToOpenFinance: () -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showOfferingsModal by remember { mutableStateOf(false) }
    var showSubscriptionsModal by remember { mutableStateOf(false) }
    var showProPlanModal by remember { mutableStateOf(false) }
    var showRatingModal by remember { mutableStateOf(false) }
    var userRating by remember { mutableIntStateOf(5) }

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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // TOPO COM SETA DE VOLTAR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color(0xFF0F172A))
                    }
                    Text(
                        text = "Perfil & Configurações",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // AVATAR CENTRALIZADO COM BADGE DE EDIÇÃO (Idêntico ao Screenshot 3)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6), Color(0xFF10B981))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Letra inicial estilosa ou avatar
                        Text(
                            text = userFullName.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black
                        )

                        // Botão de editar avatar
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Editar Foto",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // NOME COMPLETO EM DESTAQUE (GLEYSON TAVARES DE MIRANDA)
                    Text(
                        text = userFullName.uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = userEmail,
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // BANNER: SEJA PRO (Idêntico ao Screenshot 1 e 3)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { showProPlanModal = true }
                        .shadow(12.dp, RoundedCornerShape(22.dp), spotColor = IosBlue.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF0B132B), Color(0xFF1C2541), Color(0xFF3A0CA3))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SEJA PRO",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF60A5FA),
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Para quem quer conectar mais contas e ter controle total com IA",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFE2E8F0),
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Toque para ver planos e benefícios →",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2 CARDS LADO A LADO: "Grátis / Plano" e "1 Banco / Conexões" (Idêntico ao Screenshot 1 e 3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Card 1: Plano Atual
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showProPlanModal = true },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.verticalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1)))
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Grátis",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Plano Atual",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    // Card 2: Conexões Bancárias
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToOpenFinance() },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.verticalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1)))
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Logo circular do Mercado Pago
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFE600)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("M", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF003B6D))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$connectedBankCount Banco",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Conexões Open Finance",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // CARD PRINCIPAL: "O QUE O FINLINK AI OFERECE" (Com Descrição Completa e Detalhada)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showOfferingsModal = true },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.verticalGradient(listOf(Color(0xFFBBF7D0), Color(0xFF6EE7B7)))
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "O que o FinLink AI oferece",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Veja todos os 8 recursos detalhados do app",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // BOTÃO: AVALIE O FINLINK AI (Idêntico ao Screenshot 1)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showRatingModal = true },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.verticalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1)))
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = IosOrange, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Avalie o FinLink AI",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A)
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // SEÇÃO: GERAL
                SectionHeader(title = "Geral")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.verticalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1)))
                    )
                ) {
                    Column {
                        ProfileMenuRow(icon = Icons.Default.Person, title = "Perfil", onClick = {
                            Toast.makeText(context, "Nome: $userFullName\nE-mail: $userEmail", Toast.LENGTH_LONG).show()
                        })
                        RowDivider()
                        ProfileMenuRow(icon = Icons.Default.Settings, title = "Preferências", onClick = {
                            Toast.makeText(context, "Lembrete diário ativo para as 20:00!", Toast.LENGTH_SHORT).show()
                        })
                        RowDivider()
                        ProfileMenuRow(
                            icon = Icons.Default.CreditCard,
                            title = "Assinatura (${subscriptions.size} ativas)",
                            onClick = { showSubscriptionsModal = true }
                        )
                        RowDivider()
                        ProfileMenuRow(icon = Icons.Default.Key, title = "API Keys", onClick = {
                            Toast.makeText(context, "Google Gemini 3.6 Flash & Supabase ativos!", Toast.LENGTH_SHORT).show()
                        })
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // SEÇÃO: SEGURANÇA
                SectionHeader(title = "Segurança")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.verticalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1)))
                    )
                ) {
                    Column {
                        ProfileMenuRow(icon = Icons.Default.Lock, title = "Alterar senha", onClick = {
                            Toast.makeText(context, "Link de redefinição de senha enviado para $userEmail", Toast.LENGTH_LONG).show()
                        })
                        RowDivider()
                        ProfileMenuRow(icon = Icons.Default.Fingerprint, title = "Biometria", onClick = {
                            Toast.makeText(context, "Desbloqueio biométrico ativo no dispositivo!", Toast.LENGTH_SHORT).show()
                        })
                        RowDivider()
                        ProfileMenuRow(icon = Icons.Default.ReportProblem, title = "Reportar um problema", onClick = {
                            Toast.makeText(context, "Relatório de diagnóstico gerado com sucesso!", Toast.LENGTH_SHORT).show()
                        })
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // SEÇÃO: SUPORTE
                SectionHeader(title = "Suporte")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.verticalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1)))
                    )
                ) {
                    Column {
                        ProfileMenuRow(icon = Icons.Default.SupportAgent, title = "Falar com suporte", onClick = {
                            Toast.makeText(context, "Suporte FinLink AI: suporte@finlink.ai", Toast.LENGTH_LONG).show()
                        })
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // BOTÃO SAIR (Vermelho como no Screenshot 1)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSignOut() },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.verticalGradient(listOf(Color(0xFFFEE2E2), Color(0xFFFECACA)))
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, tint = IosRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Sair da Conta",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = IosRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // MODAL DETALHADO: "O QUE O MEU APP VAI OFERECER" (Todos os 8 recursos detalhados com extrema riqueza)
    if (showOfferingsModal) {
        ModalBottomSheet(
            onDismissRequest = { showOfferingsModal = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "O Que o FinLink AI Oferece",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Tecnologia de ponta para sua saúde financeira",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    IconButton(onClick = { showOfferingsModal = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                // LISTAGEM DOS RECURSOS COM DESCRIÇÃO PROFUNDA
                appOfferings.forEach { feature ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.verticalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1)))
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = feature.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = feature.shortBadge,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF059669),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = feature.fullDescription,
                                fontSize = 13.sp,
                                color = Color(0xFF334155),
                                lineHeight = 19.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // MODAL DE GESTÃO DE ASSINATURAS
    if (showSubscriptionsModal) {
        val totalSubs = subscriptions.sumOf { it.amount }
        ModalBottomSheet(
            onDismissRequest = { showSubscriptionsModal = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Assinaturas & Gastos Fixos",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Total mensal: ${totalSubs.formatBr()}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = IosPurple
                        )
                    }
                    IconButton(onClick = { showSubscriptionsModal = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                subscriptions.forEach { sub ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.verticalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1)))
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sub.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Renovação todo dia ${sub.renewalDay} (${sub.nextRenewalDate}) • ${sub.bankOrCard}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = sub.description,
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = sub.amount.formatBr(),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (sub.status == "Ativa") Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                                    modifier = Modifier.clickable { onToggleSubscription(sub.id) }
                                ) {
                                    Text(
                                        text = sub.status,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (sub.status == "Ativa") Color(0xFF059669) else IosRed,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // MODAL "SEJA PRO" COM DETALHAMENTO DE PLANOS
    if (showProPlanModal) {
        ModalBottomSheet(
            onDismissRequest = { showProPlanModal = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Planos FinLink AI", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                    IconButton(onClick = { showProPlanModal = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                // PLANO GRÁTIS
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Plano Grátis", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("R$ 0,00", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF64748B))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("• Conexão com até 1 banco via Open Finance", fontSize = 13.sp, color = Color(0xFF475569))
                        Text("• Até 15 cupons fiscais escaneados por mês", fontSize = 13.sp, color = Color(0xFF475569))
                        Text("• Extrato e histórico básico de entradas e saídas", fontSize = 13.sp, color = Color(0xFF475569))
                    }
                }

                // PLANO PRO EM DESTAQUE
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("FINLINK PRO", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF60A5FA))
                            Text("R$ 14,90/mês", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF34D399))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("• Bancos ilimitados (Mercado Pago, Nubank, Itaú, etc.)", fontSize = 13.sp, color = Color(0xFFE2E8F0))
                        Text("• Leitor de cupons com IA e SEFAZ ILIMITADO", fontSize = 13.sp, color = Color(0xFFE2E8F0))
                        Text("• Detector avançado de assinaturas e renovações", fontSize = 13.sp, color = Color(0xFFE2E8F0))
                        Text("• Consultor Financeiro Gemini AI 24/7 sem limites", fontSize = 13.sp, color = Color(0xFFE2E8F0))
                        Text("• Planejamento de Orçamento Familiar Inteligente", fontSize = 13.sp, color = Color(0xFFE2E8F0))

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                Toast.makeText(context, "Parabéns! FinLink PRO ativado com 7 dias grátis!", Toast.LENGTH_LONG).show()
                                showProPlanModal = false
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Text("Experimentar 7 Dias Grátis", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // MODAL DE AVALIAÇÃO DO APP
    if (showRatingModal) {
        ModalBottomSheet(
            onDismissRequest = { showRatingModal = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Avalie o FinLink AI", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text("Como tem sido sua experiência com a leitura de cupons e controle financeiro?", fontSize = 13.sp, color = Color(0xFF64748B))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 1..5) {
                        IconButton(onClick = { userRating = i }) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = if (i <= userRating) IosOrange else Color(0xFFCBD5E1),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        Toast.makeText(context, "Obrigado pela sua avaliação de $userRating estrelas!", Toast.LENGTH_SHORT).show()
                        showRatingModal = false
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Enviar Avaliação", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF64748B),
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
    }
}

@Composable
fun RowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFF1F5F9))
            .padding(horizontal = 16.dp)
    )
}
