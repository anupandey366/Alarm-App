package app.alarmsystem.alarms

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverters
import androidx.room.Update
import app.alarmsystem.utils.Converters
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(alarm: Alarm): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(alarm: Alarm)

    @Delete
    suspend fun delete(alarm: Alarm)

    @Query("SELECT * FROM alarms ORDER BY time ASC")
    fun getAllAlarms(): Flow<List<Alarm>>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Int?): Alarm?

    // Add upsert operation
    @Transaction
    suspend fun upsert(alarm: Alarm) {
        if (alarm.id == 0) {
            insert(alarm)
        } else {
            update(alarm)
        }
    }
}

@Database(entities = [Alarm::class], version = 1)
@TypeConverters(Converters::class)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
}

class AlarmRepository(context: Context) {
    private val db = Room.databaseBuilder(context, AlarmDatabase::class.java, "alarms.db").build()

    fun getAllAlarms() = db.alarmDao().getAllAlarms()

    suspend fun insertAlarm(alarm: Alarm) = db.alarmDao().insert(alarm)

    suspend fun updateAlarm(alarm: Alarm) = db.alarmDao().update(alarm)

    suspend fun deleteAlarm(alarm: Alarm) = db.alarmDao().delete(alarm)

    suspend fun getAlarmById(id: Int?) = db.alarmDao().getAlarmById(id)
}
