package com.example.test.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.test.data.database.MedicineDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {

            // Reschedule all alarms after reboot
            val database = MedicineDatabase.getDatabase(context)
            val alarmScheduler = AlarmScheduler(context)

            CoroutineScope(Dispatchers.IO).launch {
                val medicinesWithSchedules = database.medicineDao().getAllMedicinesWithSchedulesList()
                medicinesWithSchedules.forEach { medicineWithSchedules ->
                    medicineWithSchedules.schedules.forEach { schedule ->
                        alarmScheduler.scheduleAlarm(schedule, medicineWithSchedules.medicine.name)
                    }
                }
            }
        }
    }
}
