package app.alarmsystem.ui.adapter

import android.icu.text.SimpleDateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.alarmsystem.R
import app.alarmsystem.alarms.Alarm
import java.util.Date
import java.util.Locale

class AlarmAdapter(
    private val onAlarmToggle: (Alarm, Boolean) -> Unit,
    private val onAlarmClick: (Alarm) -> Unit
) : ListAdapter<Alarm, AlarmAdapter.AlarmViewHolder>(AlarmDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        holder.bind(getItem(position))
        holder.bindAll(listOf(getItem(position)))
    }

    inner class AlarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        private val labelTextView: TextView = itemView.findViewById(R.id.labelTextView)
        private val daysTextView: TextView = itemView.findViewById(R.id.daysTextView)
        private val toggleSwitch: SwitchCompat = itemView.findViewById(R.id.toggleSwitch)

        fun bindAll(alarms: List<Alarm>) {
            alarms.forEachIndexed { index, alarm ->
                Log.d("TAG12345", "Item $index: $alarm")
                bind(alarm)
            }
        }
        fun bind(alarm: Alarm) {

            timeTextView.text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(alarm.time)) ?: "NA"
            labelTextView.text = alarm.label ?: "NA"

            if (alarm.isRepeating && alarm.days.isNotEmpty()) {
                daysTextView.text = getDaysText(alarm.days)
                daysTextView.visibility = View.VISIBLE
            } else {
                daysTextView.visibility = View.GONE
            }

            toggleSwitch.isChecked = alarm.isEnabled
            toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
                onAlarmToggle(alarm, isChecked)
            }

            itemView.setOnClickListener {
                onAlarmClick(alarm)
            }
        }

        private fun getDaysText(days: List<Int>): String {
            val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            return days.sorted().joinToString(" ") { dayNames[it] }
        }

    }

    class AlarmDiffCallback : DiffUtil.ItemCallback<Alarm>() {
        override fun areItemsTheSame(oldItem: Alarm, newItem: Alarm): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Alarm, newItem: Alarm): Boolean {
            return oldItem == newItem
        }
    }
}