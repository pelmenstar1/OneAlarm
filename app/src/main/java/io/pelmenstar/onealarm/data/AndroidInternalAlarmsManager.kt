package io.pelmenstar.onealarm.data

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import io.pelmenstar.onealarm.AlarmService

class AndroidInternalAlarmsManager(
    private val context: Context
) : InternalAlarmsManager {
    private val alarmService = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun canScheduleExactAlarms(): Boolean {
        return if(Build.VERSION.SDK_INT >= 31) {
            alarmService.canScheduleExactAlarms()
        } else {
            true
        }
    }

    override fun schedule(id: Long, epochSeconds: Long) {
        createPendingIntent(id)?.also {
            scheduleInternal(it, epochSeconds)
        }
    }

    override fun rescheduleForId(id: Long, epochSeconds: Long) {
        cancel(id)
        schedule(id, epochSeconds)
    }

    @SuppressLint("MissingPermission")
    private fun scheduleInternal(intent: PendingIntent, epochSeconds: Long) {
        val epochMillis = epochSeconds * 1000

        if (Build.VERSION.SDK_INT < 31 || alarmService.canScheduleExactAlarms()) {
            alarmService.setAlarmClock(
                AlarmManager.AlarmClockInfo(epochMillis, intent),
                intent
            )
        } else {
            alarmService.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epochMillis, intent)
        }
    }

    override fun rescheduleAll(alarms: Array<AlarmEntry>) {
        for (entry in alarms) {
            rescheduleForId(entry.id, entry.epochSeconds)
        }
    }

    override fun cancel(id: Long) {
        createPendingIntent(id).also {
            alarmService.cancel(it)
        }
    }

    private fun createPendingIntent(id: Long, primaryFlags: Int = 0): PendingIntent? {
        val intent = AlarmService.createIntent(context, id, AlarmService.ACTION_START)

        var flags = primaryFlags
        if (Build.VERSION.SDK_INT >= 31) {
            flags = flags or PendingIntent.FLAG_MUTABLE
        }



        return PendingIntent.getService(context, id.toInt(), intent, flags)
    }
}