package app.alarmsystem.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import app.alarmsystem.services.AlarmForegroundService
import app.alarmsystem.ui.AlarmActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("AlarmReceiver", "onReceive: Alarm triggered! Intent: ${intent?.action}")
        val wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp:AlarmWakeLock")
        wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/)
        try {
            val alarmId = intent?.getIntExtra("ALARM_ID", -1)
            if (alarmId == -1) return
            val serviceIntent = Intent(context, AlarmForegroundService::class.java).apply {
                putExtra("ALARM_ID", alarmId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } finally {
            wakeLock.release()
        }
    }
}
class AlarmDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        if (alarmId == -1) return
        CoroutineScope(Dispatchers.IO).launch {
            val alarm = AlarmRepository(context).getAlarmById(alarmId)
            val stopServiceIntent = Intent(context, AlarmForegroundService::class.java)
            context.stopService(stopServiceIntent)
            closeAlarmActivity(context)
            alarm?.let {
                withContext(Dispatchers.Main) {
                    AlarmScheduler(context).cancel(it)
                    Toast.makeText(context, "Alarm dismissed.", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }
}
class AlarmSnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        if (alarmId == -1) return
        CoroutineScope(Dispatchers.IO).launch {
            val alarm = AlarmRepository(context).getAlarmById(alarmId) ?: return@launch
            val stopServiceIntent = Intent(context, AlarmForegroundService::class.java)
            context.stopService(stopServiceIntent)
            closeAlarmActivity(context)
            val snoozeDuration = alarm.snoozeDuration * 60 * 1000L
            val snoozeAlarm = alarm.copy(
                time = System.currentTimeMillis() + snoozeDuration,
                isRepeating = false
            )
            withContext(Dispatchers.Main) {
                AlarmScheduler(context).schedule(snoozeAlarm)
                Toast.makeText(context, "Alarm snoozed for ${alarm.snoozeDuration} minutes", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                val repository = AlarmRepository(context)
                val scheduler = AlarmScheduler(context)
                repository.getAllAlarms().collect { alarms ->
                    alarms.forEach { alarm ->
                        if (alarm.isEnabled) {
                            scheduler.schedule(alarm)
                        }
                    }
                }
            }
        }
    }
}
private fun closeAlarmActivity(context: Context) {
    val closeIntent = Intent(context, AlarmActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        putExtra("FINISH_ALARM_ACTIVITY", true)
    }
    context.startActivity(closeIntent)
}