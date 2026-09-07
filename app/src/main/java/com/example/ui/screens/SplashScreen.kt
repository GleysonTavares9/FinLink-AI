package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosGreen
import com.example.ui.theme.IosIndigo
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // Transição infinita de respiração para a Aurora e o Emblema
    val infiniteTransition = rememberInfiniteTransition(label = "aurora_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    LaunchedEffect(Unit) {
        delay(2000)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F5FA)),
        contentAlignment = Alignment.Center
    ) {
        // Iluminação Aurora Dinâmica e Viva
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Círculo Aurora Azul Elétrico
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0284C7).copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.20f),
                    radius = size.width * 0.80f * pulseScale
                )
            )
            // Círculo Aurora Esmeralda Vibrante
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF10B981).copy(alpha = 0.24f), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.45f),
                    radius = size.width * 0.75f * (2f - pulseScale)
                )
            )
            // Círculo Aurora Índigo/Violeta
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF6366F1).copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(size.width * 0.50f, size.height * 0.82f),
                    radius = size.width * 0.70f * pulseScale
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Cartão de Apresentação em Vidro Branco Translúcido
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .shadow(20.dp, RoundedCornerShape(28.dp), spotColor = Color(0x18000000)),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xF2FFFFFF))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(Color.White, Color(0x20000000))
                            ),
                            shape = RoundedCornerShape(28.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "FinLink AI",
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        color = Color(0xFF1D1D1F),
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Inteligência Financeira em Tempo Real",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Indicador de Carregamento com Barra Gradiente
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFFE5E7EB))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(pulseScale - 0.2f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(IosGreen, IosBlue, IosIndigo)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Pills com os Pilares do FinLink
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SplashFeaturePill(icon = Icons.Default.AccountBalance, label = "Open Finance", color = IosIndigo)
                        SplashFeaturePill(icon = Icons.Default.ReceiptLong, label = "NFC-e SEFAZ", color = IosGreen)
                        SplashFeaturePill(icon = Icons.Default.CloudDone, label = "Nuvem Segura", color = IosBlue)
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = "CARREGANDO SUAS FINANÇAS...",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color(0xFF9CA3AF),
                letterSpacing = 1.2.sp
            )
        }
    }
}

@Composable
private fun SplashFeaturePill(
    icon: ImageVector,
    label: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
