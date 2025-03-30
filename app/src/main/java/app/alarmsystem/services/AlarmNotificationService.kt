package app.alarmsystem.services

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import app.alarmsystem.R
import app.alarmsystem.utils.Constants
import app.alarmsystem.utils.NotificationUtils

class AlarmNotificationService : Service() {
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var vibrator: Vibrator

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayer.create(this, R.raw.notification_ringtone).apply {
            isLooping = true
        }
        vibrator = getSystemService(Vibrator::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getIntExtra(Constants.ALARM_ID_EXTRA, -1) ?: -1
        if (alarmId != -1) {
            startForeground(alarmId, NotificationUtils.createAlarmNotification(this, alarmId))
            playAlarm()
        }
        return START_STICKY
    }

    private fun playAlarm() {
        mediaPlayer.start()
        val pattern = Constants.VIBRATE_PATTERN.split(",").map { it.toLong() }.toLongArray()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            vibrator.vibrate(pattern, 0)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.stop()
        mediaPlayer.release()
        vibrator.cancel()
    }
}