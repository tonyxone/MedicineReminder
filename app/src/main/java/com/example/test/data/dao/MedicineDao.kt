package com.example.test.data.dao

import androidx.room.*
import com.example.test.data.model.Medicine
import com.example.test.data.model.MedicineWithSchedules
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines ORDER BY createdAt DESC")
    fun getAllMedicines(): Flow<List<Medicine>>

    @Transaction
    @Query("SELECT * FROM medicines ORDER BY createdAt DESC")
    fun getAllMedicinesWithSchedules(): Flow<List<MedicineWithSchedules>>

    @Transaction
    @Query("SELECT * FROM medicines ORDER BY createdAt DESC")
    suspend fun getAllMedicinesWithSchedulesList(): List<MedicineWithSchedules>

    @Transaction
    @Query("SELECT * FROM medicines WHERE id = :medicineId")
    suspend fun getMedicineWithSchedules(medicineId: Long): MedicineWithSchedules?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: Medicine): Long

    @Delete
    suspend fun deleteMedicine(medicine: Medicine)

    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun getMedicineById(id: Long): Medicine?
}
