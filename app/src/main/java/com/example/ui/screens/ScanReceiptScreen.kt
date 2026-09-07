package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.gemini.ScannedReceiptResult
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosGreen
import com.example.ui.theme.IosIndigo
import com.example.ui.theme.IosOrange
import com.example.ui.theme.IosPurple
import com.example.ui.theme.IosRed
import com.example.ui.viewmodel.ScanUiState
import com.example.util.formatBr
import com.example.util.parseBrDouble
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScanReceiptScreen(
    scanState: ScanUiState,
    onScanImage: (Bitmap) -> Unit,
    onScanNfceUrlOrKey: (String) -> Unit = {},
    onSaveReceipt: (ScannedReceiptResult) -> Unit,
    onResetScan: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scanMode by remember { mutableStateOf("CAMERA") } // "CAMERA" ou "NFCE_SEFAZ"
    var nfceInputUrlOrKey by remember { mutableStateOf("") }

    var editableStoreName by remember { mutableStateOf("") }
    var editableAmount by remember { mutableStateOf("") }
    var editableCategory by remember { mutableStateOf("Alimentação") }
    var editablePaymentMethod by remember { mutableStateOf("PIX") }
    var editableDate by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.forLanguageTag("pt-BR")).format(Date()))
    }
    val editableItems = remember { mutableStateListOf<String>() }
    var newItemText by remember { mutableStateOf("") }
    var formValidationErr by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Alimentação", "Transporte", "Lazer", "Saúde", "Moradia", "Mercado", "Outros")
    val paymentMethods = listOf("PIX", "Cartão de Crédito", "Cartão de Débito", "Dinheiro", "Boleto", "NFC-e")

    val gmsBarcodeScanner = remember {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, options)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            selectedBitmap = bitmap
            val detectedQr = decodeQrFromBitmap(bitmap)
            if (!detectedQr.isNullOrBlank()) {
                Toast.makeText(context, "QR Code de Cupom identificado na foto!", Toast.LENGTH_SHORT).show()
                onScanNfceUrlOrKey(detectedQr)
            } else {
                onScanImage(bitmap)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao abrir câmera: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(
                context,
                "Permissão de câmera é necessária para fotografar cupons fiscais.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    selectedBitmap = bitmap
                    val detectedQr = decodeQrFromBitmap(bitmap)
                    if (!detectedQr.isNullOrBlank()) {
                        Toast.makeText(context, "QR Code de Cupom identificado na imagem!", Toast.LENGTH_SHORT).show()
                        onScanNfceUrlOrKey(detectedQr)
                    } else {
                        onScanImage(bitmap)
                    }
                } else {
                    Toast.makeText(context, "Não foi possível carregar a imagem selecionada.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao carregar imagem: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Escanear Cupom & NFC-e",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF3F5FA))
        ) {
            // Fundo Aurora Orgânico
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF10B981).copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.10f),
                        radius = size.width * 0.65f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF0EA5E9).copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(size.width * 0.12f, size.height * 0.35f),
                        radius = size.width * 0.55f
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Seletor de Modo em Cápsula de Vidro Branco (Sem fundos pretos)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xEEFFFFFF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isCamera = scanMode == "CAMERA"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCamera) IosBlue else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { scanMode = "CAMERA" }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = if (isCamera) Color.White else Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "Foto Cupom (IA)",
                                        fontWeight = if (isCamera) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = if (isCamera) Color.White else Color(0xFF475569)
                                    )
                                }
                            }
                        }

                        val isNfce = scanMode == "NFCE_SEFAZ"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isNfce) Color(0xFF10B981) else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { scanMode = "NFCE_SEFAZ" }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        tint = if (isNfce) Color.White else Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "QR Code SEFAZ",
                                        fontWeight = if (isNfce) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = if (isNfce) Color.White else Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }
                }

                if (scanMode == "NFCE_SEFAZ") {
                    // Card SEFAZ com Vidro Translúcido Branco
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xEEFFFFFF))
                                .border(1.2.dp, Color(0xFFE2E8F0), RoundedCornerShape(22.dp))
                                .padding(20.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(24.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Consulta Direta NFC-e (SEFAZ)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                                        Text("Extrai itens, valor e estabelecimento diretamente da Nota", fontSize = 12.sp, color = Color(0xFF64748B))
                                    }
                                }

                                // Botão Principal: Apontar câmera e ler QR Code na hora
                                Button(
                                    onClick = {
                                        gmsBarcodeScanner.startScan()
                                            .addOnSuccessListener { barcode ->
                                                val rawValue = barcode.rawValue ?: barcode.displayValue
                                                if (!rawValue.isNullOrBlank()) {
                                                    nfceInputUrlOrKey = rawValue
                                                    Toast.makeText(context, "QR Code lido com sucesso!", Toast.LENGTH_SHORT).show()
                                                    onScanNfceUrlOrKey(rawValue)
                                                } else {
                                                    Toast.makeText(context, "QR Code lido, mas sem dados.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(context, "Leitura de QR Code: ${e.localizedMessage ?: e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Escanear QR Code com a Câmera", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }

                                OutlinedTextField(
                                    value = nfceInputUrlOrKey,
                                    onValueChange = { nfceInputUrlOrKey = it },
                                    label = { Text("URL da NFC-e ou Chave de Acesso (44 dígitos)") },
                                    placeholder = { Text("Cole a chave ou link do QR Code da SEFAZ...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF10B981),
                                        unfocusedBorderColor = Color(0xFFCBD5E1),
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    singleLine = false,
                                    maxLines = 3
                                )

                                // Botão Real de Colar da Área de Transferência
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val clip = clipboardManager.getText()?.text ?: ""
                                            if (clip.isNotBlank()) {
                                                nfceInputUrlOrKey = clip.trim()
                                                Toast.makeText(context, "Texto colado da área de transferência!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Área de transferência vazia.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Colar Chave/URL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    if (nfceInputUrlOrKey.isNotBlank()) {
                                        OutlinedButton(
                                            onClick = { nfceInputUrlOrKey = "" },
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Limpar", fontSize = 12.sp)
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (nfceInputUrlOrKey.isNotBlank()) {
                                            onScanNfceUrlOrKey(nfceInputUrlOrKey)
                                        } else {
                                            Toast.makeText(context, "Cole uma chave de 44 dígitos ou link de NFC-e primeiro.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Consultar Cupom na SEFAZ", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                } else {
                    // Card Câmera / Galeria com Vidro Branco Lindo
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xEEFFFFFF))
                                .border(1.2.dp, Color(0xFFE2E8F0), RoundedCornerShape(22.dp))
                                .padding(20.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (selectedBitmap != null) {
                                    Image(
                                        bitmap = selectedBitmap!!.asImageBitmap(),
                                        contentDescription = "Foto do Cupom",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(220.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(16.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFFF1F5F9))
                                            .border(1.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(16.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Default.CameraAlt,
                                                contentDescription = null,
                                                tint = Color(0xFF64748B),
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "Tire uma foto nítida do cupom fiscal",
                                                color = Color(0xFF64748B),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val hasPermission = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.CAMERA
                                            ) == PackageManager.PERMISSION_GRANTED

                                            if (hasPermission) {
                                                try {
                                                    cameraLauncher.launch(null)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Erro ao abrir câmera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = IosBlue)
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Tirar Foto", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { galleryLauncher.launch("image/*") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Galeria", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }

                                // Atalho direto para escanear QR Code da NFC-e com a câmera
                                OutlinedButton(
                                    onClick = {
                                        gmsBarcodeScanner.startScan()
                                            .addOnSuccessListener { barcode ->
                                                val rawValue = barcode.rawValue ?: barcode.displayValue
                                                if (!rawValue.isNullOrBlank()) {
                                                    Toast.makeText(context, "QR Code lido com sucesso!", Toast.LENGTH_SHORT).show()
                                                    onScanNfceUrlOrKey(rawValue)
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(context, "Scanner: ${e.localizedMessage ?: e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF10B981))
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Ou Ler QR Code da NFC-e (Câmera)", color = Color(0xFF059669), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }

                // ESTADOS DE CARREGAMENTO / RESULTADO DO ESCANEAMENTO
                when (scanState) {
                    is ScanUiState.Scanning -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xEEFFFFFF))
                                    .border(1.2.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(color = IosBlue)
                                    Text(
                                        text = "Processando cupom e consultando itens...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "A IA está extraindo data, estabelecimento, valor total e itens.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }

                    is ScanUiState.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFEF2F2))
                                    .border(1.2.dp, Color(0xFFFCA5A5), RoundedCornerShape(20.dp))
                                    .padding(18.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Erro na leitura do cupom", fontWeight = FontWeight.Bold, color = IosRed)
                                    Text(scanState.message, fontSize = 13.sp, color = Color(0xFF7F1D1D))
                                    Button(
                                        onClick = onResetScan,
                                        colors = ButtonDefaults.buttonColors(containerColor = IosRed),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Tentar Novamente")
                                    }
                                }
                            }
                        }
                    }

                    is ScanUiState.Success -> {
                        val res = scanState.result
                        LaunchedEffect(res) {
                            editableStoreName = res.storeName
                            editableAmount = if (res.totalAmount > 0) String.format(Locale.forLanguageTag("pt-BR"), "%.2f", res.totalAmount) else ""
                            editableCategory = res.category.ifBlank { "Alimentação" }
                            editablePaymentMethod = res.paymentMethod.ifBlank { "NFC-e" }
                            editableDate = res.transactionDate.ifBlank {
                                SimpleDateFormat("yyyy-MM-dd", Locale.forLanguageTag("pt-BR")).format(Date())
                            }
                            editableItems.clear()
                            editableItems.addAll(res.items)
                        }

                        // FORMULÁRIO DE REVISÃO E CONFIRMAÇÃO EM MOEDA BRASILEIRA (R$)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xEEFFFFFF))
                                    .border(1.2.dp, Color(0xFFBBF7D0), RoundedCornerShape(22.dp))
                                    .padding(20.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                                        Text(
                                            "Cupom Reconhecido com Sucesso!",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                    }

                                    OutlinedTextField(
                                        value = editableStoreName,
                                        onValueChange = { editableStoreName = it },
                                        label = { Text("Estabelecimento") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp)
                                    )

                                    OutlinedTextField(
                                        value = editableAmount,
                                        onValueChange = { editableAmount = it },
                                        label = { Text("Valor Total (Moeda BR)") },
                                        prefix = { Text("R$ ", fontWeight = FontWeight.Bold, color = Color(0xFF059669)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                    )

                                    Text("Categoria", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        categories.forEach { cat ->
                                            val isSel = editableCategory == cat
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isSel) IosBlue else Color(0xFFF1F5F9),
                                                modifier = Modifier.clickable { editableCategory = cat }
                                            ) {
                                                Text(
                                                    text = cat,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSel) Color.White else Color(0xFF334155)
                                                )
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            val parsedVal = editableAmount.parseBrDouble() ?: 0.0
                                            if (parsedVal <= 0.0) {
                                                Toast.makeText(context, "Insira um valor válido em R$.", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            val confirmedResult = ScannedReceiptResult(
                                                storeName = editableStoreName.ifBlank { "Compra Cupom Fiscal" },
                                                totalAmount = parsedVal,
                                                transactionDate = editableDate,
                                                category = editableCategory,
                                                paymentMethod = editablePaymentMethod,
                                                items = editableItems.toList()
                                            )
                                            onSaveReceipt(confirmedResult)
                                            Toast.makeText(context, "Despesa salva com sucesso!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Confirmar e Salvar no Banco", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                }
                            }
                        }
                    }

                    else -> {}
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Tenta decodificar um QR Code a partir de um Bitmap usando a biblioteca ZXing.
 */
fun decodeQrFromBitmap(bitmap: Bitmap): String? {
    return try {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val source = RGBLuminanceSource(width, height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val reader = MultiFormatReader()
        val result = reader.decode(binaryBitmap)
        result.text
    } catch (e: Exception) {
        null
    }
}
