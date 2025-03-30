package app.alarmsystem.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import app.alarmsystem.R
import app.alarmsystem.alarms.AlarmRepository
import app.alarmsystem.alarms.AlarmScheduler
import app.alarmsystem.databinding.ActivityAlarmListBinding
import app.alarmsystem.databinding.PopupLogoutBinding
import app.alarmsystem.ui.adapter.AlarmAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.net.toUri

class AlarmListActivity : AppCompatActivity() {
    private lateinit var binding : ActivityAlarmListBinding
    private lateinit var adapter: AlarmAdapter
    private lateinit var repository: AlarmRepository
    private lateinit var scheduler: AlarmScheduler

    override fun onResume() {
        super.onResume()

        setupRecyclerView()
        observeAlarms()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AlarmRepository(applicationContext)
        scheduler = AlarmScheduler(applicationContext)

        setupRecyclerView()
        observeAlarms()

        binding.addAlarmButton.setOnClickListener {
            startActivity(Intent(this, AlarmDetailActivity::class.java))
        }

        requestBatteryOptimizationDisable(this)

        if (!isNotificationPermissionGranted()) {
            requestNotificationPermission()
        }
    }

    private fun isNotificationPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            Toast.makeText(this, "Notification permission is not required for this version.", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notification Permission Granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notification Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestBatteryOptimizationDisable(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                showBatteryOptimizationDialog(context)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun showBatteryOptimizationDialog(context: Context) {
        AlertDialog.Builder(context).apply {
            setTitle("Important!")
            setMessage("To ensure uninterrupted service, please disable battery optimization for this app.")
            setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            setPositiveButton("Open Settings") { dialog, _ ->
                openBatteryOptimizationSettings(context)
                dialog.dismiss()
            }
            create().show()
        }
    }

    @SuppressLint("BatteryLife")
    private fun openBatteryOptimizationSettings(context: Context) {
        try {
            try {
                val oppoIntent = Intent().apply {
                    setClassName(
                        "com.coloros.oppoguardelf",
                        "com.coloros.powermanager.fuelgaue.PowerConsumptionActivity"
                    )
                }
                context.startActivity(oppoIntent)
            } catch (e: Exception) {
                val appSettings = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:${context.packageName}".toUri()
                }
                context.startActivity(appSettings)
            }
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = "package:${context.packageName}".toUri()
            }
            context.startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        adapter = AlarmAdapter(
            onAlarmToggle = { alarm, isChecked ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val updatedAlarm = alarm.copy(isEnabled = isChecked)
                    repository.updateAlarm(updatedAlarm)

                    if (isChecked) {
                        scheduler.schedule(updatedAlarm)
                    } else {
                        scheduler.cancel(updatedAlarm)
                    }
                }
            },
            onAlarmClick = { alarm ->
                val intent = Intent(this, AlarmDetailActivity::class.java).apply {
                    putExtra("ALARM_ID", alarm.id)
                }
                startActivity(intent)
            }
        )

        binding.alarmsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.alarmsRecyclerView.adapter = adapter
    }

    private fun observeAlarms() {
        lifecycleScope.launch {
            repository.getAllAlarms().collect { alarms ->
                adapter.submitList(alarms)
            }
        }
    }
}