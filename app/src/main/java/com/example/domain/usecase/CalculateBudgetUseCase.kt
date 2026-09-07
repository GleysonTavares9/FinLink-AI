package com.example.domain.usecase

import com.example.data.db.ExpenseEntity
import com.example.domain.model.BudgetAnalysis
import com.example.domain.model.CategoryBudgetStatus
import com.example.domain.model.HouseholdCostReference
import com.example.domain.model.HouseholdProfile
import com.example.util.formatBr
import java.util.Calendar
import java.util.Locale

/**
 * Caso de Uso: CalculateBudgetUseCase
 * Calcula o orçamento e projeção financeira baseando-se na composição familiar (adultos e crianças),
 * identificando desvios per capita e gerando diagnósticos objetivos.
 */
class CalculateBudgetUseCase(
    private val costReference: HouseholdCostReference = HouseholdCostReference()
) {

    fun execute(
        expenses: List<ExpenseEntity>,
        profile: HouseholdProfile,
        calendar: Calendar = Calendar.getInstance()
    ): BudgetAnalysis {
        val totalMembers = profile.totalMembers.coerceAtLeast(1)
        val idealBudget = if (profile.monthlyIncome > 0) {
            profile.monthlyIncome
        } else {
            costReference.calculateTotalIdealMonthlyBudget(profile.adultCount, profile.childCount)
        }

        val totalSpent = expenses.sumOf { it.totalAmount }
        val overallPercentage = if (idealBudget > 0) (totalSpent / idealBudget) * 100.0 else 0.0
        val totalPerCapitaSpent = totalSpent / totalMembers

        // Projeção fim de mês
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        val maxDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dailyAverage = totalSpent / dayOfMonth
        val projectedMonthEndSpent = dailyAverage * maxDaysInMonth

        // Agrupamento por categoria
        val monitoredCategories = listOf("Mercado", "Alimentação", "Moradia", "Saúde", "Transporte", "Lazer", "Outros")
        val expensesByCategory = expenses.groupBy { normalizeCategory(it.category) }

        val categoryStatuses = monitoredCategories.map { cat ->
            val spentInCat = expensesByCategory[normalizeCategory(cat)]?.sumOf { it.totalAmount } ?: 0.0
            val idealInCat = costReference.calculateIdealBudget(profile.adultCount, profile.childCount, cat)
            val perCapita = spentInCat / totalMembers
            val percent = if (idealInCat > 0.0) (spentInCat / idealInCat) * 100.0 else 0.0
            val isOver = spentInCat > idealInCat
            val diff = spentInCat - idealInCat

            CategoryBudgetStatus(
                category = cat,
                actualSpent = spentInCat,
                recommendedBudget = idealInCat,
                perCapitaSpent = perCapita,
                percentageUsed = percent,
                isOverBudget = isOver,
                differenceAmount = diff
            )
        }

        val overBudgetCategories = categoryStatuses.filter { it.isOverBudget }
        val summaryMessage = buildSummaryMessage(profile, totalSpent, idealBudget, overBudgetCategories)
        val actionableTips = generateActionableTips(profile, overBudgetCategories, projectedMonthEndSpent, idealBudget)

        return BudgetAnalysis(
            profile = profile,
            totalActualSpent = totalSpent,
            totalRecommendedBudget = idealBudget,
            totalPerCapitaSpent = totalPerCapitaSpent,
            projectedMonthEndSpent = projectedMonthEndSpent,
            overallPercentage = overallPercentage,
            categoryStatuses = categoryStatuses,
            summaryMessage = summaryMessage,
            actionableTips = actionableTips
        )
    }

    private fun normalizeCategory(cat: String): String {
        val lower = cat.trim().lowercase(Locale.getDefault())
        return when {
            lower.contains("mercado") || lower.contains("supermercado") -> "Mercado"
            lower.contains("alimenta") || lower.contains("restaurante") -> "Alimentação"
            lower.contains("moradia") || lower.contains("casa") -> "Moradia"
            lower.contains("saude") || lower.contains("saúde") || lower.contains("farmacia") -> "Saúde"
            lower.contains("transporte") || lower.contains("combust") -> "Transporte"
            lower.contains("lazer") || lower.contains("viagem") -> "Lazer"
            else -> "Outros"
        }
    }

    private fun buildSummaryMessage(
        profile: HouseholdProfile,
        totalSpent: Double,
        idealBudget: Double,
        overCategories: List<CategoryBudgetStatus>
    ): String {
        val familyDesc = "${profile.adultCount} adulto(s) e ${profile.childCount} criança(s)"
        return if (overCategories.isEmpty()) {
            "Excelente controle! O orçamento para a sua casa ($familyDesc) está dentro do planejado (${formatBr(totalSpent)} de ${formatBr(idealBudget)})."
        } else {
            val names = overCategories.joinToString(", ") { it.category }
            "Atenção: Os gastos na composição ($familyDesc) ultrapassaram o teto sugerido em: $names."
        }
    }

    private fun generateActionableTips(
        profile: HouseholdProfile,
        overCategories: List<CategoryBudgetStatus>,
        projectedMonthEnd: Double,
        idealBudget: Double
    ): List<String> {
        val tips = mutableListOf<String>()

        if (projectedMonthEnd > idealBudget && idealBudget > 0.0) {
            val excessPercent = ((projectedMonthEnd - idealBudget) / idealBudget) * 100.0
            tips.add("No ritmo atual, a projeção do mês fechará em ${formatBr(projectedMonthEnd)} (+${String.format(Locale.US, "%.0f", excessPercent)}% acima do ideal).")
        }

        val mercadoOver = overCategories.find { it.category == "Mercado" }
        if (mercadoOver != null) {
            val perCapita = mercadoOver.perCapitaSpent
            tips.add("Gasto em Mercado por pessoa está em ${formatBr(perCapita)}. Utilize o escaneamento de NFC-e para comparar preços de atacarejos e marcas próprias.")
        }

        if (profile.childCount > 0) {
            tips.add("Para famílias com ${profile.childCount} criança(s), priorize compras planejadas de lanches escolares e itens de higiene infantil no atacado.")
        } else {
            tips.add("Para residência com ${profile.adultCount} adulto(s), revise assinaturas mensais e entregas por aplicativo para liberar até 15% do orçamento.")
        }

        if (tips.isEmpty()) {
            tips.add("Mantenha a rotina diária de registrar cupons fiscais e sincronizar suas contas para manter o orçamento verde.")
        }

        return tips
    }
}
