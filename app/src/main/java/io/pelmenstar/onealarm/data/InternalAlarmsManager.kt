package io.pelmenstar.onealarm.data

class AlarmAlreadyScheduledException: RuntimeException()

interface InternalAlarmsManager {
    fun canScheduleExactAlarms(): Boolean

    fun schedule(id: Long, epochSeconds: Long)
    fun rescheduleForId(id: Long, epochSeconds: Long)
    fun rescheduleAll(alarms: Array<AlarmEntry>)

    fun cancel(id: Long)
}