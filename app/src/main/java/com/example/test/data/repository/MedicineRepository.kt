package com.example.test.data.repository

import com.example.test.data.dao.MedicineDao
import com.example.test.data.dao.MedicineIntakeDao
import com.example.test.data.dao.MedicineScheduleDao
import com.example.test.data.model.Medicine
import com.example.test.data.model.MedicineIntake
import com.example.test.data.model.MedicineSchedule
import com.example.test.data.model.MedicineWithSchedules
import kotlinx.coroutines.flow.Flow

class MedicineRepository(
    private val medicineDao: MedicineDao,
    private val scheduleDao: MedicineScheduleDao,
    private val intakeDao: MedicineIntakeDao
) {
    fun getAllMedicines(): Flow<List<Medicine>> = medicineDao.getAllMedicines()

    fun getAllMedicinesWithSchedules(): Flow<List<MedicineWithSchedules>> =
        medicineDao.getAllMedicinesWithSchedules()

    suspend fun getMedicineWithSchedules(medicineId: Long): MedicineWithSchedules? =
        medicineDao.getMedicineWithSchedules(medicineId)

    suspend fun insertMedicine(medicine: Medicine): Long =
        medicineDao.insertMedicine(medicine)

    suspend fun deleteMedicine(medicine: Medicine) =
        medicineDao.deleteMedicine(medicine)

    fun getSchedulesForMedicine(medicineId: Long): Flow<List<MedicineSchedule>> =
        scheduleDao.getSchedulesForMedicine(medicineId)

    suspend fun insertSchedule(schedule: MedicineSchedule): Long =
        scheduleDao.insertSchedule(schedule)

    suspend fun updateSchedule(schedule: MedicineSchedule) =
        scheduleDao.updateSchedule(schedule)

    suspend fun deleteSchedule(schedule: MedicineSchedule) =
        scheduleDao.deleteSchedule(schedule)

    suspend fun getAllEnabledSchedules(): List<MedicineSchedule> =
        scheduleDao.getAllEnabledSchedules()

    // Intake history methods
    suspend fun insertIntake(intake: MedicineIntake): Long =
        intakeDao.insert(intake)

    fun getIntakesByMedicine(medicineId: Long): Flow<List<MedicineIntake>> =
        intakeDao.getIntakesByMedicine(medicineId)

    fun getAllIntakes(): Flow<List<MedicineIntake>> =
        intakeDao.getAllIntakes()

    fun getIntakesByDateRange(startTime: Long, endTime: Long): Flow<List<MedicineIntake>> =
        intakeDao.getIntakesByDateRange(startTime, endTime)

    suspend fun deleteIntake(intake: MedicineIntake) =
        intakeDao.delete(intake)

    suspend fun getMissedIntakeCount(medicineId: Long): Int =
        intakeDao.getMissedIntakeCount(medicineId)

    suspend fun wasScheduleTakenToday(scheduleId: Long, startOfDay: Long, endOfDay: Long): Boolean =
        intakeDao.getIntakeTakenToday(scheduleId, startOfDay, endOfDay) != null

    suspend fun markScheduleAsTaken(medicineId: Long, scheduleId: Long, startOfDay: Long, endOfDay: Long) {
        // Check if there's already an intake record for today
        val existingIntake = intakeDao.getTodaysIntakeForSchedule(scheduleId, startOfDay, endOfDay)

        if (existingIntake != null) {
            // Update the existing record to mark as taken
            val updatedIntake = existingIntake.copy(
                wasTaken = true,
                takenTime = System.currentTimeMillis()
            )
            intakeDao.update(updatedIntake)
        } else {
            // No existing record, create a new one
            val newIntake = MedicineIntake(
                medicineId = medicineId,
                scheduleId = scheduleId,
                scheduledTime = System.currentTimeMillis(),
                takenTime = System.currentTimeMillis(),
                wasTaken = true
            )
            intakeDao.insert(newIntake)
        }
    }
}
