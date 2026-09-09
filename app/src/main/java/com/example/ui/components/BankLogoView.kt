package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Componente que renderiza o logotipo oficial e realista de cada instituição bancária
 * brasileira com alto padrão visual (fidelidade de cores, formas e tipografia).
 */
@Composable
fun BankLogoView(
    bankName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val cleanName = bankName.lowercase()

    when {
        cleanName.contains("nubank") || cleanName.contains("nu pagamentos") || cleanName.contains("nu conta") -> {
            NubankLogo(modifier = modifier, size = size)
        }
        cleanName.contains("itau") || cleanName.contains("itaú") -> {
            ItauLogo(modifier = modifier, size = size)
        }
        cleanName.contains("bradesco") -> {
            BradescoLogo(modifier = modifier, size = size)
        }
        cleanName.contains("banco do brasil") || cleanName.contains("bb") -> {
            BancoDoBrasilLogo(modifier = modifier, size = size)
        }
        cleanName.contains("santander") -> {
            SantanderLogo(modifier = modifier, size = size)
        }
        cleanName.contains("caixa") || cleanName.contains("cef") -> {
            CaixaLogo(modifier = modifier, size = size)
        }
        cleanName.contains("inter") || cleanName.contains("banco inter") -> {
            InterLogo(modifier = modifier, size = size)
        }
        cleanName.contains("c6") -> {
            C6BankLogo(modifier = modifier, size = size)
        }
        cleanName.contains("mercado pago") || cleanName.contains("mercadopago") -> {
            MercadoPagoLogo(modifier = modifier, size = size)
        }
        else -> {
            GenericBankLogo(bankName = bankName, modifier = modifier, size = size)
        }
    }
}

// ==========================================
// 1. NUBANK (Roxo Oficial #820AD1 com "nu")
// ==========================================
@Composable
fun NubankLogo(modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, RoundedCornerShape(size * 0.28f), spotColor = Color(0x55820AD1))
            .clip(RoundedCornerShape(size * 0.28f))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF8A05BE), Color(0xFF6C00A8))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "nu",
            color = Color.White,
            fontSize = (size.value * 0.44f).sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.5).sp,
            fontFamily = FontFamily.SansSerif
        )
    }
}

// ==========================================
// 2. ITAÚ (Laranja #EC7000 com box azul & "itaú")
// ==========================================
@Composable
fun ItauLogo(modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, RoundedCornerShape(size * 0.28f), spotColor = Color(0x44EC7000))
            .clip(RoundedCornerShape(size * 0.28f))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFFF7A00), Color(0xFFEC7000))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.68f)
                .clip(RoundedCornerShape(size * 0.18f))
                .background(Color(0xFF002776)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "itaú",
                color = Color(0xFFFCDB00),
                fontSize = (size.value * 0.26f).sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

// ==========================================
// 3. BRADESCO (Vermelho #CC092F com Arcos)
// ==========================================
@Composable
fun BradescoLogo(modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, RoundedCornerShape(size * 0.28f), spotColor = Color(0x55CC092F))
            .clip(RoundedCornerShape(size * 0.28f))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFE1173F), Color(0xFFB30024))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.65f)) {
            val w = this.size.width
            val h = this.size.height

            drawLine(
                color = Color.White,
                start = Offset(w * 0.5f, h * 0.15f),
                end = Offset(w * 0.5f, h * 0.85f),
                strokeWidth = w * 0.11f,
                cap = StrokeCap.Round
            )

            val leftPath = Path().apply {
                moveTo(w * 0.2f, h * 0.85f)
                cubicTo(w * 0.2f, h * 0.4f, w * 0.45f, h * 0.35f, w * 0.48f, h * 0.35f)
            }
            drawPath(
                path = leftPath,
                color = Color.White,
                style = Stroke(width = w * 0.11f, cap = StrokeCap.Round)
            )

            val rightPath = Path().apply {
                moveTo(w * 0.8f, h * 0.85f)
                cubicTo(w * 0.8f, h * 0.4f, w * 0.55f, h * 0.35f, w * 0.52f, h * 0.35f)
            }
            drawPath(
                path = rightPath,
                color = Color.White,
                style = Stroke(width = w * 0.11f, cap = StrokeCap.Round)
            )
        }
    }
}

// ==========================================
// 4. BANCO DO BRASIL (Azul Marinho & Amarelo Ouro)
// ==========================================
@Composable
fun BancoDoBrasilLogo(modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, RoundedCornerShape(size * 0.28f), spotColor = Color(0x440038A8))
            .clip(RoundedCornerShape(size * 0.28f))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0038A8), Color(0xFF002266))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.65f)) {
            val w = this.size.width
            val h = this.size.height

            val strokeW = w * 0.14f
            val yellow = Color(0xFFFCDB00)

            drawRoundRect(
                color = yellow,
                topLeft = Offset(w * 0.12f, h * 0.22f),
                size = Size(w * 0.5f, h * 0.5f),
                cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
                style = Stroke(width = strokeW)
            )
            drawRoundRect(
                color = yellow,
                topLeft = Offset(w * 0.38f, h * 0.28f),
                size = Size(w * 0.5f, h * 0.5f),
                cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
                style = Stroke(width = strokeW)
            )
        }
    }
}

