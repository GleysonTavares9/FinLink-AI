package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.supabase.AuthUiState
import com.example.data.remote.supabase.SyncResult
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosGreen
import com.example.ui.theme.IosPurple
import com.example.ui.theme.IosRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    authState: AuthUiState,
    syncResult: SyncResult?,
    isSyncing: Boolean,
    onSignIn: (email: String, password: String) -> Unit,
    onSignUp: (email: String, password: String) -> Unit,
    onSignOut: () -> Unit,
    onSyncWithCloud: () -> Unit,
    onContinueAsGuest: () -> Unit = {},
    onGoogleSignIn: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isSignUpMode by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F5FA))
    ) {
        // Fundo Aurora Multicolorido Suave
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF10B981).copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.90f, size.height * 0.10f),
                    radius = size.width * 0.65f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0EA5E9).copy(alpha = 0.10f), Color.Transparent),
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
                            "Conta Sincronizada",
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
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF10B981), Color(0xFF059669))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    "Criptografia de Ponta a Ponta",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = Color(0xFF1D1D1F)
                                )
                                Text(
                                    "PostgreSQL com Row Level Security (RLS)",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B7280)
                                )
                            }
                        }
                    }
                }

                if (authState is AuthUiState.Authenticated) {
                    val session = authState.session
                    val user = session.user

                    // Perfil Autenticado Glass Card
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
                                "Meu Perfil",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color(0xFF1D1D1F)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(Color(0xFF34C759), Color(0xFF10B981)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val initial = if (user.email.isNotBlank()) user.email.take(1).uppercase() else "U"
                                    Text(initial, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        user.email.ifBlank { "Usuário (Sessão Local)" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color(0xFF1D1D1F)
                                    )
                                    Text(
                                        "Status: Conta Logada ✅",
                                        fontSize = 14.sp,
                                        color = Color(0xFF34C759),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Security Pill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = IosGreen.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Isolamento Ativo: Row Level Security (RLS)",
                                        fontSize = 12.sp,
                                        color = Color(0xFF15803D),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Sincronizar Nuvem
                            Button(
                                onClick = onSyncWithCloud,
                                enabled = !isSyncing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IosBlue)
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Sincronizando...", fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sincronizar Dados", fontWeight = FontWeight.Bold)
                                }
                            }

                            syncResult?.let { res ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (res.isSuccess) IosGreen.copy(alpha = 0.12f) else IosRed.copy(alpha = 0.12f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (res.isSuccess) Icons.Default.CloudDone else Icons.Default.Security,
                                            contentDescription = null,
                                            tint = if (res.isSuccess) Color(0xFF15803D) else IosRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            res.message,
                                            fontSize = 12.sp,
                                            color = if (res.isSuccess) Color(0xFF15803D) else IosRed,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = onContinueAsGuest,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IosBlue)
                            ) {
                                Text("Acessar Painel Financeiro", fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            OutlinedButton(
                                onClick = onSignOut,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, IosRed)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = IosRed, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sair da Conta (Logout)", color = IosRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Auth Form Card (Sign In / Sign Up)
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
                            // Toggle Login / Cadastro
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFE5E7EB), RoundedCornerShape(14.dp))
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (!isSignUpMode) Color.White else Color.Transparent)
                                        .clickable { isSignUpMode = false }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Fazer Login",
                                        fontWeight = if (!isSignUpMode) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = if (!isSignUpMode) IosBlue else Color(0xFF6B7280)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSignUpMode) Color.White else Color.Transparent)
                                        .clickable { isSignUpMode = true }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Cadastrar Conta",
                                        fontWeight = if (isSignUpMode) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = if (isSignUpMode) Color(0xFF10B981) else Color(0xFF6B7280)
                                    )
                                }
                            }

                            Text(
                                if (isSignUpMode) "Crie sua conta segura para backup" else "Acesse suas despesas sincronizadas",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF1D1D1F)
                            )

                            // Email Field
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("E-mail") },
                                placeholder = { Text("seu@email.com") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = IosBlue) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IosBlue,
                                    unfocusedBorderColor = Color(0xFFD1D5DB),
                                    focusedTextColor = Color(0xFF1D1D1F),
                                    unfocusedTextColor = Color(0xFF1D1D1F)
                                ),
                                singleLine = true
                            )

                            // Password Field
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Senha") },
                                placeholder = { Text("Mínimo 6 caracteres") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = IosBlue) },
                                trailingIcon = {
                                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                        Icon(
                                            if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Alternar visibilidade"
                                        )
                                    }
                                },
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IosBlue,
                                    unfocusedBorderColor = Color(0xFFD1D5DB),
                                    focusedTextColor = Color(0xFF1D1D1F),
                                    unfocusedTextColor = Color(0xFF1D1D1F)
                                ),
                                singleLine = true
                            )

                            if (authState is AuthUiState.Error) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = IosRed.copy(alpha = 0.12f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "⚠️ ${authState.message}",
                                        color = IosRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (emailInput.isBlank() || !emailInput.contains("@")) {
                                        Toast.makeText(context, "Insira um e-mail válido", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (passwordInput.length < 6) {
                                        Toast.makeText(context, "A senha deve conter no mínimo 6 caracteres", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    if (isSignUpMode) {
                                        onSignUp(emailInput.trim(), passwordInput)
                                    } else {
                                        onSignIn(emailInput.trim(), passwordInput)
                                    }
                                },
                                enabled = authState !is AuthUiState.Loading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSignUpMode) Color(0xFF10B981) else IosBlue
                                )
                            ) {
                                if (authState is AuthUiState.Loading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Text(
                                        if (isSignUpMode) "Cadastrar Conta Segura" else "Entrar na Conta",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            // Divisor OU
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(1.dp)
                                        .background(Color(0xFFE5E7EB))
                                )
                                Text(
                                    "OU",
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF9CA3AF)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(1.dp)
                                        .background(Color(0xFFE5E7EB))
                                )
                            }

                            // Botão Entrar com Google
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .clickable { onGoogleSignIn() },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    GoogleIcon(modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Continuar com o Google",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF374151)
                                    )
                                }
                            }

                            // Botão Acessar sem Login (Modo Local Offline)
                            OutlinedButton(
                                onClick = onContinueAsGuest,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                            ) {
                                Text(
                                    "Acessar sem Login (Modo Local Offline)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF4B5563)
                                )
                            }
                        }
                    }

                    // Security Explanation Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(10.dp, RoundedCornerShape(24.dp), spotColor = Color(0x10000000)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xF4FFFFFF))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Brush.verticalGradient(listOf(Color.White, Color(0x1E000000))), RoundedCornerShape(24.dp))
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = IosPurple, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Por que suas informações estão seguras?", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1D1D1F))
                            }

                            Text(
                                "• Criptografia em Trânsito: Todas as conexões utilizam HTTPS/TLS 1.3 obrigatório.\n" +
                                        "• Row Level Security (RLS): Políticas nativas no banco PostgreSQL garantem que somente você pode visualizar ou alterar suas despesas.\n" +
                                        "• Autonomia Offline: Você pode continuar lançando despesas sem internet; tudo é sincronizado quando estiver online.",
                                fontSize = 12.sp,
                                color = Color(0xFF4B5563),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun GoogleIcon(modifier: Modifier = Modifier.size(20.dp)) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.width * 0.20f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Arco Vermelho (topo)
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth)
        )
        // Arco Amarelo (inferior esquerdo)
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth)
        )
        // Arco Verde (inferior direito)
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth)
        )
        // Arco Azul (superior direito)
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = 270f,
            sweepAngle = 45f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth)
        )
        // Barra Azul central
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(center.x, center.y),
            end = Offset(size.width - strokeWidth / 4f, center.y),
            strokeWidth = strokeWidth
        )
    }
}
