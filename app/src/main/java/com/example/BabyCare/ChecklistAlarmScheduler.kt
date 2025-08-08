package com.example.babycare

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.*

object ChecklistAlarmScheduler {

    fun scheduleChecklistReminder(context: Context, checklistItem: ChecklistItem) {
        val (hour, minute) = checklistItem.time.split(":").mapNotNull { it.toIntOrNull() }
            .takeIf { it.size == 2 } ?: return

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_MONTH, 1)
        }

        val intent = Intent(context, ChecklistAlarmReceiver::class.java).apply {
            putExtra("title", checklistItem.title)
            putExtra("body", checklistItem.description ?: "🍼 הגיע הזמן לבצע משימה לתינוק")
            putExtra("babyId", checklistItem.babyId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            checklistItem.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }
}
