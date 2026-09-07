package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseholdDao {

    @Query("SELECT * FROM household_profile WHERE id = 1 LIMIT 1")
    fun getHouseholdProfile(): Flow<HouseholdEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: HouseholdEntity)
}
