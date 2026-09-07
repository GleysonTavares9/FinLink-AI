package com.example.domain.model

/**
 * Tabela de custos básicos referenciais por indivíduo (inspirado em POF/IBGE e Cesta Básica DIEESE).
 * Permite customização e calibração periódica dos índices.
 */
data class HouseholdCostReference(
    val foodAdult: Double = 650.0,
    val foodChild: Double = 420.0,
    val housingAdult: Double = 500.0,
    val housingChild: Double = 150.0,
    val healthAdult: Double = 250.0,
    val healthChild: Double = 200.0,
    val transportAdult: Double = 280.0,
    val transportChild: Double = 100.0,
    val leisureAdult: Double = 180.0,
    val leisureChild: Double = 130.0,
    val otherAdult: Double = 140.0,
    val otherChild: Double = 100.0
) {
    fun calculateIdealBudget(adults: Int, children: Int, category: String): Double {
        val safeAdults = adults.coerceAtLeast(1)
        val safeChildren = children.coerceAtLeast(0)

        return when (category.lowercase()) {
            "mercado", "alimentação", "alimentacao" -> (safeAdults * foodAdult) + (safeChildren * foodChild)
            "moradia" -> (safeAdults * housingAdult) + (safeChildren * housingChild)
            "saúde", "saude" -> (safeAdults * healthAdult) + (safeChildren * healthChild)
            "transporte" -> (safeAdults * transportAdult) + (safeChildren * transportChild)
            "lazer" -> (safeAdults * leisureAdult) + (safeChildren * leisureChild)
            else -> (safeAdults * otherAdult) + (safeChildren * otherChild)
        }
    }

    fun calculateTotalIdealMonthlyBudget(adults: Int, children: Int): Double {
        val safeAdults = adults.coerceAtLeast(1)
        val safeChildren = children.coerceAtLeast(0)
        val totalPerAdult = foodAdult + housingAdult + healthAdult + transportAdult + leisureAdult + otherAdult
        val totalPerChild = foodChild + housingChild + healthChild + transportChild + leisureChild + otherChild
        return (safeAdults * totalPerAdult) + (safeChildren * totalPerChild)
    }
}

/**
 * Perfil da Composição Familiar da residência.
 */
data class HouseholdProfile(
    val id: Long = 1,
    val adultCount: Int = 1,
    val childCount: Int = 0,
    val monthlyIncome: Double = 0.0,
    val notes: String = "",
    val isConfigured: Boolean = false
) {
    val totalMembers: Int get() = adultCount + childCount
}

/**
 * Diagnóstico de uma categoria específica no orçamento familiar.
 */
data class CategoryBudgetStatus(
    val category: String,
    val actualSpent: Double,
    val recommendedBudget: Double,
    val perCapitaSpent: Double,
    val percentageUsed: Double,
    val isOverBudget: Boolean,
    val differenceAmount: Double
)

/**
 * Análise consolidada do orçamento familiar.
 */
data class BudgetAnalysis(
    val profile: HouseholdProfile,
    val totalActualSpent: Double,
    val totalRecommendedBudget: Double,
    val totalPerCapitaSpent: Double,
    val projectedMonthEndSpent: Double,
    val overallPercentage: Double,
    val categoryStatuses: List<CategoryBudgetStatus>,
    val summaryMessage: String,
    val actionableTips: List<String>
)
