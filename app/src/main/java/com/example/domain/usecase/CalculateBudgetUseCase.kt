package com.example.domain.usecase

import com.example.data.db.ExpenseEntity
import com.example.data.model.TransactionType
import com.example.data.model.UnifiedTransaction
import com.example.domain.model.BudgetAnalysis
import com.example.domain.model.CategoryBudgetStatus
import com.example.domain.model.HouseholdCostReference
import com.example.domain.model.HouseholdProfile
import com.example.util.formatBr
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Caso de Uso: CalculateBudgetUseCase
 * Calcula o orçamento e projeção financeira baseando-se na composição familiar (adultos e crianças),
 * CONECTANDO 100% COM O OPEN FINANCE E TRANSAÇÕES UNIFICADAS:
 * - Faturas de cartão, contas bancárias conectadas (Nubank, Mercado Pago) e cupons fiscais comunicam-se diretamente com o orçamento.
 * - Tetos categoriais são frações proporcionais da renda familiar real.
 * - Discrimina valores de Open Finance vs Notas Fiscais para rastreabilidade total.
 */
class CalculateBudgetUseCase(
    private val costReference: HouseholdCostReference = HouseholdCostReference()
) {

    /**
     * Executa a análise orçamentária integrando diretamente as transações do Open Finance e Cupons Fiscais.
     */
    fun executeWithUnified(
        transactions: List<UnifiedTransaction>,
        profile: HouseholdProfile,
        calendar: Calendar = Calendar.getInstance()
    ): BudgetAnalysis {
        val totalMembers = profile.totalMembers.coerceAtLeast(1)

        // Teto mensal ideal: prioriza a renda familiar declarada pelo usuário
        val idealBudget = if (profile.monthlyIncome > 0) {
            profile.monthlyIncome
        } else {
            costReference.calculateTotalIdealMonthlyBudget(profile.adultCount, profile.childCount)
        }

        // Filtra apenas SAÍDAS/DESPESAS (exclui receitas, salários e transferências internas de caixinhas/bolsos)
        val expenseTransactions = transactions.filter {
            it.type != TransactionType.ENTRADA &&
            it.type != TransactionType.TRANSFERENCIA &&
            !it.isInternalTransfer
        }

        // Filtra estritamente as despesas do MÊS ATUAL (competência corrente)
        val currentYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
        val currentMonthTxs = expenseTransactions.filter {
            it.date.isNotBlank() && it.date.startsWith(currentYearMonth)
        }

        val activeTxs = if (currentMonthTxs.isNotEmpty()) {
            currentMonthTxs
        } else {
            // Fallback caso estejamos em início de mês sem transações ainda
            val latestMonth = expenseTransactions.mapNotNull { it.date.takeIf { d -> d.length >= 7 }?.take(7) }.maxOrNull()
            if (latestMonth != null) {
                expenseTransactions.filter { it.date.startsWith(latestMonth) }
            } else {
                emptyList()
            }
        }

        // Segregação Open Finance vs Cupons Fiscais (NFC-e)
        val openFinanceTxs = activeTxs.filter {
            it.id.startsWith("of_") ||
            it.paymentMethod.contains("Open Finance", ignoreCase = true) ||
            it.notes?.contains("Mercado Pago", ignoreCase = true) == true ||
            it.notes?.contains("Nubank", ignoreCase = true) == true
        }
        val nfcTxs = activeTxs.filter { !openFinanceTxs.contains(it) }

        val openFinanceSpent = openFinanceTxs.sumOf { it.amount }
        val nfcSpent = nfcTxs.sumOf { it.amount }
        val totalSpent = activeTxs.sumOf { it.amount }

        val connectedBanks = activeTxs
            .map { it.bankName.trim() }
            .filter { it.isNotBlank() && it != "Dinheiro" && it != "Manual" }
            .distinct()

        val overallPercentage = if (idealBudget > 0) (totalSpent / idealBudget) * 100.0 else 0.0
        val totalPerCapitaSpent = totalSpent / totalMembers

        // Projeção fim de mês baseada no ritmo diário do mês ativo
        val maxDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH).coerceIn(1, maxDaysInMonth)
        val dailyAverage = if (dayOfMonth > 0) totalSpent / dayOfMonth else 0.0
        val projectedMonthEndSpent = dailyAverage * maxDaysInMonth

        // Agrupamento inteligente por categoria analisando descrição e categoria Open Finance
        val monitoredCategories = listOf("Mercado", "Alimentação", "Moradia", "Saúde", "Transporte", "Lazer", "Compras & Casa", "Outros")
        val expensesByCategory = activeTxs.groupBy { categorizeTransaction(it) }

        val categoryStatuses = monitoredCategories.map { cat ->
            val spentInCat = expensesByCategory[cat]?.sumOf { it.amount } ?: 0.0

            // Tetos proporcionais à renda familiar real
            val idealInCat = if (profile.monthlyIncome > 0) {
                val weight = when (cat) {
                    "Mercado" -> 0.28
                    "Alimentação" -> 0.10
                    "Moradia" -> 0.25
                    "Transporte" -> 0.12
                    "Saúde" -> 0.08
                    "Lazer" -> 0.07
                    "Compras & Casa" -> 0.06
                    else -> 0.04
                }
                profile.monthlyIncome * weight
            } else {
                costReference.calculateIdealBudget(profile.adultCount, profile.childCount, cat)
            }

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
        val summaryMessage = buildSummaryMessage(profile, totalSpent, idealBudget, overBudgetCategories, openFinanceTxs.size, nfcTxs.size)
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
            actionableTips = actionableTips,
            openFinanceTransactionsCount = openFinanceTxs.size,
            nfcReceiptsCount = nfcTxs.size,
            connectedBanks = connectedBanks,
            openFinanceSpent = openFinanceSpent,
            nfcSpent = nfcSpent
        )
    }

    /**
     * Compatibilidade legada para ExpenseEntity
     */
    fun execute(
        expenses: List<ExpenseEntity>,
        profile: HouseholdProfile,
        calendar: Calendar = Calendar.getInstance()
    ): BudgetAnalysis {
        val converted = expenses.map { exp ->
            UnifiedTransaction(
                id = exp.id.toString(),
                title = exp.storeName,
                subtitle = exp.category,
                amount = exp.totalAmount,
                type = TransactionType.SAIDA,
                date = exp.transactionDate,
                formattedDate = exp.transactionDate,
                dateGroup = "Recente",
                category = exp.category,
                paymentMethod = exp.paymentMethod
            )
        }
        return executeWithUnified(converted, profile, calendar)
    }

    private fun categorizeTransaction(tx: UnifiedTransaction): String {
        val text = "${tx.category} ${tx.title} ${tx.subtitle}".lowercase(Locale.getDefault())
        return when {
            text.contains("mercado") || text.contains("supermercado") ||
            text.contains("epa") || text.contains("mart minas") ||
            text.contains("carrefour") || text.contains("assai") ||
            text.contains("atacadao") || text.contains("sacolao") ||
            text.contains("hortifruti") || text.contains("acougue") ||
            text.contains("groceries") -> "Mercado"

            text.contains("restaurante") || text.contains("alimenta") ||
            text.contains("lanche") || text.contains("ifood") ||
            text.contains("mcdonald") || text.contains("burger") ||
            text.contains("pizza") || text.contains("padaria") ||
            text.contains("pao mais") || text.contains("pao de minas") ||
            text.contains("eating out") -> "Alimentação"

            text.contains("moradia") || text.contains("aluguel") ||
            text.contains("condominio") || text.contains("cemig") ||
            text.contains("copasa") || text.contains("energia") ||
            text.contains("luz") || text.contains("agua") ||
            text.contains("gas") || text.contains("claro") ||
            text.contains("vivo") || text.contains("tim") -> "Moradia"

            text.contains("saude") || text.contains("saúde") ||
            text.contains("farmacia") || text.contains("farmácia") ||
            text.contains("drogaria") || text.contains("lucena") ||
            text.contains("remedio") || text.contains("medico") ||
            text.contains("hospital") || text.contains("laboratorio") ||
            text.contains("cosmeti") || text.contains("pharmacy") -> "Saúde"

            text.contains("transporte") || text.contains("combust") ||
            text.contains("posto") || text.contains("gasolina") ||
            text.contains("etanol") || text.contains("uber") ||
            text.contains("99") || text.contains("pedagio") ||
            text.contains("estacionamento") -> "Transporte"

            text.contains("lazer") || text.contains("netflix") ||
            text.contains("spotify") || text.contains("cinema") ||
            text.contains("streaming") || text.contains("google") ||
            text.contains("entretenimento") || text.contains("viagem") ||
            text.contains("show") -> "Lazer"

            text.contains("compras") || text.contains("shopping") ||
            text.contains("mercadolivre") || text.contains("shopee") ||
            text.contains("amazon") || text.contains("marisa") ||
            text.contains("calcados") || text.contains("vestuario") ||
            text.contains("roupa") || text.contains("eletron") -> "Compras & Casa"

            else -> "Outros"
        }
    }

    private fun buildSummaryMessage(
        profile: HouseholdProfile,
        totalSpent: Double,
        idealBudget: Double,
        overCategories: List<CategoryBudgetStatus>,
        openFinanceCount: Int,
        nfcCount: Int
    ): String {
        val familyDesc = "${profile.adultCount} adulto(s) e ${profile.childCount} dependente(s)"
        val sourceInfo = if (openFinanceCount > 0) " com $openFinanceCount lançamentos via Open Finance e $nfcCount via notas fiscais" else ""
        return if (overCategories.isEmpty()) {
            "Excelente controle! O orçamento doméstico ($familyDesc)$sourceInfo está dentro do teto planejado (${formatBr(totalSpent)} de ${formatBr(idealBudget)})."
        } else {
            val names = overCategories.joinToString(", ") { it.category }
            "Atenção: Os gastos consolidados no Open Finance ($familyDesc)$sourceInfo ultrapassaram o teto sugerido em: $names."
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
            tips.add("No ritmo atual do Open Finance, a projeção do mês fechará em ${formatBr(projectedMonthEnd)} (+${String.format(Locale.US, "%.0f", excessPercent)}% acima da renda familiar).")
        }

        val mercadoOver = overCategories.find { it.category == "Mercado" }
        if (mercadoOver != null) {
            val perCapita = mercadoOver.perCapitaSpent
            tips.add("Gasto em Mercado por pessoa está em ${formatBr(perCapita)}. Acompanhe os cupons de atacado para economizar até 18%.")
        }

        if (profile.childCount > 0) {
            tips.add("Para famílias com ${profile.childCount} dependente(s), controle faturas do cartão vinculadas a vestuário e itens infantis.")
        } else {
            tips.add("Para residência com ${profile.adultCount} adulto(s), revise assinaturas recorrentes e débitos automáticos.")
        }

        if (tips.isEmpty()) {
            tips.add("Seus dados bancários e notas fiscais estão em perfeito equilíbrio com a renda declarada.")
        }

        return tips
    }
}
