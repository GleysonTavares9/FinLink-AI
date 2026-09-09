package com.example.data.model

/**
 * Tipos de transações no extrato unificado do FinLink AI
 */
enum class TransactionType {
    ENTRADA,        // Receitas reais, salários, rendimentos (+R$)
    SAIDA,          // Despesas reais de consumo, compras, débitos (-R$)
    ASSINATURA,     // Serviços recorrentes como Netflix, Spotify, Claro, Amazon
    PAGAMENTO,      // Boletos, tributos (DARF, impostos) e contas essenciais
    CUPOM_FISCAL,   // Compras registradas via foto de cupom ou QR Code SEFAZ
    TRANSFERENCIA   // Transferências entre contas próprias, poupança, caixinhas, pagamento de fatura
}

/**
 * Modelo consolidado de transação para as telas de Atividades e Dashboard
 */
data class UnifiedTransaction(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val type: TransactionType,
    val date: String, // YYYY-MM-DD
    val formattedDate: String, // ex: "7 de set" ou "Hoje"
    val dateGroup: String, // ex: "Hoje", "Ontem", "Esta semana", "Setembro 2026"
    val category: String,
    val bankName: String = "Mercado Pago",
    val paymentMethod: String = "PIX",
    val isSubscription: Boolean = false,
    val isInternalTransfer: Boolean = false,
    val accountId: String? = null,
    val items: List<String> = emptyList(),
    val notes: String? = null
)

/**
 * Modelo de Assinatura / Gasto Fixo Recorrente
 */
data class SubscriptionItem(
    val id: String,
    val name: String,
    val category: String,
    val amount: Double,
    val billingCycle: String = "Mensal",
    val renewalDay: Int,
    val nextRenewalDate: String,
    val bankOrCard: String = "Mercado Pago",
    val status: String = "Ativa", // "Ativa", "Pausada"
    val iconName: String = "netflix",
    val description: String = ""
)

/**
 * Modelo de Recurso oferecido pelo FinLink AI
 */
data class AppOfferingFeature(
    val id: String,
    val title: String,
    val shortBadge: String,
    val summary: String,
    val fullDescription: String,
    val category: String,
    val isProOnly: Boolean = false
)