// ==========================================
// 5. SANTANDER (Vermelho #EC0000 com Chamas)
// ==========================================
@Composable
fun SantanderLogo(modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, RoundedCornerShape(size * 0.28f), spotColor = Color(0x55EC0000))
            .clip(RoundedCornerShape(size * 0.28f))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFEC0000), Color(0xFFBA0000))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.65f)) {
            val w = this.size.width
            val h = this.size.height

            val flameCenter = Path().apply {
                moveTo(w * 0.5f, h * 0.15f)
                cubicTo(w * 0.38f, h * 0.45f, w * 0.42f, h * 0.75f, w * 0.5f, h * 0.85f)
                cubicTo(w * 0.58f, h * 0.75f, w * 0.62f, h * 0.45f, w * 0.5f, h * 0.15f)
            }
            drawPath(path = flameCenter, color = Color.White)

            val flameLeft = Path().apply {
                moveTo(w * 0.35f, h * 0.32f)
                cubicTo(w * 0.22f, h * 0.52f, w * 0.26f, h * 0.72f, w * 0.38f, h * 0.82f)
                cubicTo(w * 0.36f, h * 0.68f, w * 0.42f, h * 0.50f, w * 0.35f, h * 0.32f)
            }
            drawPath(path = flameLeft, color = Color.White)

            val flameRight = Path().apply {
                moveTo(w * 0.65f, h * 0.32f)
                cubicTo(w * 0.78f, h * 0.52f, w * 0.74f, h * 0.72f, w * 0.62f, h * 0.82f)
                cubicTo(w * 0.64f, h * 0.68f, w * 0.58f, h * 0.50f, w * 0.65f, h * 0.32f)
            }
            drawPath(path = flameRight, color = Color.White)
        }
    }
}

// ==========================================
// 6. CAIXA ECONÔMICA (Azul Royal #0066B3 & 'X' Laranja)
// ==========================================
@Composable
fun CaixaLogo(modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, RoundedCornerShape(size * 0.28f), spotColor = Color(0x440066B3))
            .clip(RoundedCornerShape(size * 0.28f))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0066B3), Color(0xFF004982))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.62f)) {
            val w = this.size.width
            val h = this.size.height

            drawLine(
                color = Color.White,
                start = Offset(w * 0.2f, h * 0.2f),
                end = Offset(w * 0.8f, h * 0.8f),
                strokeWidth = w * 0.20f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFF37021),
                start = Offset(w * 0.8f, h * 0.2f),
                end = Offset(w * 0.2f, h * 0.8f),
                strokeWidth = w * 0.20f,
                cap = StrokeCap.Round
            )
        }
    }
}

// ==========================================
// 7. BANCO INTER (Laranja #FF7A00 com 'inter')
// ==========================================
@Composable
fun InterLogo(modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, RoundedCornerShape(size * 0.28f), spotColor = Color(0x55FF7A00))
            .clip(RoundedCornerShape(size * 0.28f))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFFF851B), Color(0xFFFF5E00))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "inter",
            color = Color.White,
            fontSize = (size.value * 0.27f).sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif
        )
    }
}

// ==========================================
// 8. C6 BANK (Grafite Carbono #242424 com 'C6')
// ==========================================
@Composable
fun C6BankLogo(modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, RoundedCornerShape(size * 0.28f), spotColor = Color(0x44000000))
            .clip(RoundedCornerShape(size * 0.28f))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF2C2C2C), Color(0xFF141414))
                )
            )
            .border(0.5.dp, Color(0xFF404040), RoundedCornerShape(size * 0.28f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "C6",
            color = Color.White,
            fontSize = (size.value * 0.40f).sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = (-0.5).sp
        )
    }
}

// ==========================================
// 9. MERCADO PAGO (Azul Celeste #009EE3)
// ==========================================
@Composable
fun MercadoPagoLogo(modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, RoundedCornerShape(size * 0.28f), spotColor = Color(0x44009EE3))
            .clip(RoundedCornerShape(size * 0.28f))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF00A9F4), Color(0xFF0083CA))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.62f)) {
            val w = this.size.width
            val h = this.size.height

            val path1 = Path().apply {
                moveTo(w * 0.15f, h * 0.45f)
                cubicTo(w * 0.35f, h * 0.25f, w * 0.65f, h * 0.45f, w * 0.85f, h * 0.55f)
            }
            drawPath(
                path = path1,
                color = Color.White,
                style = Stroke(width = w * 0.14f, cap = StrokeCap.Round)
            )

            val path2 = Path().apply {
                moveTo(w * 0.25f, h * 0.65f)
                cubicTo(w * 0.45f, h * 0.75f, w * 0.75f, h * 0.55f, w * 0.85f, h * 0.35f)
            }
            drawPath(
                path = path2,
                color = Color(0xFFFFF159),
                style = Stroke(width = w * 0.14f, cap = StrokeCap.Round)
            )
        }
    }
}

// ==========================================
// 10. GENÉRICO ELEGANTE
// ==========================================
@Composable
fun GenericBankLogo(bankName: String, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val initial = bankName.take(2).uppercase().ifBlank { "BK" }
    Box(
        modifier = modifier
            .size(size)
            .shadow(3.dp, RoundedCornerShape(size * 0.28f), spotColor = Color(0x334F46E5))
            .clip(RoundedCornerShape(size * 0.28f))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF4F46E5), Color(0xFF3730A3))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontSize = (size.value * 0.35f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}
