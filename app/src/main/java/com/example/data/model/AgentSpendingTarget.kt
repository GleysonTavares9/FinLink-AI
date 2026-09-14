package com.example.data.model

/**
 * Meta de gastos ou limite de transações definido pelo próprio usuário
 * monitorado por um Agente Educador Financeiro à sua escolha.
 */
data class AgentSpendingTarget(
    val id: String,
    val agentId: String,
    val agentName: String,
    val targetType: String,   // "CARTAO_CREDITO", "CATEGORIA", "GERAL"
    val targetName: String,   // "Cartão de Crédito", "Alimentação / Supermercado", etc.
    val limitAmount: Double,  // Valor máximo estabelecido pelo usuário (ex: 1500.0)
    val currentSpent: Double = 0.0,
    val isNotifiedWarning: Boolean = false,  // Alerta preventivo quando atinge >= 80%
    val isNotifiedExceeded: Boolean = false, // Alerta quando atinge >= 100%
    val createdAt: Long = System.currentTimeMillis()
)
