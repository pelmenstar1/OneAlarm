package io.pelmenstar.onealarm

import android.util.Log
import io.pelmenstar.onealarm.data.AlarmsDao
import io.pelmenstar.onealarm.data.InternalAlarmsManager
import io.pelmenstar.onealarm.shared.time.PackedDateTime
import io.pelmenstar.onealarm.shared.time.SECONDS_IN_DAY
import java.util.*

object AlarmHelper {
    private const val TAG = "AlarmHelper"

    /**
     *
     */
    fun getEpochSeconds(minutesOfDay: Int, mode: AlarmMode): Long {
        val nowEpochSecondsUtc = PackedDateTime.nowEpochSecondsUtc()

        return if (mode == AlarmMode.EXACT_AT) {
            val zoneOffsetSeconds = TimeZone.getDefault().getOffset(nowEpochSecondsUtc * 1000) / 1000

            val zonedEpochSeconds = nowEpochSecondsUtc + zoneOffsetSeconds

            val nowEpochDay = zonedEpochSeconds / SECONDS_IN_DAY
            val nowSecsOfDay = (zonedEpochSeconds - nowEpochDay * SECONDS_IN_DAY).toInt()
            val secsOfDay = minutesOfDay * 60

            val zonedResult = if (nowSecsOfDay >= secsOfDay) {
                (nowEpochDay + 1) * SECONDS_IN_DAY + secsOfDay
            } else {
                nowEpochDay * SECONDS_IN_DAY + secsOfDay
            }

            zonedResult - zoneOffsetSeconds
        } else {
            nowEpochSecondsUtc + minutesOfDay * 60
        }
    }

    suspend fun scheduleAndSave(
        alarmsDao: AlarmsDao,
        alarmsManager: InternalAlarmsManager,
        minutesOfDay: Int, mode: AlarmMode
    ) {
        val epochSeconds = getEpochSeconds(minutesOfDay, mode)
        val id = alarmsDao.addAlarm(epochSeconds)

        Log.i(TAG, "Alarm with id $id added to dao, sheduling in manager. Exact date: ${PackedDateTime.ofEpochSecond(epochSeconds)}. Epoch millis UTC: ${epochSeconds * 1000}")

        try {
            alarmsManager.schedule(id, epochSeconds)

            GlobalAlarmStateManager.notify(id, AlarmState.SCHEDULED)
            Log.i(TAG, "Alarm with id $id sucessfully scheduled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm with id $id, backing up")

            try {
                alarmsDao.deleteAlarmById(id)
            } catch (e1: Exception) {
                // eat exception
            }

            throw e
        }
    }

    suspend fun cancelAndDelete(
        alarmsDao: AlarmsDao,
        alarmsManager: InternalAlarmsManager,
        id: Long
    ) {
        alarmsManager.cancel(id)

        alarmsDao.deleteAlarmById(id)
    }

    suspend fun rescheduleForFromNow(
        alarmsDao: AlarmsDao,
        alarmsManager: InternalAlarmsManager,
        id: Long,
        durationMinutes: Int
    ) {
        val newEpochSeconds = getEpochSeconds(durationMinutes, AlarmMode.FROM_NOW)

        try {
            Log.i(TAG, "Re-scheduling alarm with id $id")

            alarmsManager.rescheduleForId(id, newEpochSeconds)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to re-schedule alarm", e)

            throw e
        }

        alarmsDao.updateAlarm(id, newEpochSeconds)
    }
}