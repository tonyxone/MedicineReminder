package com.example.test.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class MedicineWithSchedules(
    @Embedded val medicine: Medicine,
    @Relation(
        parentColumn = "id",
        entityColumn = "medicineId"
    )
    val schedules: List<MedicineSchedule>
)
