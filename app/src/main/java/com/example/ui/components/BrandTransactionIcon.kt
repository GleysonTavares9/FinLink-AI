package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Ícone estilizado ultra-realista para transações financeiras (Estilo Pierre App),
 * com logos icônicos de marcas (Netflix, Spotify, Google, etc.) e mini badge do banco emissor.
 */
@Composable
fun BrandTransactionIcon(
    title: String,
    category: String,
    bankName: String,
    isIncome: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp
) {
    val t = title.lowercase(Locale.ROOT)
    val c = category.lowercase(Locale.ROOT)

    Box(modifier = modifier.size(size)) {
        // Ícone Principal de Marca ou Categoria
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    when {
                        t.contains("netflix") -> Color(0xFF0F0F0F)
                        t.contains("spotify") -> Color(0xFF1DB954)
                        t.contains("google") -> Color(0xFF1E293B)
                        t.contains("mercadolivre") || t.contains("mercado livre") -> Color(0xFFFFF159)
                        t.contains("amazon") || t.contains("prime") -> Color(0xFF131921)
                        t.contains("drogaria") || t.contains("farm") || c.contains("pharmacy") || c.contains("farmácia") -> Color(0xFF1E293B)
                        t.contains("supermercado") || t.contains("epa") || t.contains("carrefour") || t.contains("bh") -> Color(0xFF1E293B)
                        t.contains("uber") || t.contains("99") -> Color(0xFF111111)
                        isIncome -> Color(0xFF1E293B)
                        else -> Color(0xFF1E293B) // Fundo dark grafite estilo Pierre
                    }
                )
                .border(1.dp, Color(0xFF334155).copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when {
                // NETFLIX: O lendário "N" vermelho da Netflix
                t.contains("netflix") -> {
                    NetflixIcon(modifier = Modifier.size(size * 0.58f))
                }

                // SPOTIFY: Fundo verde e ondas sonoras
                t.contains("spotify") -> {
                    SpotifyIcon(modifier = Modifier.size(size * 0.60f))
                }

                // GOOGLE: Letra "G" clássica de quatro cores ou design Google
                t.contains("google") -> {
                    GoogleIcon(modifier = Modifier.size(size * 0.58f))
                }

                // FARMÁCIA / SAÚDE: Ícone ciano/verde água estilizado
                t.contains("drogaria") || t.contains("farm") || c.contains("pharmacy") || c.contains("farmácia") -> {
                    Icon(
                        imageVector = Icons.Default.LocalPharmacy,
                        contentDescription = null,
                        tint = Color(0xFF2DD4BF), // Ciano esmeralda idêntico ao Pierre
                        modifier = Modifier.size(size * 0.52f)
                    )
                }

                // SUPERMERCADO / ALIMENTAÇíO
                t.contains("supermercado") || t.contains("epa") || t.contains("carrefour") || t.contains("bh") || c.contains("groceries") -> {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(size * 0.52f)
                    )
                }

                // RESTAURANTE
                c.contains("eating out") || c.contains("restaurante") -> {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = Color(0xFFF43F5E),
                        modifier = Modifier.size(size * 0.52f)
                    )
                }

                // COMPRAS / VESTUÁRIO
                c.contains("shopping") || c.contains("clothing") || t.contains("mercadolivre") -> {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = if (t.contains("mercadolivre")) Color(0xFF2D3277) else Color(0xFFA855F7),
                        modifier = Modifier.size(size * 0.52f)
                    )
                }

                // TRANSFERÊNCIA / PIX / RESERVAS / DINHEIRO (Ícone de cédula estilo Pierre)
                else -> {
                    CashBillIcon(
                        tint = if (isIncome) Color(0xFF34D399) else Color(0xFF94A3B8),
                        modifier = Modifier.size(size * 0.54f)
                    )
                }
            }
        }

        // Mini Badge da Instituição Financeira no Canto Inferior Direito (estilo Pierre)
        BankMiniBadge(
            bankName = bankName,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 2.dp, y = 2.dp)
        )
    }
}

/**
 * Logo do Netflix em Canvas ("N" Vermelho estilizado com sombra e curva)
 */
@Composable
fun NetflixIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val colRedDark = Color(0xFFB81D24)
        val colRedBright = Color(0xFFE50914)
        val colRedMid = Color(0xFFD81F26)

        val barW = w * 0.28f

        // Barra esquerda (reta)
        drawRect(
            color = colRedDark,
            topLeft = Offset(0f, 0f),
            size = Size(barW, h)
        )

        // Barra direita (reta)
        drawRect(
            color = colRedDark,
            topLeft = Offset(w - barW, 0f),
            size = Size(barW, h)
        )

        // Barra diagonal (com degradê para efeito 3D característico da Netflix)
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(barW, 0f)
            lineTo(w, h)
            lineTo(w - barW, h)
            close()
        }
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(colRedBright, colRedMid, colRedBright),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )
    }
}

