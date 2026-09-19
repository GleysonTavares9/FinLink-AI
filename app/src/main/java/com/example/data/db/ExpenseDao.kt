package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY transactionDate DESC, createdAt DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): ExpenseEntity?

    @Query("SELECT SUM(totalAmount) FROM expenses")
    fun getTotalSpent(): Flow<Double?>

    @Query("SELECT category, SUM(totalAmount) as total FROM expenses GROUP BY category")
    fun getCategoryTotals(): Flow<List<CategoryTotal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Query("SELECT * FROM expenses WHERE storeName LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY transactionDate DESC")
    fun searchExpenses(query: String): Flow<List<ExpenseEntity>>

    @Query("DELETE FROM expenses WHERE rawOcrText = 'Open Finance Oficial Mercado Pago'")
    suspend fun cleanLegacyMockExpenses()
}

data class CategoryTotal(
    val category: String,
    val total: Double
)
