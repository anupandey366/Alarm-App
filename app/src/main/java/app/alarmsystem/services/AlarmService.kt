package app.alarmsystem.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import app.alarmsystem.R
import app.alarmsystem.alarms.AlarmDismissReceiver
import app.alarmsystem.alarms.AlarmRepository
import app.alarmsystem.alarms.AlarmSnoozeReceiver
import app.alarmsystem.ui.AlarmActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmForegroundService : Service() {
    private val notificationId = 1234
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var vibrator: Vibrator

    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayer()
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getIntExtra("ALARM_ID", -1) ?: -1
        if (alarmId == -1) {
            stopSelf()
            return START_NOT_STICKY
        }
        val notification = createNotification(alarmId)
        startForeground(notificationId, notification)
        CoroutineScope(Dispatchers.IO).launch {
            startAlarm(alarmId)
        }

        return START_STICKY
    }

    @SuppressLint("LaunchActivityFromNotification")
    private fun createNotification(alarmId: Int): Notification {
        val dismissIntent = Intent(this, AlarmDismissReceiver::class.java).apply {
            putExtra("ALARM_ID", alarmId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            alarmId,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, AlarmSnoozeReceiver::class.java).apply {
            putExtra("ALARM_ID", alarmId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this,
            alarmId + 1000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            alarmId,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "alarm_channel")
            .setContentTitle("Alarm")
            .setContentText("Time to wake up!")
            .setSmallIcon(R.drawable.homoeo_logo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.snooze, "Snooze", snoozePendingIntent)
            .addAction(R.drawable.dismiss, "Dismiss", dismissPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .build()
    }

    private suspend fun startAlarm(alarmId: Int) {
        val alarm = AlarmRepository(applicationContext).getAlarmById(alarmId)
        try {
            val soundUri = alarm?.soundUri?.takeIf { it.isNotEmpty() }?.let { Uri.parse(it) }
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer.apply {
                reset()
                setDataSource(applicationContext, soundUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Error playing alarm sound", e)
        }

        if (alarm?.isVibrate == true) {
            val pattern = longArrayOf(0, 1000, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                vibrator.vibrate(pattern, 0)
            }
        }
    }

    override fun onDestroy() {
        mediaPlayer.stop()
        mediaPlayer.release()
        vibrator.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}