package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OpenFinanceDao {
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

    @Query("SELECT * FROM open_finance_transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<OpenFinanceTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<OpenFinanceTransactionEntity>)

    @Query("UPDATE open_finance_transactions SET isSyncedToExpenses = 1 WHERE id = :id")
    suspend fun markTransactionSynced(id: String)
}
