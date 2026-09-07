package com.example

import com.example.data.db.ExpenseEntity
import com.example.domain.model.HouseholdCostReference
import com.example.domain.model.HouseholdProfile
import com.example.domain.usecase.CalculateBudgetUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CalculateBudgetUseCaseTest {

    private val costReference = HouseholdCostReference(
        foodAdult = 600.0,
        foodChild = 400.0,
        housingAdult = 500.0,
        housingChild = 100.0
    )

    private val useCase = CalculateBudgetUseCase(costReference)

    @Test
    fun testBudgetCalculationForTwoAdultsAndOneChild() {
        val profile = HouseholdProfile(
            adultCount = 2,
            childCount = 1,
            monthlyIncome = 6000.0
        )

        // Alimentação esperada: 2 * 600 + 1 * 400 = 1600.0
        val idealFood = costReference.calculateIdealBudget(2, 1, "Mercado")
        assertEquals(1600.0, idealFood, 0.001)

        val expenses = listOf(
            ExpenseEntity(
                storeName = "Supermercado A",
                totalAmount = 800.0,
                category = "Mercado",
                transactionDate = "2026-03-01",
                isManual = true
            ),
            ExpenseEntity(
                storeName = "Supermercado B",
                totalAmount = 900.0,
                category = "Mercado",
                transactionDate = "2026-03-10",
                isManual = false
            )
        )

        // Mock calendar no dia 15 de um mês de 30 dias
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 15)
        }

        val analysis = useCase.execute(expenses, profile, calendar)

        assertNotNull(analysis)
        assertEquals(1700.0, analysis.totalActualSpent, 0.001)
        // 3 membros (2 adultos + 1 criança) => 1700 / 3 = 566.66...
        assertEquals(1700.0 / 3.0, analysis.totalPerCapitaSpent, 0.01)

        // Mercado gastou 1700, ideal era 1600 => ultrapassou
        val mercadoStatus = analysis.categoryStatuses.find { it.category == "Mercado" }
        assertNotNull(mercadoStatus)
        assertTrue(mercadoStatus!!.isOverBudget)
        assertEquals(100.0, mercadoStatus.differenceAmount, 0.01)

        // No dia 15 gastou 1700, projeção no dia 30 é ~3400
        assertTrue(analysis.projectedMonthEndSpent > 3000.0)
    }
}
