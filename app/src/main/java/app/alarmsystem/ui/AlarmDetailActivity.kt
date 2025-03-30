package app.alarmsystem.ui

import android.app.AlarmManager
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import app.alarmsystem.R
import app.alarmsystem.alarms.Alarm
import app.alarmsystem.alarms.AlarmRepository
import app.alarmsystem.alarms.AlarmScheduler
import app.alarmsystem.databinding.ActivityAlarmDetailBinding
import app.alarmsystem.databinding.PopupLogoutBinding
import app.alarmsystem.utils.NotificationUtils.createNotificationChannel
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AlarmDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAlarmDetailBinding
    private lateinit var repository: AlarmRepository
    private lateinit var scheduler: AlarmScheduler
    private var alarmId: Int = -1
    private var hourGet: Int = 0
    private var minuteGet: Int = 0
    private var isEditMode = false
    private var selectedDays = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createNotificationChannel(this)
        requestExactAlarmPermission()

        repository = AlarmRepository(applicationContext)
        scheduler = AlarmScheduler(applicationContext)

        alarmId = intent.getIntExtra("ALARM_ID", -1)
        isEditMode = alarmId != -1

        Log.d("AlarmReceiver", "AlarmDetailActivity: alarmId $alarmId isEditMode $isEditMode")

        setupToolbar()
        setupDaysChips()
        setupSnoozeSlider()
        setupDifficultyOptions()

        if (isEditMode) {
            loadAlarm()
        }

        binding.timeEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val timePickerDialog = TimePickerDialog(this, { _, hour, minute ->
                val selectedTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                }
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val formattedTime = timeFormat.format(selectedTime.time)
                binding.timeEditText.setText(formattedTime)
                hourGet = hour
                minuteGet = minute
                Log.d("TAG12345", "timeEditText.setOnClickListener: hourGet $hourGet minuteGet $minuteGet")
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false)
            timePickerDialog.show()
        }
        binding.saveButton.setOnClickListener {
            saveAlarm()
        }
        binding.deleteButton.setOnClickListener {
            deleteAlarmDialog()
        }
        binding.deleteButton.visibility = if (isEditMode) View.VISIBLE else View.GONE

        selectedDays = mutableListOf<Int>().apply {
            for (i in 0 until binding.daysChipGroup.childCount) {
                val chip = binding.daysChipGroup.getChildAt(i) as Chip
                if (chip.isChecked) {
                    add(i)
                }
            }
        }

        binding.timeEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.timeInputLayout.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.labelEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.labelInputLayout.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun deleteAlarmDialog() {
        val dialog = Dialog(this)
        val popupBinding = PopupLogoutBinding.inflate(layoutInflater)
        dialog.setContentView(popupBinding.root)
        val window: Window? = dialog.window
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val params: WindowManager.LayoutParams? = window?.attributes
        params?.gravity = Gravity.BOTTOM
        dialog.window?.attributes = params
        binding.svAlarmDetails.alpha = 0.7f
        dialog.setOnDismissListener {
            binding.svAlarmDetails.alpha = 1.0f
        }
        popupBinding.tvCancel.setOnClickListener {
            dialog.dismiss()
        }
        popupBinding.tvYesLogout.setOnClickListener {
            dialog.dismiss()
            deleteAlarm()
        }
        dialog.show()
    }

    /*private fun deleteAlarmDialog(){
        AlertDialog.Builder(this)
            .setTitle("Delete Alarm")
            .setMessage("Are you sure you want to delete this alarm?")
            .setPositiveButton("Yes") { _, _ ->
                deleteAlarm()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }*/

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (isEditMode) {
            getString(R.string.edit_alarm_title)
        } else {
            getString(R.string.new_alarm_title)
        }
    }
    private fun setupDaysChips() {
        val days = resources.getStringArray(R.array.week_days)
        binding.daysChipGroup.removeAllViews()
        days.forEachIndexed { index, day ->
            val chip = Chip(this).apply {
                text = day
                isCheckable = true
                chipStrokeWidth = 1f
                setEnsureMinTouchTargetSize(true)

                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedDays.add(index)
                    } else {
                        selectedDays.remove(index)
                    }
                }
            }
            binding.daysChipGroup.addView(chip)
        }
    }

    private fun setupSnoozeSlider() {
        binding.snoozeValueText.text = "5 minutes"
        binding.snoozeSlider.addOnChangeListener { _, value, _ ->
            binding.snoozeValueText.text = resources.getQuantityString(
                R.plurals.minutes, value.toInt(), value.toInt()
            )
        }
    }

    private fun setupDifficultyOptions() {
        binding.noneRadio.isChecked = true
    }

    private fun loadAlarm() {
        lifecycleScope.launch(Dispatchers.IO) {
            val alarm = repository.getAlarmById(alarmId)
            withContext(Dispatchers.Main) {
                alarm?.let {
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = it.time
                    }
                    binding.timeEditText.setText(
                        SimpleDateFormat("h:mm a", Locale.getDefault()).format(calendar.time)
                    )
                    for (i in 0 until binding.daysChipGroup.childCount) {
                        val chip = binding.daysChipGroup.getChildAt(i) as? Chip
                        chip?.isChecked = it.days.contains(i)
                    }
                    binding.labelEditText.setText(it.label)
                    binding.vibrateSwitch.isChecked = it.isVibrate
                    binding.snoozeSlider.value = it.snoozeDuration.toFloat()
                    when (it.difficulty) {
                        0 -> binding.noneRadio.isChecked = true
                        1 -> binding.easyRadio.isChecked = true
                        2 -> binding.mediumRadio.isChecked = true
                        3 -> binding.hardRadio.isChecked = true
                    }
                }
            }
        }
    }
    private fun saveAlarm() {
        if (!validateAlarmFields()) {
            return
        }
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourGet)
            set(Calendar.MINUTE, minuteGet)
            set(Calendar.SECOND, 0)
            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val difficulty = when (binding.difficultyRadioGroup.checkedRadioButtonId) {
            R.id.easyRadio -> 1
            R.id.mediumRadio -> 2
            R.id.hardRadio -> 3
            else -> 0
        }
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val alarm = Alarm(
            id = if (isEditMode) alarmId else 0,
            time = calendar.timeInMillis,
            label = binding.labelEditText.text.toString(),
            days = selectedDays,
            isEnabled = true,
            isVibrate = binding.vibrateSwitch.isChecked,
            soundUri = defaultSoundUri?.toString() ?: "",
            snoozeDuration = binding.snoozeSlider.value.toInt(),
            isRepeating = selectedDays.isNotEmpty(),
            difficulty = difficulty
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (isEditMode) {
                    repository.updateAlarm(alarm)
                    scheduler.cancel(alarm)
                } else {
//                    alarmId = repository.insertAlarm(alarm).toInt()
                    val newId = repository.insertAlarm(alarm).toInt()
                    val alarmWithId = alarm.copy(id = newId)
                    scheduler.schedule(alarmWithId)
                    alarmId = newId
                }
                scheduler.schedule(alarm)

                withContext(Dispatchers.Main) {
                    Log.d("TAG12345", "saveAlarm: alarmId $alarmId")
                    Toast.makeText(this@AlarmDetailActivity, "Alarm ${if (isEditMode) "updated" else "set"}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AlarmDetailActivity, "Error saving alarm", Toast.LENGTH_SHORT).show()
                    Log.e("Alarm", "Save failed", e)
                }
            }
        }
    }

    private fun deleteAlarm() {
        lifecycleScope.launch(Dispatchers.IO) {
            repository.getAlarmById(alarmId)?.let { alarm ->
                scheduler.cancel(alarm)
                repository.deleteAlarm(alarm)
                withContext(Dispatchers.Main) {
                    finish()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun validateAlarmFields(): Boolean {
        if (binding.timeEditText.text.isNullOrBlank()) {
            binding.timeInputLayout.error = "Please select a time!"
            return false
        }else {
            binding.timeInputLayout.error = null
            true
        }
        if (binding.labelEditText.text.isNullOrBlank()) {
            binding.labelInputLayout.error = "Please enter an alarm label!"
            return false
        }else {
            binding.labelInputLayout.error = null
            true
        }
        if (selectedDays.isEmpty()) {
            Toast.makeText(this, "Please select at least one day", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }
}