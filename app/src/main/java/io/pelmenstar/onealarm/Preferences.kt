package io.pelmenstar.onealarm

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.pelmenstar.onealarm.shared.appendPaddedTwoDigits
import io.pelmenstar.onealarm.shared.time.PackedHourMinute
import io.pelmenstar.onealarm.shared.toPositiveInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

@Suppress("UNCHECKED_CAST")
object AppPreferences {
    abstract class Entry<T> {
        abstract val defaultValue: T

        fun toFlow(dataStore: DataStore<Preferences>): Flow<T> {
            return dataStore.data.map { getFrom(it) }
        }

        abstract fun getFrom(prefs: Preferences): T
        abstract fun setValue(prefs: MutablePreferences, value: T)

        inline fun <O> twoWayMap(
            crossinline forward: (T) -> O,
            crossinline backward: (O) -> T
        ): Entry<O> {
            return object : Entry<O>() {
                override val defaultValue: O = forward(this@Entry.defaultValue)

                override fun getFrom(prefs: Preferences): O = forward(this@Entry.getFrom(prefs))

                override fun setValue(prefs: MutablePreferences, value: O) {
                    this@Entry.setValue(prefs, backward(value))
                }
            }
        }

        internal class Impl<T>(name: String, override val defaultValue: T) : Entry<T>() {
            private val key = intPreferencesKey(name) as Preferences.Key<T>

            override fun getFrom(prefs: Preferences) = prefs[key] ?: defaultValue

            override fun setValue(prefs: MutablePreferences, value: T) {
                prefs[key] = value
            }
        }
    }

    private const val DEFAULT_MOST_USED_ALARMS_RAW = "001500300100"

    val snoozeDuration = entry("snooze_duration", 10)
    val silenceAfter = entry("silence_after", 10)
    val volumeButtonBehaviour = entry("volume_button_behaviour", VolumeButtonBehaviour.NONE)
    val alarmsDeletionReason = entry("alarms_deletion_reason", AlarmsDeletionReason.NONE)
    val exactAlarmsNeverShowAgain = entry("exact_alarms_never_shown_again", false)
    val mostUsedAlarms = entry("most_used_alarms", DEFAULT_MOST_USED_ALARMS_RAW).twoWayMap(
        forward = { raw ->
            val capacity = raw.length / 4

            Array(capacity) { i ->
                val offset = i * 4

                val hour = raw.toPositiveInt(offset, offset + 2)
                val minute = raw.toPositiveInt(offset + 2, offset + 4)

                if ((hour or minute) < 0) {
                    throw IllegalStateException("Illegal app state")
                }

                PackedHourMinute(hour, minute)
            }
        },
        backward = { array ->
            buildString(array.size * 4) {
                array.forEach { hm ->
                    appendPaddedTwoDigits(hm.hour)
                    appendPaddedTwoDigits(hm.minute)
                }
            }
        }
    )

    private fun <T> entry(name: String, defaultValue: T): Entry<T> {
        return Entry.Impl(name, defaultValue)
    }
}

suspend inline fun <T> AppPreferences.Entry<T>.setTo(prefs: DataStore<Preferences>, value: T) {
    prefs.updateData {
        val mutable = it.toMutablePreferences()
        setValue(mutable, value)

        mutable
    }
}

suspend inline fun <T> AppPreferences.Entry<T>.getFrom(prefs: DataStore<Preferences>): T {
    return toFlow(prefs).firstOrNull() ?: defaultValue
}

val Context.appPreferences by preferencesDataStore("preferences")