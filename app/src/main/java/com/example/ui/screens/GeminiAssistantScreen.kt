package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgentChatMessage
import com.example.data.model.AgentChatSession
import com.example.data.model.CategoryItem
import com.example.data.model.DefaultFinancialAgents
import com.example.data.model.FinancialAgentPersona
import com.example.data.model.FinancialWidget
import com.example.data.remote.supabase.AuthUiState
import com.example.ui.viewmodel.ExpenseViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiAssistantScreen(
    viewModel: ExpenseViewModel,
    onBack: () -> Unit
) {
    val messages by viewModel.agentChatMessages.collectAsState()
    val chatSessions by viewModel.agentChatSessions.collectAsState()
    val activePersona by viewModel.activeAgentPersona.collectAsState()
    val isThinking by viewModel.isAgentThinking.collectAsState()
    val searchStatus by viewModel.agentSearchStatus.collectAsState()
    val authState by viewModel.authState.collectAsState()

    // Nome do usuário para a saudação
    val userName = remember(authState) {
        val sessionUser = (authState as? AuthUiState.Authenticated)?.session?.user
        val name = sessionUser?.fullName?.trim()?.ifBlank { null }
            ?: sessionUser?.email?.substringBefore("@")
            ?: "GLEYSON"
        name.split(" ").firstOrNull()?.uppercase(Locale.getDefault()) ?: "GLEYSON"
    }

    var inputPrompt by remember { mutableStateOf("") }
    var showPersonaSelector by remember { mutableStateOf(false) }
    var showProSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Auto-scroll ao receber novas mensagens
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Tela cheia no tema Light com identidade visual Apple iOS do FinLink AI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF8FAFC),
                        Color(0xFFF1F5F9)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 1. Barra Superior Limpa Apple iOS
            AgentTopBar(
                activePersona = activePersona,
                onClose = onBack,
                onNewChat = { viewModel.clearAgentChat() },
                onSelectPersona = { showPersonaSelector = true },
                onHistory = { showHistorySheet = true }
            )

            // 2. Conteúdo Central: Estado Inicial com Mascote Fiscal Inteligente OU Lista de Mensagens
            if (messages.isEmpty() && !isThinking) {
                // Estado Vazio: Mascote de Nota Fiscal & Finanças animado e elegante
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Mascote Fiscal Inteligente do FinLink AI (Cupom Inteligente + Scanner Holográfico)
                        FiscalFinMascot(
                            isAnimated = true,
                            sizeDp = 92.dp
                        )

                        Spacer(Modifier.height(22.dp))

                        Text(
                            text = "Olá, $userName",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            letterSpacing = 0.5.sp
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = "Como posso te ajudar?",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else {
                // Lista de Mensagens estilo Chat com Dados Reais
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(Modifier.height(4.dp)) }

                    itemsIndexed(messages, key = { _, msg -> msg.id }) { index, msg ->
                        if (msg.isUser) {
                            UserMessageBubble(text = msg.text)
                        } else {
                            val isLatest = (index == messages.lastIndex)
                            AgentMessageBlock(
                                message = msg,
                                isLatest = isLatest,
                                onToggleReasoning = { viewModel.toggleReasoningExpanded(msg.id) },
                                onChipClick = { chipText ->
                                    if (chipText.contains("Seja Pro", ignoreCase = true) || chipText.contains("Pro", ignoreCase = true)) {
                                        showProSheet = true
                                    } else {
                                        viewModel.sendAgentUserMessage(chipText)
                                    }
                                }
                            )
                        }
                    }

                    // Animação de Busca Sutil, Elegante e Não Intrusiva
                    if (isThinking) {
                        item {
                            SubtleAgentSearchIndicator(
                                searchStatus = searchStatus,
                                activePersona = activePersona
                            )
                        }
                    }

                    item { Spacer(Modifier.height(8.dp)) }
                }
            }

            // 3. Chips Rápidos de Sugestão (acima da barra de digitação)
            QuickFollowUpChipsBar(
                onChipSelected = { chipText ->
                    if (chipText.contains("Seja Pro", ignoreCase = true)) {
                        showProSheet = true
                    } else {
                        viewModel.sendAgentUserMessage(chipText)
                    }
                }
            )

            // 4. Barra Inferior de Entrada estilo Apple iOS
            AgentBottomInputBar(
                text = inputPrompt,
                onTextChange = { inputPrompt = it },
                onSend = {
                    if (inputPrompt.isNotBlank()) {
                        viewModel.sendAgentUserMessage(inputPrompt)
                        inputPrompt = ""
                    }
                },
                onAddClick = {
                    viewModel.sendAgentUserMessage("Supermercado nos últimos 3 meses")
                }
            )
        }

        // Modal de Seleção de Especialista / Persona do Agente
        if (showPersonaSelector) {
            ModalBottomSheet(
                onDismissRequest = { showPersonaSelector = false },
                containerColor = Color(0xFFFFFFFF),
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                PersonaSelectorSheet(
                    currentPersona = activePersona,
                    onSelect = { persona ->
                        viewModel.selectAgentPersona(persona)
                        showPersonaSelector = false
                    }
                )
            }
        }

        // Modal do FinLink Pro (Links funcionais dentro do chat)
        if (showProSheet) {
            ModalBottomSheet(
                onDismissRequest = { showProSheet = false },
                containerColor = Color(0xFFFFFFFF),
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                FinLinkProSheet(
                    onDismiss = { showProSheet = false }
                )
            }
        }

        // Modal de Histórico Real de Conversas
        if (showHistorySheet) {
            ModalBottomSheet(
                onDismissRequest = { showHistorySheet = false },
                containerColor = Color(0xFFFFFFFF),
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                RealChatHistorySheet(
                    sessions = chatSessions,
                    onSelectSession = { session ->
                        viewModel.loadChatSession(session)
                        showHistorySheet = false
                    },
                    onDeleteSession = { sessionId ->
                        viewModel.deleteChatSession(sessionId)
                    },
                    onClearCurrentChat = {
                        viewModel.clearAgentChat()
                        showHistorySheet = false
                    }
                )
            }
        }
    }
}

