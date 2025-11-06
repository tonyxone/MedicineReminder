package com.example.test.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.test.data.database.MedicineDatabase
import com.example.test.data.model.Medicine
import com.example.test.data.model.MedicineIntake
import com.example.test.data.model.MedicineSchedule
import com.example.test.data.model.MedicineWithSchedules
import com.example.test.data.repository.MedicineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MedicineViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MedicineRepository

    private val _medicines = MutableStateFlow<List<MedicineWithSchedules>>(emptyList())
    val medicines: StateFlow<List<MedicineWithSchedules>> = _medicines.asStateFlow()

    private val _selectedMedicine = MutableStateFlow<MedicineWithSchedules?>(null)
    val selectedMedicine: StateFlow<MedicineWithSchedules?> = _selectedMedicine.asStateFlow()

    private val _intakeHistory = MutableStateFlow<List<MedicineIntake>>(emptyList())
    val intakeHistory: StateFlow<List<MedicineIntake>> = _intakeHistory.asStateFlow()

    init {
        val database = MedicineDatabase.getDatabase(application)
        repository = MedicineRepository(
            database.medicineDao(),
            database.medicineScheduleDao(),
            database.medicineIntakeDao()
        )

        viewModelScope.launch {
            repository.getAllMedicinesWithSchedules().collect {
                _medicines.value = it
            }
        }

        viewModelScope.launch {
            repository.getAllIntakes().collect {
                _intakeHistory.value = it
            }
        }
    }

    fun addMedicine(name: String, onSuccess: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val medicine = Medicine(name = name)
            val id = repository.insertMedicine(medicine)
            onSuccess(id)
        }
    }

    fun deleteMedicine(medicine: Medicine) {
        viewModelScope.launch {
            repository.deleteMedicine(medicine)
        }
    }

    fun addSchedule(medicineId: Long, hour: Int, minute: Int, intervalDays: Int = 1) {
        viewModelScope.launch {
            val schedule = MedicineSchedule(
                medicineId = medicineId,
                hour = hour,
                minute = minute,
                intervalDays = intervalDays
            )
            repository.insertSchedule(schedule)
            // Reload the medicine to refresh the UI
            _selectedMedicine.value = repository.getMedicineWithSchedules(medicineId)
        }
    }

    fun updateSchedule(schedule: MedicineSchedule) {
        viewModelScope.launch {
            repository.updateSchedule(schedule)
            // Reload the medicine to refresh the UI
            _selectedMedicine.value = repository.getMedicineWithSchedules(schedule.medicineId)
        }
    }

    fun deleteSchedule(schedule: MedicineSchedule) {
        viewModelScope.launch {
            repository.deleteSchedule(schedule)
            // Reload the medicine to refresh the UI
            _selectedMedicine.value = repository.getMedicineWithSchedules(schedule.medicineId)
        }
    }

    fun loadMedicineWithSchedules(medicineId: Long) {
        viewModelScope.launch {
            _selectedMedicine.value = repository.getMedicineWithSchedules(medicineId)
        }
    }

    fun clearSelectedMedicine() {
        _selectedMedicine.value = null
    }

    fun loadIntakesByDateRange(startTime: Long, endTime: Long) {
        viewModelScope.launch {
            repository.getIntakesByDateRange(startTime, endTime).collect {
                _intakeHistory.value = it
            }
        }
    }

    suspend fun getMissedIntakeCount(medicineId: Long): Int {
        return repository.getMissedIntakeCount(medicineId)
    }

    fun markScheduleAsTaken(medicineId: Long, scheduleId: Long, scheduledTime: Long) {
        viewModelScope.launch {
            // Calculate start and end of today
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis

            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
            val endOfDay = calendar.timeInMillis

            // Use repository method to update or insert
            repository.markScheduleAsTaken(medicineId, scheduleId, startOfDay, endOfDay)

            // Reload the medicine to refresh the UI
            _selectedMedicine.value = repository.getMedicineWithSchedules(medicineId)
        }
    }

    suspend fun wasScheduleTakenToday(scheduleId: Long): Boolean {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis

        return repository.wasScheduleTakenToday(scheduleId, startOfDay, endOfDay)
    }
}
