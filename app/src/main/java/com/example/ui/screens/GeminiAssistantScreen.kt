package com.example.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosIndigo
import com.example.ui.theme.IosPurple

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GeminiAssistantScreen(
    aiResponse: String,
    isLoading: Boolean,
    onBack: () -> Unit,
    onAskGemini: (String) -> Unit
) {
    var userPromptText by remember { mutableStateOf("") }

    val promptSuggestions = listOf(
        "Como economizar 15% este mês?",
        "Analisar meus maiores cupons fiscais",
        "Dicas de orçamento para alimentação",
        "Avanço dos gastos nos últimos 30 dias"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F5FA))
    ) {
        // Aurora Lighting
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.90f, size.height * 0.10f),
                    radius = size.width * 0.65f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF6366F1).copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(size.width * 0.10f, size.height * 0.35f),
                    radius = size.width * 0.55f
                )
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Assistente IA",
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
                // Hero Glass Card
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
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Brush.linearGradient(listOf(IosIndigo, IosPurple))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "GEMINI ADVISOR FINANCEIRO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IosPurple,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Análise inteligente de despesas, comparação de preços e planejamento financeiro.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF4B5563),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // Sugestões
                Text(
                    text = "SUGESTÕES RÁPIDAS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF86868B),
                    letterSpacing = 0.8.sp
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    promptSuggestions.forEach { suggestion ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xF4FFFFFF),
                            modifier = Modifier
                                .clickable {
                                    userPromptText = suggestion
                                    onAskGemini(suggestion)
                                }
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFFE5E7EB),
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Text(
                                text = suggestion,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = IosIndigo,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Chat Input Field
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
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = userPromptText,
                            onValueChange = { userPromptText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_prompt_input"),
                            placeholder = { Text("Faça uma pergunta sobre seus gastos...", fontSize = 14.sp, color = Color(0xFF9CA3AF)) },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IosPurple,
                                unfocusedBorderColor = Color(0xFFD1D5DB),
                                focusedTextColor = Color(0xFF1D1D1F),
                                unfocusedTextColor = Color(0xFF1D1D1F)
                            ),
                            maxLines = 4
                        )

                        Button(
                            onClick = {
                                if (userPromptText.isNotBlank() && !isLoading) {
                                    onAskGemini(userPromptText.trim())
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_send_ai_prompt"),
                            enabled = userPromptText.isNotBlank() && !isLoading,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IosPurple)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Consultando Gemini...")
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Enviar Pergunta", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // AI Answer Display Bubble
                if (aiResponse.isNotBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(14.dp, RoundedCornerShape(24.dp), spotColor = Color(0x12000000))
                            .testTag("ai_response_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xF8FFFFFF))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Brush.verticalGradient(listOf(Color.White, Color(0x1E000000))), RoundedCornerShape(24.dp))
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(IosPurple.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = IosPurple, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Resposta do Assistente",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D1D1F)
                                )
                            }

                            Text(
                                text = aiResponse,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 22.sp,
                                color = Color(0xFF374151)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
