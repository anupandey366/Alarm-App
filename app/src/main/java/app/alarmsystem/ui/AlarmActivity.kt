package app.alarmsystem.ui

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import app.alarmsystem.R
import app.alarmsystem.alarms.Alarm
import app.alarmsystem.alarms.AlarmDismissReceiver
import app.alarmsystem.alarms.AlarmReceiver
import app.alarmsystem.alarms.AlarmRepository
import app.alarmsystem.alarms.AlarmScheduler
import app.alarmsystem.alarms.AlarmSnoozeReceiver
import app.alarmsystem.databinding.ActivityAlarmBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmActivity : AppCompatActivity() {
    private lateinit var binding : ActivityAlarmBinding
    private lateinit var alarm: Alarm
    private lateinit var repository: AlarmRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent?.getBooleanExtra("FINISH_ALARM_ACTIVITY", false) == true) {
            finish()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                )

        repository = AlarmRepository(applicationContext)

        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        Log.d("TAG12345", "AlarmActivity onCreate: alarmId $alarmId")
        if (alarmId != -1) {
            loadAlarm(alarmId)
        } else {
            finish()
        }
    }

    private fun loadAlarm(alarmId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val loadedAlarm = repository.getAlarmById(alarmId)
            withContext(Dispatchers.Main) {
                loadedAlarm?.let {
                    alarm = it
                    updateUI()
                } ?: run {
                    finish()
                }
            }
        }
    }

    private fun updateUI() {
        val timeText = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(alarm.time))
        val dateText = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date(alarm.time))

        binding.timeText.text = timeText
        binding.dateText.text = dateText
        binding.labelText.text = alarm.label

        if (alarm.isRepeating) {
            binding.daysText.text = getDaysText(alarm.days)
            binding.daysText.visibility = View.VISIBLE
        } else {
            binding.daysText.visibility = View.GONE
        }

        binding.snoozeButton.setOnClickListener {
            val intent = Intent(this, AlarmSnoozeReceiver::class.java).apply {
                putExtra("ALARM_ID", alarm.id)
            }
            sendBroadcast(intent)
            finish()
        }

        binding.dismissButton.setOnClickListener {
            val intent = Intent(this, AlarmDismissReceiver::class.java).apply {
                putExtra("ALARM_ID", alarm.id)
            }
            sendBroadcast(intent)
            finish()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        })
    }

    private fun getDaysText(days: List<Int>): String {
        if (days.isEmpty()) return ""

        val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        return days.sorted().joinToString(", ") { dayNames[it] }
    }
}