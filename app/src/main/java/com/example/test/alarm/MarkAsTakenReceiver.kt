package com.example.test.alarm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.test.data.database.MedicineDatabase
import com.example.test.data.model.MedicineIntake
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MarkAsTakenReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra("SCHEDULE_ID", 0)
        val scheduledTime = intent.getLongExtra("SCHEDULED_TIME", 0)
        val notificationId = intent.getIntExtra("NOTIFICATION_ID", 0)

        // Find and update the existing intake record
        val database = MedicineDatabase.getDatabase(context)

        CoroutineScope(Dispatchers.IO).launch {
            val intake = database.medicineIntakeDao().getIntakeByScheduleAndTime(scheduleId, scheduledTime)
            intake?.let {
                // Update the existing record to mark as taken
                val updatedIntake = it.copy(
                    takenTime = System.currentTimeMillis(),
                    wasTaken = true
                )
                database.medicineIntakeDao().update(updatedIntake)
            }
        }

        // Dismiss the notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }
}