/**
 * Mascote Fiscal & Financeiro do FinLink AI (Cupom Fiscal Inteligente + Scanner Holográfico)
 * Design moderno em cores vivas da plataforma (Apple Blue, Emerald e Cyan). ZERO PRETO!
 */
@Composable
fun FiscalFinMascot(
    modifier: Modifier = Modifier,
    isAnimated: Boolean = true,
    sizeDp: Dp = 92.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fiscal_mascot")

    // Flutuação suave (levitação)
    val floatY by if (isAnimated) {
        infiniteTransition.animateFloat(
            initialValue = -4f,
            targetValue = 4f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "levitate"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    // Feixe de varredura fiscal laser suave (scanner de cupom)
    val scanProgress by if (isAnimated) {
        infiniteTransition.animateFloat(
            initialValue = 0.20f,
            targetValue = 0.82f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scan_laser"
        )
    } else {
        remember { mutableStateOf(0.5f) }
    }

    // Brilho dos olhinhos digitais inteligentes
    val eyeBlinkScale by if (isAnimated) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 0.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(2800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "eye_blink"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    // Rotação suave da moeda de finanças flutuante
    val coinOffset by if (isAnimated) {
        infiniteTransition.animateFloat(
            initialValue = -3f,
            targetValue = 3f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200),
                repeatMode = RepeatMode.Reverse
            ),
            label = "coin_float"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .offset(y = floatY.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val strokePx = 2.4f * density

            // Cores da plataforma FinLink AI (sem preto)
            val primaryBlue = Color(0xFF007AFF)
            val vibrantCyan = Color(0xFF06B6D4)
            val emeraldGreen = Color(0xFF10B981)
            val softAmber = Color(0xFFF59E0B)

            // 1. Silhueta do Cupom Fiscal Inteligente (com topo arredondado e base denteada de nota fiscal)
            val ticketLeft = w * 0.22f
            val ticketRight = w * 0.78f
            val ticketTop = h * 0.12f
            val ticketBottom = h * 0.88f

            val receiptPath = Path().apply {
                // Topo arredondado
                moveTo(ticketLeft + 12f, ticketTop)
                lineTo(ticketRight - 12f, ticketTop)
                cubicTo(ticketRight, ticketTop, ticketRight, ticketTop, ticketRight, ticketTop + 12f)

                // Lado Direito reto
                lineTo(ticketRight, ticketBottom)

                // Base Denteada / Serrilhada clássica de Cupom Fiscal (Zig-zag)
                val teethCount = 5
                val teethWidth = (ticketRight - ticketLeft) / teethCount
                for (i in 0 until teethCount) {
                    val toothStartX = ticketRight - (i * teethWidth)
                    val toothMidX = toothStartX - (teethWidth / 2f)
                    val toothEndX = toothStartX - teethWidth
                    lineTo(toothMidX, ticketBottom - 6f)
                    lineTo(toothEndX, ticketBottom)
                }

                // Lado Esquerdo reto subindo
                lineTo(ticketLeft, ticketTop + 12f)
                cubicTo(ticketLeft, ticketTop, ticketLeft, ticketTop, ticketLeft + 12f, ticketTop)
                close()
            }

            // Fundo perolado do Cupom com gradiente suave
            drawPath(
                path = receiptPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF8FAFC),
                        Color(0xFFF0FDF4)
                    )
                ),
                style = Fill
            )

            // Contorno elegante em degradê Apple Blue e Cyan
            drawPath(
                path = receiptPath,
                brush = Brush.linearGradient(
                    colors = listOf(primaryBlue, vibrantCyan),
                    start = Offset(ticketLeft, ticketTop),
                    end = Offset(ticketRight, ticketBottom)
                ),
                style = Stroke(
                    width = strokePx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // 2. Selo Holográfico de NF no Topo (Dobra de segurança)
            drawCircle(
                color = emeraldGreen,
                radius = 4.dp.toPx(),
                center = Offset((ticketLeft + ticketRight) / 2f, ticketTop + 9.dp.toPx())
            )

            // 3. Olhos Digitais Expressivos (Visor Inteligente)
            val eyesY = h * 0.38f
            val eyeRadiusX = 4.dp.toPx()
            val eyeRadiusY = 5.dp.toPx() * eyeBlinkScale

            // Olho Esquerdo
            drawOval(
                color = primaryBlue,
                topLeft = Offset(w * 0.37f - eyeRadiusX, eyesY - eyeRadiusY),
                size = Size(eyeRadiusX * 2, eyeRadiusY * 2)
            )
            // Brilho do olho esquerdo
            drawCircle(
                color = Color.White,
                radius = 1.3f.dp.toPx(),
                center = Offset(w * 0.37f - 1.dp.toPx(), eyesY - 1.dp.toPx())
            )

            // Olho Direito
            drawOval(
                color = primaryBlue,
                topLeft = Offset(w * 0.63f - eyeRadiusX, eyesY - eyeRadiusY),
                size = Size(eyeRadiusX * 2, eyeRadiusY * 2)
            )
            // Brilho do olho direito
            drawCircle(
                color = Color.White,
                radius = 1.3f.dp.toPx(),
                center = Offset(w * 0.63f - 1.dp.toPx(), eyesY - 1.dp.toPx())
            )

            // Sorriso amigável
            val smilePath = Path().apply {
                moveTo(w * 0.46f, h * 0.48f)
                cubicTo(w * 0.48f, h * 0.52f, w * 0.52f, h * 0.52f, w * 0.54f, h * 0.48f)
            }
            drawPath(
                path = smilePath,
                color = primaryBlue,
                style = Stroke(width = 1.6f * density, cap = StrokeCap.Round)
            )

            // 4. Linhas micro-fiscais do cupom (Representando itens da NF)
            val lineY1 = h * 0.58f
            val lineY2 = h * 0.66f
            val lineY3 = h * 0.74f

            drawLine(
                color = Color(0xFFCBD5E1),
                start = Offset(ticketLeft + 14.dp.toPx(), lineY1),
                end = Offset(ticketRight - 14.dp.toPx(), lineY1),
                strokeWidth = 1.6f * density,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFE2E8F0),
                start = Offset(ticketLeft + 14.dp.toPx(), lineY2),
                end = Offset(ticketRight - 22.dp.toPx(), lineY2),
                strokeWidth = 1.6f * density,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFE2E8F0),
                start = Offset(ticketLeft + 14.dp.toPx(), lineY3),
                end = Offset(ticketRight - 18.dp.toPx(), lineY3),
                strokeWidth = 1.6f * density,
                cap = StrokeCap.Round
            )

            // 5. Feixe de Scanner Laser Holográfico Suave (Auditando NF em tempo real)
            val scanY = h * scanProgress
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        emeraldGreen.copy(alpha = 0.85f),
                        vibrantCyan.copy(alpha = 0.95f),
                        emeraldGreen.copy(alpha = 0.85f),
                        Color.Transparent
                    )
                ),
                start = Offset(ticketLeft - 4.dp.toPx(), scanY),
                end = Offset(ticketRight + 4.dp.toPx(), scanY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // 6. Moedinha Financeira Flutuante (% / Finanças) orbitando
            val coinCenterX = ticketRight + 8.dp.toPx()
            val coinCenterY = h * 0.35f + coinOffset.dp.toPx()

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(softAmber, Color(0xFFD97706)),
                    center = Offset(coinCenterX, coinCenterY),
                    radius = 8.dp.toPx()
                ),
                radius = 7.dp.toPx(),
                center = Offset(coinCenterX, coinCenterY)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = 5.dp.toPx(),
                center = Offset(coinCenterX, coinCenterY),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

