package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val storeName: String,
    val totalAmount: Double,
    val transactionDate: String, // YYYY-MM-DD
    val category: String = "Alimentação", // Alimentação, Transporte, Lazer, Saúde, Moradia, Outros
    val paymentMethod: String = "PIX", // PIX, Cartão de Crédito, Cartão de Débito, Dinheiro, Boleto
    val rawOcrText: String? = null,
    val receiptImagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isManual: Boolean = false,
    val source: String = "MANUAL", // OCR_GEMINI, MANUAL, OPEN_FINANCE, EXTRATO_BANCARIO
    val itemsJson: String? = null,
    val notes: String? = null
)

