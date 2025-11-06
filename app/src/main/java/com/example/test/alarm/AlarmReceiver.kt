package com.example.test.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.test.MainActivity
import com.example.test.R
import com.example.test.data.database.MedicineDatabase
import com.example.test.data.model.MedicineIntake
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Acquire wake lock to ensure processing completes
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MedicineReminder::AlarmWakeLock"
        )
        wakeLock.acquire(10 * 60 * 1000L) // 10 minutes max

        try {
            val medicineName = intent.getStringExtra("MEDICINE_NAME") ?: "药品"
            val scheduleId = intent.getLongExtra("SCHEDULE_ID", 0)
            val medicineId = intent.getLongExtra("MEDICINE_ID", 0)
            val intervalDays = intent.getIntExtra("INTERVAL_DAYS", 1)
            val scheduledTime = System.currentTimeMillis()

            // Calculate start and end of today
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis

            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val endOfDay = calendar.timeInMillis

            val database = MedicineDatabase.getDatabase(context)

            // Check if already taken today
            val existingIntake = runBlocking {
                database.medicineIntakeDao().getTodaysIntakeForSchedule(scheduleId, startOfDay, endOfDay)
            }

            // If already taken, skip notification
            if (existingIntake != null && existingIntake.wasTaken) {
                // Already taken, don't notify
                rescheduleForNextOccurrence(context, intent, intervalDays)
                return
            }

            // Create a "not taken" intake record only if one doesn't exist
            if (existingIntake == null) {
                val intake = MedicineIntake(
                    medicineId = medicineId,
                    scheduleId = scheduleId,
                    scheduledTime = scheduledTime,
                    takenTime = 0, // Not taken yet
                    wasTaken = false
                )

                CoroutineScope(Dispatchers.IO).launch {
                    database.medicineIntakeDao().insert(intake)
                }
            }

            createNotificationChannel(context)
            showNotification(context, medicineName, medicineId, scheduleId, scheduledTime)

            // Reschedule for next occurrence based on interval
            rescheduleForNextOccurrence(context, intent, intervalDays)
        } finally {
            // Release wake lock
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    private fun rescheduleForNextOccurrence(context: Context, originalIntent: Intent, intervalDays: Int) {
        val scheduleId = originalIntent.getLongExtra("SCHEDULE_ID", 0)
        val medicineId = originalIntent.getLongExtra("MEDICINE_ID", 0)
        val medicineName = originalIntent.getStringExtra("MEDICINE_NAME") ?: "药品"

        // Get current scheduled time and add the interval
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("SCHEDULE_ID", scheduleId)
            putExtra("MEDICINE_ID", medicineId)
            putExtra("MEDICINE_NAME", medicineName)
            putExtra("INTERVAL_DAYS", intervalDays)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduleId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculate next alarm time based on interval
        val nextAlarmTime = System.currentTimeMillis() + (AlarmManager.INTERVAL_DAY * intervalDays)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextAlarmTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                nextAlarmTime,
                pendingIntent
            )
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "吃药提醒"
            val descriptionText = "药品提醒通知"
            val importance = NotificationManager.IMPORTANCE_HIGH // Use MAX for full-screen

            // Try to use custom sound, fall back to default if not found
            val soundUri = try {
                val customSoundId = context.resources.getIdentifier("alarm_sound", "raw", context.packageName)
                if (customSoundId != 0) {
                    Uri.parse("android.resource://${context.packageName}/$customSoundId")
                } else {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }
            } catch (e: Exception) {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel("MEDICINE_REMINDER", name, importance).apply {
                description = descriptionText
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(context: Context, medicineName: String, medicineId: Long, notificationId: Long, scheduledTime: Long) {
        // Check for notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAVIGATE_TO_MEDICINE_ID", medicineId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Sound URI for pre-Android O devices - try custom, fall back to default
        val soundUri = try {
            val customSoundId = context.resources.getIdentifier("alarm_sound", "raw", context.packageName)
            if (customSoundId != 0) {
                Uri.parse("android.resource://${context.packageName}/$customSoundId")
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
        } catch (e: Exception) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        val notification = NotificationCompat.Builder(context, "MEDICINE_REMINDER")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("吃药提醒")
            .setContentText("该吃 $medicineName 了")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Auto-cancel when clicked
            .setSound(soundUri) // Sound for pre-Android O
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId.toInt(), notification)
        }
    }
}
