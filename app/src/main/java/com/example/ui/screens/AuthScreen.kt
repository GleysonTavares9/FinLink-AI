package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.remote.supabase.AuthUiState
import com.example.data.remote.supabase.SyncResult
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosGreen
import com.example.ui.theme.IosOrange
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
    onOAuthCallback: (Uri) -> Unit = {},
    onUpdateProfile: (name: String, avatarUrl: String) -> Unit = { _, _ -> },
    googleAuthUrl: String = "",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isSignUpMode by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var showGoogleLoginDialog by remember { mutableStateOf(false) }

    // Diálogo in-app para Login com Google (intercepta localhost e deep-link sem falha de conexão)
    if (showGoogleLoginDialog && googleAuthUrl.isNotBlank()) {
        Dialog(
            onDismissRequest = { showGoogleLoginDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Entrar com Google", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                        navigationIcon = {
                            IconButton(onClick = { showGoogleLoginDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                    )
                }
            ) { padding ->
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.userAgentString = settings.userAgentString + " FinLinkApp/1.0"
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    if (url.startsWith("finlink://auth-callback") || url.contains("localhost:3000/#access_token=") || url.contains("#access_token=")) {
                                        showGoogleLoginDialog = false
                                        onOAuthCallback(request.url)
                                        return true
                                    }
                                    return false
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    if (url != null && (url.startsWith("finlink://auth-callback") || url.contains("localhost:3000/#access_token=") || url.contains("#access_token="))) {
                                        showGoogleLoginDialog = false
                                        onOAuthCallback(Uri.parse(url))
                                    }
                                }
                            }
                            loadUrl(googleAuthUrl)
                        }
                    }
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (authState is AuthUiState.Authenticated) "Perfil do Usuário" else "Conta & Sincronização",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9))
                    )
                )
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // ==========================================================
                // CASO 1: USUÁRIO JÁ AUTENTICADO (PERFIL COMPLETO REAL)
                // ==========================================================
                if (authState is AuthUiState.Authenticated) {
                    val session = authState.session
                    val user = session.user

                    var editName by remember(user.fullName) {
                        mutableStateOf(user.fullName.ifBlank { "Usuário FinLink" })
                    }
                    var userAvatarUrl by remember(user.avatarUrl) {
                        mutableStateOf(user.avatarUrl)
                    }

                    // Seletor de Foto nativo da galeria
                    val photoPickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
                        if (uri != null) {
                            userAvatarUrl = uri.toString()
                            onUpdateProfile(editName, uri.toString())
                            Toast.makeText(context, "Foto de perfil atualizada!", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // Card Elegante de Perfil
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(16.dp, RoundedCornerShape(26.dp), spotColor = Color(0x15000000)),
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            // FOTO DE PERFIL COM BOTíO DE TROCA
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .clickable { photoPickerLauncher.launch("image/*") }
                            ) {
                                if (userAvatarUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = userAvatarUrl,
                                        contentDescription = "Foto de Perfil",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = editName.take(1).uppercase(),
                                            color = Color.White,
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                // Overlay para trocar foto
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(IosBlue)
                                        .border(2.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Alterar Foto",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Toque na foto para alterar",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )

                            // CAMPO: NOME COMPLETO (EDITÁVEL)
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("Nome Completo") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = IosBlue)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IosBlue,
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                )
                            )

                            // CAMPO: E-MAIL (ESTRITAMENTE DESABILITADO / NíO EDITÁVEL)
                            OutlinedTextField(
                                value = user.email.ifBlank { "Conta Google Conectada" },
                                onValueChange = {},
                                enabled = false,
                                label = { Text("E-mail (Protegido)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF94A3B8))
                                },
                                supportingText = {
                                    Text("O e-mail da sua conta Google não pode ser alterado.", fontSize = 11.sp, color = Color(0xFF64748B))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledBorderColor = Color(0xFFE2E8F0),
                                    disabledTextColor = Color(0xFF334155),
                                    disabledLabelColor = Color(0xFF64748B),
                                    disabledContainerColor = Color(0xFFF8FAFC)
                                )
                            )

                            // BOTíO: SALVAR ALTERAÇÕES NO PERFIL
                            Button(
                                onClick = {
                                    onUpdateProfile(editName, userAvatarUrl)
                                    Toast.makeText(context, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IosBlue)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Salvar Nome e Foto", fontWeight = FontWeight.Bold)
                            }

                            // BADGE DE CONEXíO ATIVA
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = IosGreen.copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Autenticação Segura Google", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF15803D))
                                        Text("Seus dados estão protegidos com criptografia ponta a ponta.", fontSize = 11.sp, color = Color(0xFF166534))
                                    }
                                }
                            }

                            // BOTíO SINCRONIZAR DADOS
                            Button(
                                onClick = onSyncWithCloud,
                                enabled = !isSyncing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sincronizando...", color = Color.White)
                                } else {
                                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sincronizar Dados na Nuvem", fontWeight = FontWeight.Bold)
                                }
                            }

                            // RESULTADO DA SINCRONIZAÇíO
                            syncResult?.let { res ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (res.isSuccess) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (res.isSuccess) Color(0xFFBBF7D0) else Color(0xFFFECACA)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (res.isSuccess) Icons.Default.CloudDone else Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = if (res.isSuccess) Color(0xFF16A34A) else Color(0xFFDC2626)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (res.isSuccess) "Nuvem atualizada: ${res.uploadedExpenses} enviados, ${res.downloadedExpenses} recebidos." else "Erro: ${res.message}",
                                            fontSize = 12.sp,
                                            color = if (res.isSuccess) Color(0xFF15803D) else Color(0xFF991B1B)
                                        )
                                    }
                                }
                            }

                            // BOTíO DESCONECTAR / LOGOUT
                            OutlinedButton(
                                onClick = onSignOut,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = IosRed)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sair da Conta", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // ==========================================================
                // CASO 2: USUÁRIO NíO AUTENTICADO (FORMULÁRIO & GOOGLE LOGIN)
                // ==========================================================
                if (authState !is AuthUiState.Authenticated) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(16.dp, RoundedCornerShape(26.dp), spotColor = Color(0x15000000)),
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = if (isSignUpMode) "Criar Nova Conta" else "Acessar sua Conta",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Sincronize seus gastos, extratos e orçamento familiar em todos os seus dispositivos.",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )

                            // BOTíO: ENTRAR COM O GOOGLE
                            Button(
                                onClick = {
                                    if (googleAuthUrl.isNotBlank()) {
                                        showGoogleLoginDialog = true
                                    } else {
                                        onGoogleSignIn()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Canvas(modifier = Modifier.size(20.dp)) {
                                    drawCircle(color = Color.White)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Continuar com o Google", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFFE2E8F0)))
                                Text("  OU  ", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFFE2E8F0)))
                            }

                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("E-mail") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = IosBlue) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                            )

                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Senha") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = IosBlue) },
                                trailingIcon = {
                                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                        Icon(
                                            if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null
                                        )
                                    }
                                },
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                            )

                            Button(
                                onClick = {
                                    if (emailInput.isNotBlank() && passwordInput.isNotBlank()) {
                                        if (isSignUpMode) onSignUp(emailInput.trim(), passwordInput)
                                        else onSignIn(emailInput.trim(), passwordInput)
                                    } else {
                                        Toast.makeText(context, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IosBlue)
                            ) {
                                Text(if (isSignUpMode) "Cadastrar Conta" else "Entrar com E-mail", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { isSignUpMode = !isSignUpMode },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    if (isSignUpMode) "Já possui conta? Fazer Login" else "Não tem conta? Cadastre-se",
                                    color = IosBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
