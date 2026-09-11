package com.example.data.model

data class FinancialAgentPersona(
    val id: String,
    val name: String,
    val shortRole: String,
    val iconEmoji: String,
    val accentColorHex: Long,
    val description: String
)

object DefaultFinancialAgents {
    val list = listOf(
        FinancialAgentPersona(
            id = "orquestrador",
            name = "Orquestrador Consultor",
            shortRole = "Economista PhD & Auditor",
            iconEmoji = "🧠",
            accentColorHex = 0xFF007AFF,
            description = "Ponto de entrada que delega de forma autônoma para os especialistas e consolida a resposta final com precisão clínica."
        ),
        FinancialAgentPersona(
            id = "auditor",
            name = "Auditor Contábil Sênior",
            shortRole = "Auditoria & Conformidade",
            iconEmoji = "⚖️",
            accentColorHex = 0xFF10B981,
            description = "Trata cada centavo com rigor métrico. Verifica categorias, conciliação e integridade de faturas e cupons."
        ),
        FinancialAgentPersona(
            id = "consultor",
            name = "Consultor Fiscal & Financeiro",
            shortRole = "Análise Clínica de Hábitos",
            iconEmoji = "💼",
            accentColorHex = 0xFF0EA5E9,
            description = "Tom clínico, denso e direto. Análise de padrões de consumo, ritmo mensal e identificação de anomalias."
        ),
        FinancialAgentPersona(
            id = "calculos",
            name = "Especialista Cálculos",
            shortRole = "Matemática Financeira",
            iconEmoji = "📐",
            accentColorHex = 0xFFF59E0B,
            description = "Cálculo exato de juros, CET, amortização de financiamentos e projeções de fluxo de caixa."
        ),
        FinancialAgentPersona(
            id = "investimentos",
            name = "Investimentos e Risco",
            shortRole = "Alocação & Reserva",
            iconEmoji = "📈",
            accentColorHex = 0xFF8B5CF6,
            description = "Estruturação de reserva de emergência, perfil de risco e balanceamento de liquidez."
        ),
        FinancialAgentPersona(
            id = "planejamento",
            name = "Planejamento Estratégico",
            shortRole = "Metas & Orçamento",
            iconEmoji = "🎯",
            accentColorHex = 0xFFEC4899,
            description = "Tetos orçamentários mensais, despesas sazonais e roteiro para independência financeira."
        )
    )

    fun getById(id: String): FinancialAgentPersona {
        return list.find { it.id == id } ?: list.first()
    }
}

data class CategoryItem(
    val name: String,
    val iconType: String,
    val colorHex: Long,
    val percentage: Int,
    val amount: Double
)

data class OriginItem(
    val label: String,
    val amount: Double,
    val colorHex: Long
)

data class ComparisonRow(
    val category: String,
    val colorHex: Long,
    val values: List<Double>
)

sealed class FinancialWidget {
    data class CategoryBreakdown(
        val title: String,
        val period: String,
        val totalAmount: Double,
        val categories: List<CategoryItem>,
        val origins: List<OriginItem>,
        val syncTimestamp: String
    ) : FinancialWidget()

    data class MonthlyComparison(
        val title: String,
        val period: String,
        val months: List<String>,
        val rows: List<ComparisonRow>,
        val totalRow: List<Double>,
        val syncTimestamp: String
    ) : FinancialWidget()
}

data class AgentChatMessage(
    val id: String,
    val isUser: Boolean,
    val agentPersona: FinancialAgentPersona? = null,
    val text: String,
    val timestamp: String,
    val reasoningSteps: List<String> = emptyList(),
    val isReasoningExpanded: Boolean = false,
    val richWidget: FinancialWidget? = null,
    val followUpChips: List<String> = emptyList()
)
data class AgentChatSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val dateFormatted: String,
    val messages: List<AgentChatMessage>
)