/**
 * Animação de Busca Sutil, Elegante e Refinada
 */
@Composable
private fun SubtleAgentSearchIndicator(
    searchStatus: String,
    activePersona: FinancialAgentPersona
) {
    val infiniteTransition = rememberInfiniteTransition(label = "search_shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF007AFF).copy(alpha = shimmerAlpha),
                        Color(0xFF10B981).copy(alpha = shimmerAlpha)
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(17.dp),
            strokeWidth = 2.2.dp,
            color = Color(0xFF007AFF)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = searchStatus.ifBlank { "Auditando dados fiscais com ${activePersona.name}..." },
            color = Color(0xFF0F172A),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Barra Superior com Close (X), Agente Ativo e Ações
 */
@Composable
private fun AgentTopBar(
    activePersona: FinancialAgentPersona,
    onClose: () -> Unit,
    onNewChat: () -> Unit,
    onSelectPersona: () -> Unit,
    onHistory: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Botão Fechar (X)
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9))
                    .border(1.dp, Color(0xFFE2E8F0), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Fechar",
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Título central com Especialista Ativo e Indicador de Online
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .clickable { onSelectPersona() }
                .background(Color(0xFFF8FAFC))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981))
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = activePersona.name,
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = activePersona.shortRole,
                    color = Color(0xFF64748B),
                    fontSize = 10.sp
                )
            }
        }

        // Botão de Histórico e Novo Chat
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onNewChat,
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Novo Chat",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            IconButton(
                onClick = onHistory,
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Histórico Real",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Balão de Mensagem do Usuário (Apple Blue com texto branco)
 */
