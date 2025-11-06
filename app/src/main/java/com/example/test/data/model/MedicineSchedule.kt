package com.example.test.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "medicine_schedules",
    foreignKeys = [
        ForeignKey(
            entity = Medicine::class,
            parentColumns = ["id"],
            childColumns = ["medicineId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MedicineSchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicineId: Long,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true,
    val intervalDays: Int = 1  // Number of days between reminders (1 = daily, 2 = every 2 days, etc.)
)
