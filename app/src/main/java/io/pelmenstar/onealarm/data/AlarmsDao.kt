package io.pelmenstar.onealarm.data

import androidx.room.Dao
import androidx.room.Query

@Dao
abstract class AlarmsDao {
    @Query("INSERT INTO pending_alarms (epoch_seconds) VALUES(:epochSeconds)")
    abstract suspend fun addAlarm(epochSeconds: Long): Long

    @Query("DELETE FROM pending_alarms WHERE id=:id")
    abstract suspend fun deleteAlarmById(id: Long)

    @Query("UPDATE pending_alarms SET epoch_seconds=:newEpochSeconds WHERE id=:id")
    abstract suspend fun updateAlarm(id: Long, newEpochSeconds: Long)

    @Query("SELECT * FROM pending_alarms")
    abstract suspend fun getAlarms(): Array<AlarmEntry>

    @Query("SELECT * FROM pending_alarms WHERE id=:id LIMIT 1")
    abstract suspend fun getAlarmById(id: Long): AlarmEntry?

    @Query("DELETE FROM pending_alarms WHERE epoch_seconds < :epochSeconds")
    abstract suspend fun deleteLessThan(epochSeconds: Long): Int

    @Query("SELECT id FROM pending_alarms WHERE epoch_seconds < :epochSeconds")
    abstract suspend fun getAlarmsIdsLessThan(epochSeconds: Long): LongArray
}