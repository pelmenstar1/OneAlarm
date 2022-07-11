package io.pelmenstar.onealarm.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AlarmEntry::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmsDao(): AlarmsDao
}