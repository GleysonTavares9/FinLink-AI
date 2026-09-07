package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.HouseholdProfile

@Entity(tableName = "household_profile")
data class HouseholdEntity(
    @PrimaryKey val id: Long = 1L,
    val adultCount: Int = 1,
    val childCount: Int = 0,
    val monthlyIncome: Double = 0.0,
    val notes: String = "",
    val isConfigured: Boolean = true
) {
    fun toDomain(): HouseholdProfile {
        return HouseholdProfile(
            id = id,
            adultCount = adultCount,
            childCount = childCount,
            monthlyIncome = monthlyIncome,
            notes = notes,
            isConfigured = isConfigured
        )
    }

    companion object {
        fun fromDomain(profile: HouseholdProfile): HouseholdEntity {
            return HouseholdEntity(
                id = profile.id,
                adultCount = profile.adultCount,
                childCount = profile.childCount,
                monthlyIncome = profile.monthlyIncome,
                notes = profile.notes,
                isConfigured = true
            )
        }
    }
}
