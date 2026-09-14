package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "open_finance_accounts")
data class OpenFinanceAccountEntity(
    @PrimaryKey val id: String, // e.g., "itau_001"
    val bankName: String, // Itaú Unibanco, Nubank, Bradesco, Banco do Brasil, Santander, Mercado Pago
    val accountType: String, // Conta Corrente, Cartão de Crédito, Poupança
    val accountNumber: String,
    val balance: Double, // Para cartão: valor da fatura em aberto
    val imageUrl: String? = null,
    val creditLimit: Double? = null,
    val availableCredit: Double? = null,
    val closeDate: String? = null,       // Data de fechamento da fatura em aberto
    val dueDate: String? = null,         // Data de vencimento da fatura a vencer
    val minimumPayment: Double? = null,  // Pagamento mínimo
    val brand: String? = null,           // MASTERCARD, VISA, ELO
    val level: String? = null,           // PLATINUM, BLACK, GOLD
    val connectedAt: Long = System.currentTimeMillis(),
    val status: String = "CONECTADO",    // CONECTADO, SINCRONIZANDO, PENDENTE
    val lastSync: Long = System.currentTimeMillis()
)

@Entity(tableName = "open_finance_bills")
data class OpenFinanceBillEntity(
    @PrimaryKey val id: String,          // ID da fatura na Pluggy (UUID)
    val accountId: String,
    val dueDate: String,                 // Data de vencimento
    val closingDate: String? = null,     // Data de fechamento
    val totalAmount: Double,             // Valor total da fatura
    val minimumPaymentAmount: Double = 0.0,
    val allowsInstallments: Boolean = true,
    val isPaid: Boolean = false,         // Status de quitação da fatura
    val financeChargesJson: String? = null, // IOF, juros, multas
    val paymentsJson: String? = null,       // Pagamentos efetuados
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "open_finance_transactions")
data class OpenFinanceTransactionEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val description: String,
    val amount: Double,                   // Valor absoluto da transação
    val type: String = "DEBIT",           // DEBIT = saída/compra, CREDIT = entrada/estorno/pagamento
    val status: String = "POSTED",        // PENDING (fatura aberta / a vencer) ou POSTED (fatura fechada/paga)
    val date: String,
    val category: String,
    val billId: String? = null,           // ID da fatura à qual pertence (se fechada)
    val billForecastDate: String? = null, // Previsão da fatura (YYYY-MM) para parcelas futuras
    val installmentNumber: Int? = null,   // Número da parcela atual (ex: 2)
    val totalInstallments: Int? = null,   // Total de parcelas (ex: 6)
    val totalInstallmentAmount: Double? = null, // Valor total da compra parcelada original
    val operationType: String? = null,    // TED, DOC, PIX, CARTAO, BOLETO, etc.
    val isSyncedToExpenses: Boolean = false
)
