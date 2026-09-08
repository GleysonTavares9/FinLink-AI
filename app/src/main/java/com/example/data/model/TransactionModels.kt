package com.example.data.model

/**
 * Tipos de transações no extrato unificado do FinLink AI
 */
enum class TransactionType {
    ENTRADA,       // Receitas, depósitos, transferências recebidas (+R$)
    SAIDA,         // Despesas, compras em lojas, débitos (-R$)
    ASSINATURA,    // Serviços recorrentes como Netflix, Spotify, Amazon (-R$)
    PAGAMENTO,     // Boletos, faturas, PIX para terceiros
    CUPOM_FISCAL   // Compras registradas via foto de cupom ou QR Code SEFAZ
}

/**
 * Modelo consolidado de transação para a tela de Atividades / Extrato
 */
data class UnifiedTransaction(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val type: TransactionType,
    val date: String, // YYYY-MM-DD
    val formattedDate: String, // ex: "7 de set" ou "Hoje"
    val dateGroup: String, // ex: "Hoje", "Ontem", "Esta semana", "Agosto 2026"
    val category: String,
    val bankName: String = "Mercado Pago",
    val paymentMethod: String = "PIX",
    val isSubscription: Boolean = false,
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
