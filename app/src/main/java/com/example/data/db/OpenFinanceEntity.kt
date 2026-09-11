package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "open_finance_accounts")
data class OpenFinanceAccountEntity(
    @PrimaryKey val id: String, // e.g., "itau_001"
    val bankName: String, // Itaú Unibanco, Nubank, Bradesco, Banco do Brasil, Santander
    val accountType: String, // Conta Corrente, Cartão de Crédito, Poupança
    val accountNumber: String,
    val balance: Double,
    val imageUrl: String? = null,
    val creditLimit: Double? = null,
    val availableCredit: Double? = null,
    val connectedAt: Long = System.currentTimeMillis(),
    val status: String = "CONECTADO", // CONECTADO, SINCRONIZANDO, PENDENTE
    val lastSync: Long = System.currentTimeMillis()
)

@Entity(tableName = "open_finance_transactions")
data class OpenFinanceTransactionEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val description: String,
    val amount: Double,         // valor absoluto
    val type: String = "DEBIT", // DEBIT = saida, CREDIT = entrada
    val date: String,
    val category: String,
    val isSyncedToExpenses: Boolean = false
)

