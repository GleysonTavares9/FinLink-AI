package com.example

import com.example.data.db.ExpenseEntity
import com.example.data.remote.supabase.SupabaseExpenseDto
import com.example.data.remote.supabase.SupabaseHouseholdDto
import com.example.data.remote.supabase.SupabaseSession
import com.example.data.remote.supabase.SupabaseUser
import com.example.domain.model.HouseholdProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SupabaseModelsTest {

    @Test
    fun testExpenseDtoConversion() {
        val entity = ExpenseEntity(
            id = 42L,
            storeName = "Supermercado Seguro",
            totalAmount = 149.90,
            category = "Mercado",
            paymentMethod = "PIX",
            transactionDate = "2026-03-01",
            rawOcrText = "NFC-e 123",
            isManual = false,
            source = "NFC-E_SEFAZ",
            notes = "Compra mensal",
            itemsJson = "Arroz | Feijao"
        )

        val userId = "user_uuid_12345"
        val dto = SupabaseExpenseDto.fromEntity(entity, userId)

        assertEquals(userId, dto.user_id)
        assertEquals(42L, dto.client_local_id)
        assertEquals("Supermercado Seguro", dto.store_name)
        assertEquals(149.90, dto.total_amount, 0.001)

        val restoredEntity = dto.toEntity()
        assertEquals(42L, restoredEntity.id)
        assertEquals("Supermercado Seguro", restoredEntity.storeName)
        assertEquals("Mercado", restoredEntity.category)
        assertEquals("NFC-E_SEFAZ", restoredEntity.source)
    }

    @Test
    fun testHouseholdDtoConversion() {
        val profile = HouseholdProfile(
            adultCount = 3,
            childCount = 2,
            monthlyIncome = 8500.0,
            notes = "Família ampliada"
        )

        val userId = "user_uuid_9999"
        val dto = SupabaseHouseholdDto.fromDomain(profile, userId)

        assertEquals(userId, dto.user_id)
        assertEquals(3, dto.adult_count)
        assertEquals(2, dto.child_count)
        assertEquals(8500.0, dto.monthly_income, 0.001)

        val restored = dto.toDomain()
        assertEquals(3, restored.adultCount)
        assertEquals(2, restored.childCount)
        assertEquals(5, restored.totalMembers)
    }

    @Test
    fun testSupabaseSessionCreation() {
        val user = SupabaseUser(
            id = "uuid_user_1",
            email = "usuario@finlink.ai"
        )
        val session = SupabaseSession(
            accessToken = "jwt.token.here",
            refreshToken = "refresh.token.here",
            user = user,
            expiresAt = System.currentTimeMillis() + 3600000
        )

        assertNotNull(session)
        assertEquals("usuario@finlink.ai", session.user.email)
        assertEquals("jwt.token.here", session.accessToken)
    }
}
