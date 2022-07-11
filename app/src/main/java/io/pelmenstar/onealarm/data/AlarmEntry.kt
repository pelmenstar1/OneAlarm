package io.pelmenstar.onealarm.data

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_alarms")
@Immutable
class AlarmEntry(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "epoch_seconds") val epochSeconds: Long
)