/**
 * Logo do Spotify em Canvas (Círculo com três ondas pretas)
 */
@Composable
fun SpotifyIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.12f

        // 3 arcos sonoros
        val path1 = Path().apply {
            moveTo(w * 0.22f, h * 0.35f)
            quadraticTo(w * 0.50f, h * 0.23f, w * 0.78f, h * 0.35f)
        }
        val path2 = Path().apply {
            moveTo(w * 0.26f, h * 0.52f)
            quadraticTo(w * 0.50f, h * 0.42f, w * 0.74f, h * 0.52f)
        }
        val path3 = Path().apply {
            moveTo(w * 0.30f, h * 0.69f)
            quadraticTo(w * 0.50f, h * 0.61f, w * 0.70f, h * 0.69f)
        }

        drawPath(path1, Color(0xFF121212), style = Stroke(width = stroke, cap = StrokeCap.Round))
        drawPath(path2, Color(0xFF121212), style = Stroke(width = stroke * 0.9f, cap = StrokeCap.Round))
        drawPath(path3, Color(0xFF121212), style = Stroke(width = stroke * 0.8f, cap = StrokeCap.Round))
    }
}

/**
 * Logo do Google (Quatro cores)
 */
@Composable
fun GoogleIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.22f
        val radius = (w - stroke) / 2f
        val center = Offset(w / 2f, h / 2f)
        val topLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2, radius * 2)

        // Arcos com as 4 cores oficiais do Google: Azul, Verde, Amarelo, Vermelho
        drawArc(Color(0xFF4285F4), 315f, 90f, false, topLeft, arcSize, style = Stroke(stroke))
        drawArc(Color(0xFF34A853), 45f, 90f, false, topLeft, arcSize, style = Stroke(stroke))
        drawArc(Color(0xFFFBBC05), 135f, 90f, false, topLeft, arcSize, style = Stroke(stroke))
        drawArc(Color(0xFFEA4335), 225f, 90f, false, topLeft, arcSize, style = Stroke(stroke))

        // Barra horizontal interna azul do "G"
        drawRect(
            color = Color(0xFF4285F4),
            topLeft = Offset(center.x - stroke * 0.2f, center.y - stroke / 2f),
            size = Size(radius + stroke / 2f, stroke)
        )
    }
}

/**
 * Ícone de Cédula de Dinheiro estilo Pierre `[ ⌾ ]`
 */
@Composable
fun CashBillIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.09f

        // Retângulo arredondado da cédula
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.08f, h * 0.22f),
            size = Size(w * 0.84f, h * 0.56f),
            cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
            style = Stroke(stroke)
        )

        // Círculo central da nota
        drawCircle(
            color = tint,
            radius = w * 0.15f,
            center = Offset(w / 2f, h / 2f),
            style = Stroke(stroke)
        )

        // Detalhes laterais (dois tracinhos verticais)
        drawLine(tint, Offset(w * 0.20f, h * 0.35f), Offset(w * 0.20f, h * 0.65f), strokeWidth = stroke * 0.8f)
        drawLine(tint, Offset(w * 0.80f, h * 0.35f), Offset(w * 0.80f, h * 0.65f), strokeWidth = stroke * 0.8f)
    }
}

/**
 * Mini-badge circular do banco (ex: Mercado Pago, Nubank, Itaú) no canto inferior
 */
@Composable
fun BankMiniBadge(bankName: String, modifier: Modifier = Modifier) {
    val b = bankName.lowercase(Locale.ROOT)
    val isMercadoPago = b.contains("mercado") || b.contains("mp")
    val isNubank = b.contains("nu") || b.contains("nubank")
    val isItau = b.contains("itau") || b.contains("itaú")

    val bg = when {
        isMercadoPago -> Color(0xFFFFE600) // Amarelo Mercado Pago
        isNubank -> Color(0xFF820AD1) // Roxo Nubank
        isItau -> Color(0xFFEC7000) // Laranja Itaú
        else -> Color(0xFFFFE600) // Padrão Mercado Pago como na screenshot
    }

    Box(
        modifier = modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.2.dp, Color(0xFF0A0A0A), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isMercadoPago) {
            // Símbolo sutil de aperto de mãos do Mercado Pago em azul
            Canvas(modifier = Modifier.size(10.dp)) {
                val w = size.width
                val h = size.height
                val col = Color(0xFF003B6D)
                // Arco do logo Mercado Pago
                drawArc(
                    color = col,
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(w * 0.15f, h * 0.15f),
                    size = Size(w * 0.70f, h * 0.70f),
                    style = Stroke(w * 0.25f, cap = StrokeCap.Round)
                )
            }
        } else if (isNubank) {
            Text("nu", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.White)
        } else {
            Text("M", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF003B6D))
        }
    }
}
