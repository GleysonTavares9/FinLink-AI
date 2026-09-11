package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash_anim")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Varredura suave da lupa (amplitude confortável)
    val magnifierScanX by infiniteTransition.animateFloat(
        initialValue = -40f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "magnifier_x"
    )

    val magnifierAngle by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "magnifier_angle"
    )

    // Flutuação das moedas douradas
    val coinFloat1 by infiniteTransition.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coin1"
    )
    val coinFloat2 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coin2"
    )
    val coinFloat3 by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coin3"
    )

    // Respiração das barras financeiras
    val barProgress by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bars"
    )

    LaunchedEffect(Unit) {
        delay(2300)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFF8FAFD),
                        Color(0xFFF1F5F9)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Aurora Ambiental suave
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF007AFF).copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(size.width * 0.80f, size.height * 0.25f),
                    radius = size.width * 0.85f * pulseScale
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF10B981).copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.20f, size.height * 0.45f),
                    radius = size.width * 0.80f * (2f - pulseScale)
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(size.width * 0.50f, size.height * 0.80f),
                    radius = size.width * 0.70f * pulseScale
                )
            )
        }

        // Conteúdo aberto e espaçoso (não comprimido)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1.1f))

            // Ilustração de Análise Financeira Ampla e Arejada
            FinancialAnalysisAnimatedView(
                magnifierScanX = magnifierScanX,
                magnifierAngle = magnifierAngle,
                coinFloat1 = coinFloat1,
                coinFloat2 = coinFloat2,
                coinFloat3 = coinFloat3,
                barProgress = barProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Nome oficial da aplicação com presença elegante
            Text(
                text = "FinLink AI",
                fontWeight = FontWeight.Black,
                fontSize = 32.sp,
                color = Color(0xFF0F172A),
                letterSpacing = (-0.8).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Gestão Financeira Inteligente",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color(0xFF334155)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "anotações · nfe · open finance",
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Barra de Progresso Ultra-Fina e Discreta (Estilo iOS)
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFE2E8F0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(pulseScale - 0.15f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF007AFF),
                                    Color(0xFF38BDF8),
                                    Color(0xFF10B981)
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Pills de Funcionalidades Oficiais
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SplashFeaturePill(
                    icon = Icons.Default.AccountBalance,
                    label = "Open Finance",
                    color = Color(0xFF007AFF),
                    bgColor = Color(0xFFEBF5FF)
                )
                Spacer(modifier = Modifier.width(8.dp))
                SplashFeaturePill(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    label = "NFC-e SEFAZ",
                    color = Color(0xFF10B981),
                    bgColor = Color(0xFFECFDF5)
                )
                Spacer(modifier = Modifier.width(8.dp))
                SplashFeaturePill(
                    icon = Icons.Default.CloudDone,
                    label = "Nuvem",
                    color = Color(0xFF0284C7),
                    bgColor = Color(0xFFF0F9FF)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "CARREGANDO SUAS FINANÇAS...",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.4.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

/**
 * Desenho vetorial dinâmico nativo e proporcional da animação solicitada:
 * Barras financeiras harmoniosas, moedas flutuantes e Lupa de varredura.
 */
@Composable
private fun FinancialAnalysisAnimatedView(
    magnifierScanX: Float,
    magnifierAngle: Float,
    coinFloat1: Float,
    coinFloat2: Float,
    coinFloat3: Float,
    barProgress: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Linha de base contábil horizontal
            val baseY = h * 0.82f
            val baseStartX = w * 0.15f
            val baseEndX = w * 0.85f

            drawLine(
                color = Color(0xFFCBD5E1),
                start = Offset(baseStartX, baseY),
                end = Offset(baseEndX, baseY),
                strokeWidth = 2.dp.toPx()
            )

            // Linhas de grade tracejadas horizontais
            val grid1Y = baseY - 42.dp.toPx()
            val grid2Y = baseY - 84.dp.toPx()
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)

            drawLine(
                color = Color(0xFFE2E8F0),
                start = Offset(baseStartX, grid1Y),
                end = Offset(baseEndX, grid1Y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect
            )
            drawLine(
                color = Color(0xFFE2E8F0),
                start = Offset(baseStartX, grid2Y),
                end = Offset(baseEndX, grid2Y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect
            )

            // 4 Barras de Gráfico Financeiro (larguras e alturas amplas)
            val barW = 26.dp.toPx()
            val spacing = 14.dp.toPx()
            val totalBarsW = 4 * barW + 3 * spacing
            val startX = (w - totalBarsW) / 2f

            val barHeights = listOf(
                55.dp.toPx() * (0.94f + (barProgress - 1f) * 0.4f),
                85.dp.toPx() * barProgress,
                115.dp.toPx() * (1.06f - (barProgress - 1f) * 0.3f),
                75.dp.toPx() * (0.95f + (barProgress - 1f) * 0.5f)
            )

            val blueGrad = Brush.verticalGradient(
                listOf(Color(0xFF007AFF), Color(0xFF60A5FA))
            )
            val greenGrad = Brush.verticalGradient(
                listOf(Color(0xFF10B981), Color(0xFF34D399))
            )

            for (i in 0 until 4) {
                val bx = startX + i * (barW + spacing)
                val bh = barHeights[i]
                val by = baseY - bh
                val grad = if (i == 2) greenGrad else blueGrad

                drawRoundRect(
                    brush = grad,
                    topLeft = Offset(bx, by),
                    size = Size(barW, bh),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }

            // Moedas Douradas Flutuantes (Apple Amber / Gold)
            // Moeda 1 (esquerda alta)
            val c1x = startX - 34.dp.toPx()
            val c1y = baseY - 75.dp.toPx() + coinFloat1 * 1.6f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFBBF24), Color(0xFFD97706)),
                    center = Offset(c1x, c1y),
                    radius = 14.dp.toPx()
                ),
                center = Offset(c1x, c1y),
                radius = 13.dp.toPx()
            )
            drawCircle(
                color = Color(0xFFFDE68A),
                center = Offset(c1x, c1y),
                radius = 10.dp.toPx(),
                style = Stroke(width = 1.3.dp.toPx())
            )

            // Moeda 2 (direita alta)
            val c2x = startX + totalBarsW + 32.dp.toPx()
            val c2y = baseY - 90.dp.toPx() + coinFloat2 * 1.6f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFBBF24), Color(0xFFD97706)),
                    center = Offset(c2x, c2y),
                    radius = 12.dp.toPx()
                ),
                center = Offset(c2x, c2y),
                radius = 11.dp.toPx()
            )
            drawCircle(
                color = Color(0xFFFDE68A),
                center = Offset(c2x, c2y),
                radius = 8.5.dp.toPx(),
                style = Stroke(width = 1.1.dp.toPx())
            )

            // Moeda 3 (direita baixa)
            val c3x = startX + totalBarsW + 38.dp.toPx()
            val c3y = baseY - 35.dp.toPx() + coinFloat3 * 1.6f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFBBF24), Color(0xFFD97706)),
                    center = Offset(c3x, c3y),
                    radius = 10.dp.toPx()
                ),
                center = Offset(c3x, c3y),
                radius = 9.dp.toPx()
            )

            // Lupa de Análise Financeira (AI Scanner)
            val centerX = w / 2f
            val lx = centerX + 30.dp.toPx() + magnifierScanX * 1.4f
            val ly = baseY - 70.dp.toPx()
            val lensR = 30.dp.toPx()

            // Feixe de luz / Glow da lente sobre o gráfico
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x38007AFF), Color.Transparent),
                    center = Offset(lx, ly),
                    radius = lensR * 1.6f
                ),
                center = Offset(lx, ly),
                radius = lensR * 1.6f
            )

            // Vidro da lente translúcido
            drawCircle(
                color = Color(0x1A007AFF),
                center = Offset(lx, ly),
                radius = lensR
            )

            // Aro metálico da lente (Apple Blue)
            drawCircle(
                color = Color(0xFF007AFF),
                center = Offset(lx, ly),
                radius = lensR,
                style = Stroke(width = 4.5.dp.toPx())
            )

            // Reflexo de luz no topo da lente
            drawCircle(
                color = Color(0x70FFFFFF),
                center = Offset(lx - 7.dp.toPx(), ly - 7.dp.toPx()),
                radius = lensR * 0.45f,
                style = Stroke(width = 1.8.dp.toPx())
            )

            // Cabo da lupa ergonômico
            val handleLen = 25.dp.toPx()
            val rad = Math.toRadians(45.0 + magnifierAngle.toDouble())
            val hStartX = lx + (lensR * Math.cos(rad)).toFloat()
            val hStartY = ly + (lensR * Math.sin(rad)).toFloat()
            val hEndX = hStartX + (handleLen * Math.cos(rad)).toFloat()
            val hEndY = hStartY + (handleLen * Math.sin(rad)).toFloat()

            drawLine(
                color = Color(0xFF0F172A),
                start = Offset(hStartX, hStartY),
                end = Offset(hEndX, hEndY),
                strokeWidth = 7.dp.toPx()
            )
            drawLine(
                color = Color(0xFF007AFF),
                start = Offset(hStartX, hStartY),
                end = Offset(hEndX, hEndY),
                strokeWidth = 3.dp.toPx()
            )
        }
    }
}

@Composable
private fun SplashFeaturePill(
    icon: ImageVector,
    label: String,
    color: Color,
    bgColor: Color
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}
