package com.example.data.remote.supabase

import com.example.data.db.ExpenseEntity
import com.example.domain.model.HouseholdProfile

/**
 * Representa um usuário autenticado no Supabase Auth.
 */
data class SupabaseUser(
    val id: String,
    val email: String,
    val createdAt: String = ""
)

/**
 * Sessão autenticada contendo os tokens JWT emitidos pelo Supabase.
 */
data class SupabaseSession(
    val accessToken: String,
    val refreshToken: String,
    val user: SupabaseUser,
    val expiresAt: Long = 0
)

/**
 * Estado da Autenticação do Usuário.
 */
sealed interface AuthUiState {
    object Unauthenticated : AuthUiState
    object Loading : AuthUiState
    data class Authenticated(val session: SupabaseSession) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

/**
 * Resultado da Sincronização em Nuvem.
 */
data class SyncResult(
    val uploadedExpenses: Int = 0,
    val downloadedExpenses: Int = 0,
    val isSuccess: Boolean = true,
    val message: String = ""
)

/**
 * DTO para transações de Despesas via PostgREST.
 */
data class SupabaseExpenseDto(
    val id: Long? = null,
    val client_local_id: Long? = null,
    val user_id: String,
    val store_name: String,
    val total_amount: Double,
    val category: String,
    val payment_method: String,
    val transaction_date: String,
    val raw_ocr_text: String? = null,
    val receipt_image_path: String? = null,
    val is_manual: Boolean = true,
    val source: String = "MANUAL",
    val notes: String? = null,
    val items_json: String? = null
) {
    fun toEntity(): ExpenseEntity {
        return ExpenseEntity(
            id = client_local_id ?: id ?: 0L,
            storeName = store_name,
            totalAmount = total_amount,
            category = category,
            paymentMethod = payment_method,
            transactionDate = transaction_date,
            rawOcrText = raw_ocr_text,
            receiptImagePath = receipt_image_path,
            isManual = is_manual,
            source = source,
            notes = notes,
            itemsJson = items_json
        )
    }

    companion object {
        fun fromEntity(entity: ExpenseEntity, userId: String): SupabaseExpenseDto {
            return SupabaseExpenseDto(
                client_local_id = entity.id,
                user_id = userId,
                store_name = entity.storeName,
                total_amount = entity.totalAmount,
                category = entity.category,
                payment_method = entity.paymentMethod,
                transaction_date = entity.transactionDate,
                raw_ocr_text = entity.rawOcrText,
                receipt_image_path = entity.receiptImagePath,
                is_manual = entity.isManual,
                source = entity.source,
                notes = entity.notes,
                items_json = entity.itemsJson
            )
        }
    }
}

/**
 * DTO para Composição Familiar no Supabase.
 */
data class SupabaseHouseholdDto(
    val user_id: String,
    val adult_count: Int,
    val child_count: Int,
    val monthly_income: Double,
    val notes: String? = null
) {
    fun toDomain(): HouseholdProfile {
        return HouseholdProfile(
            id = 1L,
            adultCount = adult_count,
            childCount = child_count,
            monthlyIncome = monthly_income,
            notes = notes ?: "Família padrão"
        )
    }

    companion object {
        fun fromDomain(profile: HouseholdProfile, userId: String): SupabaseHouseholdDto {
            return SupabaseHouseholdDto(
                user_id = userId,
                adult_count = profile.adultCount,
                child_count = profile.childCount,
                monthly_income = profile.monthlyIncome,
                notes = profile.notes
            )
        }
    }
}
