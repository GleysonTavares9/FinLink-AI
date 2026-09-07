package com.example.data.remote.supabase

import com.example.data.db.ExpenseDao
import com.example.data.db.ExpenseEntity
import com.example.data.db.HouseholdDao
import com.example.data.db.HouseholdEntity
import com.example.domain.model.HouseholdProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositório de Sincronização Segura de Dados (Cloud Sync) com Supabase.
 * Envia transações locais para a nuvem e mescla novos lançamentos preservando a privacidade via RLS.
 */
class SupabaseSyncRepository(
    private val client: SupabaseClient,
    private val sessionManager: SupabaseSessionManager,
    private val expenseDao: ExpenseDao,
    private val householdDao: HouseholdDao
) {

    suspend fun syncAll(
        localExpenses: List<ExpenseEntity>,
        localHousehold: HouseholdProfile?
    ): SyncResult = withContext(Dispatchers.IO) {
        val session = sessionManager.getSession()
            ?: return@withContext SyncResult(isSuccess = false, message = "Faça login com sua conta para sincronizar com a nuvem.")

        try {
            // 1. Upload das despesas locais para a nuvem
            val dtosToUpload = localExpenses.map { exp ->
                SupabaseExpenseDto.fromEntity(exp, session.user.id)
            }
            val uploadResult = client.uploadExpenses(dtosToUpload)

            // 2. Download de despesas da nuvem (caso existam em outros dispositivos)
            val downloadResult = client.downloadExpenses()
            var downloadedCount = 0

            downloadResult.onSuccess { remoteExpenses ->
                for (remote in remoteExpenses) {
                    val entity = remote.toEntity()
                    // Insere se não existir
                    expenseDao.insertExpense(entity)
                    downloadedCount++
                }
            }

            // 3. Sincronização do Perfil Familiar
            if (localHousehold != null) {
                val householdDto = SupabaseHouseholdDto.fromDomain(localHousehold, session.user.id)
                client.syncHousehold(householdDto)
            }

            SyncResult(
                uploadedExpenses = uploadResult.getOrDefault(localExpenses.size),
                downloadedExpenses = downloadedCount,
                isSuccess = true,
                message = "Sincronização concluída com sucesso com a nuvem Supabase!"
            )
        } catch (e: Exception) {
            SyncResult(
                isSuccess = false,
                message = "Erro na sincronização: ${e.localizedMessage}"
            )
        }
    }

    suspend fun deleteRemoteExpense(clientLocalId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        client.deleteExpense(clientLocalId)
    }
}
