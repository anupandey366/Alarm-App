package app.alarmsystem.alarms

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(alarm: Alarm) {
        val triggerTime = calculateTriggerTime(alarm)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            action = "ALARM_ACTION_${alarm.id}"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w("AlarmScheduler", "Exact alarm permission not granted")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    private fun calculateTriggerTime(alarm: Alarm): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = alarm.time
            val now = Calendar.getInstance()
            if (timeInMillis <= now.timeInMillis) {
                if (alarm.isRepeating && alarm.days.isNotEmpty()) {
                    val currentDay = now.get(Calendar.DAY_OF_WEEK) - 1
                    val nextDay = alarm.days.sorted().find { it > currentDay } ?: alarm.days.minOrNull()
                    nextDay?.let {
                        add(Calendar.DAY_OF_YEAR, if (it > currentDay) it - currentDay else 7 - (currentDay - it))
                    }
                } else {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
        }
        return calendar.timeInMillis
    }

    fun cancel(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

}