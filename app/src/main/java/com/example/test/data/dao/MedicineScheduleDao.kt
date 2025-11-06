package com.example.test.data.dao

import androidx.room.*
import com.example.test.data.model.MedicineSchedule
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineScheduleDao {
    @Query("SELECT * FROM medicine_schedules WHERE medicineId = :medicineId")
    fun getSchedulesForMedicine(medicineId: Long): Flow<List<MedicineSchedule>>

    @Query("SELECT * FROM medicine_schedules WHERE isEnabled = 1")
    suspend fun getAllEnabledSchedules(): List<MedicineSchedule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: MedicineSchedule): Long

    @Update
    suspend fun updateSchedule(schedule: MedicineSchedule)

    @Delete
    suspend fun deleteSchedule(schedule: MedicineSchedule)

    @Query("SELECT * FROM medicine_schedules WHERE id = :id")
    suspend fun getScheduleById(id: Long): MedicineSchedule?
}
