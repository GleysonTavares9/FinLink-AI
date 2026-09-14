package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OpenFinanceDao {
    // === CONTAS ===
    @Query("SELECT * FROM open_finance_accounts")
    fun getAllAccounts(): Flow<List<OpenFinanceAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: OpenFinanceAccountEntity)

    @Update
    suspend fun updateAccount(account: OpenFinanceAccountEntity)

    @Query("SELECT * FROM open_finance_accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: String): OpenFinanceAccountEntity?

    @Query("DELETE FROM open_finance_accounts WHERE id = :id")
    suspend fun deleteAccountById(id: String)

    @Query("DELETE FROM open_finance_accounts WHERE bankName LIKE '%MeuPluggy%' OR bankName LIKE '%Sandbox%'")
    suspend fun cleanLegacyMockAccounts()

    // === FATURAS (BILLS) ===
    @Query("SELECT * FROM open_finance_bills ORDER BY dueDate DESC")
    fun getAllBills(): Flow<List<OpenFinanceBillEntity>>

    @Query("SELECT * FROM open_finance_bills WHERE accountId = :accountId ORDER BY dueDate DESC")
    fun getBillsForAccount(accountId: String): Flow<List<OpenFinanceBillEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBills(bills: List<OpenFinanceBillEntity>)

    @Query("DELETE FROM open_finance_bills WHERE accountId = :accountId")
    suspend fun deleteBillsForAccount(accountId: String)

    // === TRANSAÇÕES & PARCELAS ===
    @Query("SELECT * FROM open_finance_transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<OpenFinanceTransactionEntity>>

    @Query("SELECT * FROM open_finance_transactions WHERE status = 'PENDING' ORDER BY date DESC")
    fun getPendingTransactions(): Flow<List<OpenFinanceTransactionEntity>>

    @Query("SELECT * FROM open_finance_transactions WHERE billForecastDate IS NOT NULL ORDER BY billForecastDate ASC, date DESC")
    fun getFutureInstallments(): Flow<List<OpenFinanceTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<OpenFinanceTransactionEntity>)

    @Query("DELETE FROM open_finance_transactions WHERE accountId = :accountId")
    suspend fun deleteTransactionsForAccount(accountId: String)

    @Query("UPDATE open_finance_transactions SET isSyncedToExpenses = 1 WHERE id = :id")
    suspend fun markTransactionSynced(id: String)
}
