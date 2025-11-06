package com.example.test.data.dao

import androidx.room.*
import com.example.test.data.model.MedicineIntake
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineIntakeDao {
    @Insert
    suspend fun insert(intake: MedicineIntake): Long

    @Update
    suspend fun update(intake: MedicineIntake)

    @Query("SELECT * FROM medicine_intakes WHERE scheduleId = :scheduleId AND scheduledTime = :scheduledTime LIMIT 1")
    suspend fun getIntakeByScheduleAndTime(scheduleId: Long, scheduledTime: Long): MedicineIntake?

    @Query("SELECT * FROM medicine_intakes WHERE medicineId = :medicineId ORDER BY scheduledTime DESC")
    fun getIntakesByMedicine(medicineId: Long): Flow<List<MedicineIntake>>

    @Query("SELECT * FROM medicine_intakes ORDER BY scheduledTime DESC")
    fun getAllIntakes(): Flow<List<MedicineIntake>>

    @Query("SELECT * FROM medicine_intakes WHERE scheduledTime >= :startTime AND scheduledTime < :endTime ORDER BY scheduledTime DESC")
    fun getIntakesByDateRange(startTime: Long, endTime: Long): Flow<List<MedicineIntake>>

    @Query("SELECT COUNT(*) FROM medicine_intakes WHERE medicineId = :medicineId AND wasTaken = 0")
    suspend fun getMissedIntakeCount(medicineId: Long): Int

    @Query("SELECT * FROM medicine_intakes WHERE scheduleId = :scheduleId AND wasTaken = 1 AND takenTime >= :startOfDay AND takenTime < :endOfDay LIMIT 1")
    suspend fun getIntakeTakenToday(scheduleId: Long, startOfDay: Long, endOfDay: Long): MedicineIntake?

    @Query("SELECT * FROM medicine_intakes WHERE scheduleId = :scheduleId AND scheduledTime >= :startOfDay AND scheduledTime < :endOfDay LIMIT 1")
    suspend fun getTodaysIntakeForSchedule(scheduleId: Long, startOfDay: Long, endOfDay: Long): MedicineIntake?

    @Delete
    suspend fun delete(intake: MedicineIntake)
}
