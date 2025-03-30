package app.alarmsystem.utils

object Constants {
    const val ALARM_ID_EXTRA = "ALARM_ID"
    const val SNOOZE_MINUTES_EXTRA = "SNOOZE_MINUTES"
    const val ALARM_TRIGGER_ACTION = "ACTION_ALARM_TRIGGER"
    const val SNOOZE_ALARM_ACTION = "ACTION_SNOOZE_ALARM"
    const val DISMISS_ALARM_ACTION = "ACTION_DISMISS_ALARM"
    const val DEFAULT_SNOOZE_MINUTES = 5
    const val NOTIFICATION_CHANNEL_ID = "alarm_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Alarm Notifications"
    const val VIBRATE_PATTERN = "1000,2000,1000,2000" // wait 1s, vibrate 2s, wait 1s, vibrate 2s
}