@Composable
private fun UserMessageBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clip(RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp))
                .background(Color(0xFF007AFF))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Bloco de Resposta do Agente contendo:
 * 1. Raciocínio recolhível ("Concluí 3 etapas >")
 * 2. Visual Rich Widgets (Breakdown ou Tabela)
 * 3. Texto analítico resumido com dados reais e formatação nítida
 * 4. Chips de ação funcionais
 */
@Composable
private fun AgentMessageBlock(
    message: AgentChatMessage,
    isLatest: Boolean = false,
    onToggleReasoning: () -> Unit,
    onChipClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        // 1. Acordeão de Raciocínio das LLMs ("Concluí X etapas >")
        if (message.reasoningSteps.isNotEmpty()) {
            ReasoningAccordion(
                stepCount = message.reasoningSteps.size,
                steps = message.reasoningSteps,
                isExpanded = message.isReasoningExpanded,
                onToggle = onToggleReasoning
            )
            Spacer(Modifier.height(12.dp))
        }

        // 2. Visual Rich Widgets em Tema Claro
        when (val widget = message.richWidget) {
            is FinancialWidget.CategoryBreakdown -> {
                CategoryBreakdownCard(
                    widget = widget,
                    onCategoryClick = { catName ->
                        onChipClick("Detalhar gastos de $catName")
                    }
                )
                Spacer(Modifier.height(12.dp))
            }
            is FinancialWidget.MonthlyComparison -> {
                MonthlyComparisonTableCard(widget = widget)
                Spacer(Modifier.height(12.dp))
            }
            null -> {}
        }

        // 3. Texto Analítico com Efeito Digitado e Formatação Organizada (sem texto bruto)
        if (message.text.isNotBlank()) {
            TypewriterOrganizedMessageCard(
                fullText = message.text,
                isLatest = isLatest,
                accentColor = Color(message.agentPersona?.accentColorHex ?: 0xFF007AFF)
            )
        }

        // 4. Follow-up Chips clicáveis
        if (message.followUpChips.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(message.followUpChips) { chip ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (chip.contains("Pro", ignoreCase = true)) Color(0xFFEFF6FF) else Color.White
                            )
                            .border(
                                1.dp,
                                if (chip.contains("Pro", ignoreCase = true)) Color(0xFF007AFF) else Color(0xFFCBD5E1),
                                RoundedCornerShape(18.dp)
                            )
                            .clickable { onChipClick(chip) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = chip,
                            color = if (chip.contains("Pro", ignoreCase = true)) Color(0xFF007AFF) else Color(0xFF1E293B),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Acordeão de Raciocínio das LLMs: "Concluí 3 etapas >"
 */
@Composable
private fun ReasoningAccordion(
    stepCount: Int,
    steps: List<String>,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onToggle() }
                .background(Color(0xFFF1F5F9))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                .padding(vertical = 6.dp, horizontal = 10.dp)
        ) {
            Text(
                text = "Concluí $stepCount etapas",
                color = Color(0xFF475569),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier
                    .size(15.dp)
                    .rotate(if (isExpanded) 90f else 0f)
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                steps.forEachIndexed { idx, step ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFECFDF5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = step,
                            color = Color(0xFF475569),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Widget 1: Detalhamento por Categoria com Dados Reais
 */
@Composable
private fun CategoryBreakdownCard(
    widget: FinancialWidget.CategoryBreakdown,
    onCategoryClick: (String) -> Unit
) {
    val ptBr = Locale.forLanguageTag("pt-BR")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header do Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = widget.title,
                    color = Color(0xFF0F172A),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = widget.period,
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "R$ ${String.format(ptBr, "%,.2f", widget.totalAmount)}",
                color = Color(0xFF0F172A),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(Modifier.height(16.dp))

            // Barra de Progresso Multi-Segmentada
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                widget.categories.forEach { cat ->
                    val weight = (cat.percentage.coerceAtLeast(4)).toFloat()
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .height(8.dp)
                            .background(Color(cat.colorHex))
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Lista de Categorias
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                widget.categories.forEach { cat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onCategoryClick(cat.name) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(cat.colorHex).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(cat.iconType),
                                    contentDescription = null,
                                    tint = Color(cat.colorHex),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = cat.name,
                                    color = Color(0xFF0F172A),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${cat.percentage}% dos gastos",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "R$ ${String.format(ptBr, "%,.2f", cat.amount)}",
                                color = Color(0xFF0F172A),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Origem dos Gastos (Cartão vs Conta)
            Text(
                text = "Origem dos gastos",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                widget.origins.forEach { origin ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(origin.colorHex))
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = origin.label,
                                color = Color(0xFF475569),
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = "R$ ${String.format(ptBr, "%,.2f", origin.amount)}",
                            color = Color(0xFF0F172A),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Nota de Sincronização
            Text(
                text = "⚡ ${widget.syncTimestamp}",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
    }
}

/**
 * Widget 2: Tabela de Comparação Mensal em Tema Claro
 */
@Composable
private fun MonthlyComparisonTableCard(widget: FinancialWidget.MonthlyComparison) {
    val ptBr = Locale.forLanguageTag("pt-BR")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = widget.title,
                    color = Color(0xFF0F172A),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = widget.period,
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            // Cabeçalho da Tabela
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Categoria",
                    color = Color(0xFF475569),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1.3f)
                )
                widget.months.forEach { month ->
                    Text(
                        text = month,
                        color = Color(0xFF475569),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Linhas de Dados
            widget.rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = row.category,
                        color = Color(0xFF0F172A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1.3f)
                    )
                    row.values.forEach { amount ->
                        Text(
                            text = "R$ ${String.format(ptBr, "%,.0f", amount)}",
                            color = Color(0xFF0F172A),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "⚡ ${widget.syncTimestamp}",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Barra Horizontal de Chips Rápidos de Ação
 */
@Composable
private fun QuickFollowUpChipsBar(
    onChipSelected: (String) -> Unit
) {
    val quickChips = listOf(
        "🚀 Seja Pro",
        "E o CDB FinLink?",
        "Supermercado nos últimos 3 meses",
        "Auditar cupons fiscais",
        "Como estão meus gastos este mês?",
        "Comparar gastos com o mês anterior"
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        items(quickChips) { chip ->
            val isPro = chip.contains("Seja Pro", ignoreCase = true)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isPro) Color(0xFFEFF6FF) else Color.White)
                    .border(
                        1.dp,
                        if (isPro) Color(0xFF007AFF) else Color(0xFFE2E8F0),
                        RoundedCornerShape(18.dp)
                    )
                    .clickable { onChipSelected(chip) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = chip,
                    color = if (isPro) Color(0xFF007AFF) else Color(0xFF1E293B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Barra Inferior de Digitação estilo Apple iOS
 */
@Composable
private fun AgentBottomInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Botão (+)
        IconButton(onClick = onAddClick) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Adicionar",
                    tint = Color(0xFF334155),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.width(6.dp))

        // Campo de Texto estilo Pill Apple iOS
        Box(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFFF1F5F9))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(22.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (text.isEmpty()) {
                Text(
                    text = "Envie uma mensagem",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp),
                cursorBrush = SolidColor(Color(0xFF007AFF)),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() })
            )
        }

        Spacer(Modifier.width(6.dp))

        // Botão de Enviar ou Microfone
        IconButton(
            onClick = {
                if (text.isNotBlank()) onSend()
            }
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (text.isNotBlank()) Color(0xFF007AFF) else Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (text.isNotBlank()) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                    contentDescription = if (text.isNotBlank()) "Enviar" else "Microfone",
                    tint = if (text.isNotBlank()) Color.White else Color(0xFF64748B),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

/**
 * Modal BottomSheet do FinLink Pro (Links funcionais dentro do chat)
 */
@Composable
private fun FinLinkProSheet(
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .padding(bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFFEFF6FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "FinLink AI Pro 💎",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Desbloqueie auditoria ilimitada de cupons fiscais e análises preditivas com IA.",
            fontSize = 13.sp,
            color = Color(0xFF64748B),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ProBenefitRow("⚡ Consultas e auditorias fiscais ilimitadas")
            ProBenefitRow("📊 Análise preditiva em tempo real de faturas de cartão")
            ProBenefitRow("🏦 Conexões bancárias Open Finance sem restrições")
            ProBenefitRow("📱 Alertas inteligentes de contenção de desperdícios")
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
        ) {
            Text(
                text = "Experimentar 7 dias grátis",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 15.sp
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Depois R$ 19,90/mês. Cancele quando quiser.",
            fontSize = 11.sp,
            color = Color(0xFF94A3B8)
        )
    }
}

@Composable
private fun ProBenefitRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Color(0xFFECFDF5)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(13.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            color = Color(0xFF334155),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Modal BottomSheet para Histórico Real de Conversas
 */
@Composable
private fun RealChatHistorySheet(
    sessions: List<AgentChatSession>,
    onSelectSession: (AgentChatSession) -> Unit,
    onDeleteSession: (String) -> Unit,
    onClearCurrentChat: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .padding(bottom = 28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Histórico de Conversas",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            if (sessions.isNotEmpty()) {
                Text(
                    text = "${sessions.size} conversas",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhuma conversa gravada ainda.\nEnvie uma mensagem para iniciar seu histórico.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sessions.forEach { session ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                            .clickable { onSelectSession(session) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = Color(0xFF007AFF),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = session.title,
                                    color = Color(0xFF0F172A),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${session.dateFormatted} • ${session.messages.size} mensagens",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = { onDeleteSession(session.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Excluir",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = onClearCurrentChat,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2))
        ) {
            Text(
                text = "Limpar conversa ativa",
                color = Color(0xFFDC2626),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Modal BottomSheet para Seleção dos Especialistas em Tema Claro
 */
@Composable
private fun PersonaSelectorSheet(
    currentPersona: FinancialAgentPersona,
    onSelect: (FinancialAgentPersona) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Especialistas Fiscais & Financeiros",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )
        Text(
            text = "Selecione o agente para direcionar suas análises:",
            fontSize = 12.sp,
            color = Color(0xFF64748B)
        )

        Spacer(Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DefaultFinancialAgents.list.forEach { persona ->
                val isSelected = persona.id == currentPersona.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) Color(0xFFF1F5F9) else Color.White)
                        .border(
                            1.dp,
                            if (isSelected) Color(persona.accentColorHex) else Color(0xFFE2E8F0),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelect(persona) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = persona.iconEmoji, fontSize = 22.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = persona.name,
                            color = Color(0xFF0F172A),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = persona.shortRole,
                            color = Color(persona.accentColorHex),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = persona.description,
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            maxLines = 2
                        )
                    }
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(persona.accentColorHex)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getCategoryIcon(type: String): ImageVector {
    return when (type.lowercase()) {
        "restaurant" -> Icons.Default.Restaurant
        "shopping_bag" -> Icons.Default.ShoppingBag
        "shopping_cart" -> Icons.Default.ShoppingCart
        "phone" -> Icons.Default.PhoneAndroid
        "car" -> Icons.Default.DirectionsCar
        else -> Icons.Default.CreditCard
    }
}

/**
 * Card de Mensagem com Efeito Digitador (Typewriter) e Formatação Organizada:
 * - O texto é digitado progressivamente com cadência suave estilo LLM (ChatGPT/Claude/Gemini).
 * - O usuário pode tocar no card para completar instantaneamente.
 * - Converte Markdown bruto (###, **, •, R$) em componentes visuais limpos e elegantes.
 */
@Composable
private fun TypewriterOrganizedMessageCard(
    fullText: String,
    isLatest: Boolean,
    accentColor: Color
) {
    // Se for a última mensagem recebida, anima a digitação gradualmente
    var displayedLength by remember(fullText) {
        mutableIntStateOf(if (isLatest) 0 else fullText.length)
    }
    var isTyping by remember(fullText) {
        mutableStateOf(isLatest && fullText.isNotEmpty())
    }

    LaunchedEffect(fullText, isLatest) {
        if (isLatest && displayedLength < fullText.length) {
            isTyping = true
            while (displayedLength < fullText.length) {
                delay(10) // Cadência suave e natural de digitação
                val chunk = if (fullText.length - displayedLength > 5) (3..6).random() else 1
                displayedLength = (displayedLength + chunk).coerceAtMost(fullText.length)
            }
            isTyping = false
        } else {
            displayedLength = fullText.length
            isTyping = false
        }
    }

    val currentText = fullText.take(displayedLength)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
            .clickable {
                // Toque no card conclui a digitação imediatamente
                if (isTyping) {
                    displayedLength = fullText.length
                    isTyping = false
                }
            }
            .padding(18.dp)
    ) {
        OrganizedMarkdownRenderer(
            text = currentText,
            isTyping = isTyping,
            accentColor = accentColor
        )
    }
}

/**
 * Renderizador de Conteúdo Estruturado & Organizado (sem caracteres brutos de markdown)
 */
@Composable
private fun OrganizedMarkdownRenderer(
    text: String,
    isTyping: Boolean,
    accentColor: Color
) {
    val lines = remember(text) { text.split("\n") }

    // Animação suave de cursor piscante
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        var idx = 0
        while (idx < lines.size) {
            val line = lines[idx].trim()

            when {
                line.isBlank() -> {
                    // Ignora linhas vazias repetidas
                }

                // 1. Títulos de Seção (### ou ##)
                line.startsWith("### ") || line.startsWith("## ") -> {
                    val rawTitle = line.removePrefix("### ").removePrefix("## ").trim()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(accentColor)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = rawTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            letterSpacing = 0.2.sp
                        )
                    }
                }

                // 2. Alertas & Destaques (⚠️, 🚨, ✅, 💡)
                line.startsWith("⚠️") || line.startsWith("🚨") -> {
                    AlertCapsule(
                        text = line,
                        bgColor = Color(0xFFFFFBEB),
                        borderColor = Color(0xFFFDE68A),
                        textColor = Color(0xFF92400E),
                        accentColor = Color(0xFFD97706)
                    )
                }
                line.startsWith("✅") -> {
                    AlertCapsule(
                        text = line,
                        bgColor = Color(0xFFF0FDF4),
                        borderColor = Color(0xFFBBF7D0),
                        textColor = Color(0xFF166534),
                        accentColor = Color(0xFF16A34A)
                    )
                }
                line.startsWith("💡") -> {
                    AlertCapsule(
                        text = line,
                        bgColor = Color(0xFFEFF6FF),
                        borderColor = Color(0xFFBFDBFE),
                        textColor = Color(0xFF1E40AF),
                        accentColor = Color(0xFF3B82F6)
                    )
                }

                // 3. Bullet Points (• ou - ou *)
                line.startsWith("• ") || line.startsWith("- ") || line.startsWith("* ") -> {
                    val bulletContent = line.substring(2).trim()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp, end = 10.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            FormattedAnnotatedText(
                                rawText = bulletContent,
                                baseColor = Color(0xFF334155),
                                accentColor = accentColor
                            )
                        }
                    }
                }

                // 4. Itens numerados (1. , 2. , etc.)
                line.length > 3 && line[0].isDigit() && (line[1] == '.' || line[2] == '.') -> {
                    val dotIdx = line.indexOf('.')
                    val num = line.substring(0, dotIdx).trim()
                    val rest = line.substring(dotIdx + 1).trim()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp, end = 10.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(accentColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = num,
                                color = accentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            FormattedAnnotatedText(
                                rawText = rest,
                                baseColor = Color(0xFF334155),
                                accentColor = accentColor
                            )
                        }
                    }
                }

                // 5. Parágrafo comum
                else -> {
                    FormattedAnnotatedText(
                        rawText = line,
                        baseColor = Color(0xFF334155),
                        accentColor = accentColor
                    )
                }
            }
            idx++
        }

        // Cursor digitador piscante ao final da mensagem enquanto digita
        if (isTyping) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 8.dp, height = 15.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accentColor.copy(alpha = cursorAlpha))
                )
            }
        }
    }
}

/**
 * Formatação de Texto com remoção limpa de markdown bruto e estilização elegante
 */
@Composable
private fun FormattedAnnotatedText(
    rawText: String,
    baseColor: Color = Color(0xFF334155),
    accentColor: Color = Color(0xFF007AFF)
) {
    val annotated = remember(rawText) {
        parseMarkdownInline(rawText, baseColor, accentColor)
    }
    Text(
        text = annotated,
        fontSize = 14.sp,
        lineHeight = 22.sp
    )
}

private fun parseMarkdownInline(
    text: String,
    baseColor: Color,
    accentColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val len = text.length

        while (cursor < len) {
            // Verifica negrito **
            if (cursor + 1 < len && text[cursor] == '*' && text[cursor + 1] == '*') {
                val endBold = text.indexOf("**", cursor + 2)
                if (endBold != -1) {
                    val boldContent = text.substring(cursor + 2, endBold)
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))) {
                        append(boldContent)
                    }
                    cursor = endBold + 2
                    continue
                }
            }

            // Verifica itálico *(...) ou *...*
            if (text[cursor] == '*' && (cursor + 1 >= len || text[cursor + 1] != '*')) {
                val endItalic = text.indexOf('*', cursor + 1)
                if (endItalic != -1) {
                    val italicContent = text.substring(cursor + 1, endItalic)
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Color(0xFF64748B))) {
                        append(italicContent)
                    }
                    cursor = endItalic + 1
                    continue
                }
            }

            // Caractere normal
            withStyle(SpanStyle(color = baseColor, fontWeight = FontWeight.Normal)) {
                append(text[cursor])
            }
            cursor++
        }
    }
}

/**
 * Cápsula de Alerta / Dica Elegante
 */
@Composable
private fun AlertCapsule(
    text: String,
    bgColor: Color,
    borderColor: Color,
    textColor: Color,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            val icon = when {
                text.startsWith("⚠️") -> "⚠️"
                text.startsWith("🚨") -> "🚨"
                text.startsWith("✅") -> "✅"
                text.startsWith("💡") -> "💡"
                else -> "📌"
            }
            Text(
                text = icon,
                fontSize = 15.sp,
                modifier = Modifier.padding(end = 8.dp, top = 1.dp)
            )
            val cleanText = text
                .removePrefix("⚠️").removePrefix("🚨")
                .removePrefix("✅").removePrefix("💡").trim()

            FormattedAnnotatedText(
                rawText = cleanText,
                baseColor = textColor,
                accentColor = accentColor
            )
        }
    }
}
