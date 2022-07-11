package io.pelmenstar.onealarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.AndroidEntryPoint
import io.pelmenstar.onealarm.data.AppDatabase
import io.pelmenstar.onealarm.data.InternalAlarmsManager
import io.pelmenstar.onealarm.shared.time.PackedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

// Receives intent, when alarm permission state was changed or on boot completed,
// in order to maintain state of alarms consistent (reschedule them all)
@AndroidEntryPoint
class AlarmUpdateReceiver : BroadcastReceiver() {
    @Inject
    lateinit var appDatabase: AppDatabase

    @Inject
    lateinit var alarmsManager: InternalAlarmsManager

    @Inject
    lateinit var preferences: DataStore<Preferences>

    override fun onReceive(context: Context, intent: Intent) {
        val alarmsDao = appDatabase.alarmsDao()
        val action = intent.action

        val result = goAsync()

        scope.launch {
            var deletedAlarmsIds = LongArray(0)
            if (action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
                val threshold = PackedDateTime.nowEpochSecondsZoned()

                deletedAlarmsIds = alarmsDao.getAlarmsIdsLessThan(threshold)
                val deletedCount = alarmsDao.deleteLessThan(threshold)

                if (deletedCount > 0) {
                    val reason = when (action) {
                        Intent.ACTION_BOOT_COMPLETED -> AlarmsDeletionReason.DEVICE_OFF
                        else -> 0
                    }

                    preferences.edit {
                        val prevReason = AppPreferences.alarmsDeletionReason.getFrom(it)

                        AppPreferences.alarmsDeletionReason.setValue(it, prevReason or reason)
                    }
                }
            }

            val alarms = alarmsDao.getAlarms()

            for(id in deletedAlarmsIds) {
                alarmsManager.cancel(id)
            }

            alarmsManager.rescheduleAll(alarms)

            result.finish()
        }
    }

    companion object {
        private val scope = CoroutineScope(Dispatchers.IO)
    }
}