package app.alarmsystem.alarms

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val time: Long,
    val label: String,
    @ColumnInfo(name = "days_of_week")
    val days: List<Int>,
    val isEnabled: Boolean,
    val isVibrate: Boolean,
    val soundUri: String,
    val snoozeDuration: Int = 5,
    val isRepeating: Boolean = false,
    val difficulty: Int = 0
)
