@file:Suppress("NOTHING_TO_INLINE")

package io.pelmenstar.onealarm.shared.compose

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import io.pelmenstar.onealarm.shared.android.getTimeZoneOnChanged
import io.pelmenstar.onealarm.shared.time.HourFormat
import io.pelmenstar.onealarm.shared.time.PackedDate
import io.pelmenstar.onealarm.shared.time.getHourFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.*

/**
 * Returns a difference between origin [value] and [value] which aligned-up which given alignment.
 */
@Stable
private fun differenceBetweenAlignedUp(value: Long, alignment: Long): Long {
    val aligned = (value / alignment + 1) * alignment

    return aligned - value
}

private suspend inline fun adjustForInterval(now: Long, intervalMillis: Long) {
    val adjustment = differenceBetweenAlignedUp(now, intervalMillis)

    delay(adjustment)
}

/**
 * Returns a state which holds current epoch seconds (timezone is UTC)
 * and the state is changed with given interval or because of other reasons that change current epoch seconds.
 */
@Composable
inline fun epochSecondsUtcObserver(intervalSeconds: Int) =
    epochSecondsUtcObserver(intervalSeconds.toLong())

/**
 * Returns a state which holds current epoch seconds (timezone is UTC)
 * and the state is changed with given interval or because of other reasons that change current epoch seconds.
 */
@Composable
fun epochSecondsUtcObserver(intervalSeconds: Long): State<Long> {
    val millis by epochMillisObserver(intervalSeconds * 1000)

    return remember(millis) {
        derivedStateOf {
            millis / 1000
        }
    }
}

/**
 * Returns a state which holds current epoch millis (timezone is UTC)
 * and the state is changed with given interval or because of other reasons that change current epoch millis.
 */
@Composable
inline fun epochMillisObserver(intervalMillis: Int) =
    epochMillisObserver(intervalMillis.toLong())

/**
 * Returns a state which holds current epoch millis (timezone is UTC)
 * and the state is changed with given interval or because of other reasons that change current epoch millis.
 */
@Composable
fun epochMillisObserver(intervalMillis: Long): State<Long> {
    val context = LocalContext.current

    val state = remember { mutableStateOf(System.currentTimeMillis()) }

    BroadcastReceiver(intentFilter(
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
        Intent.ACTION_DATE_CHANGED
    )) { action, _ ->
        when(action) {
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_DATE_CHANGED -> {
                state.value = System.currentTimeMillis()
            }
        }
    }

    LaunchedEffect(context, intervalMillis) {
        adjustForInterval(state.value, intervalMillis)

        while (isActive) {
            val now = System.currentTimeMillis()

            state.value = now

            adjustForInterval(now, intervalMillis)
        }
    }

    return state
}

/**
 * Returns a state which holds current epoch day (timezone is UTC)
 * and the state is changed each time system's current date changes.
 */
@Composable
fun epochDayObserver(): State<Long> {
    val state = remember {
        mutableStateOf(PackedDate.todayEpochDayUtc())
    }

    BroadcastReceiver(intentFilter(Intent.ACTION_DATE_CHANGED)) { _, _ ->
        state.value = PackedDate.todayEpochDayUtc()
    }

    return state
}

/**
 * Returns a state which holds default system timezone
 * and the state is changed when system timezone is changed.
 */
@Composable
fun defaultTimeZoneObserver(): State<TimeZone> {
    val state = remember { mutableStateOf(TimeZone.getDefault()) }

    BroadcastReceiver(intentFilter(Intent.ACTION_TIMEZONE_CHANGED)) { _, extras ->
        state.value = extras.getTimeZoneOnChanged()
    }

    return state
}

/**
 * Returns a state which holds default hour format
 * and the state is changed when the format is changed.
 */
@Composable
fun hourFormatObserver(): State<HourFormat> {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val state = remember(context, configuration) {
        mutableStateOf(context.getHourFormat())
    }

    ContentObserver(settingsUri(Settings.System.TIME_12_24)) {
        state.value = context.getHourFormat()
    }

    return state
}

/**
 * Returns a state which holds a value that determines whether it's allowed for the application to schedule exact alarms.
 * and the state is changed when the such value is changed.
 *
 * As a possibility to disallow scheduling exact alarms for particular application was intoduced in API level 31,
 * it requires at least this API level.
 */
@Composable
@RequiresApi(31)
fun canSheduleExactAlarmsObserver(): State<Boolean> {
    val context = LocalContext.current

    val alarmService = remember(context) {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    val state = remember {
        mutableStateOf(alarmService.canScheduleExactAlarms())
    }

    BroadcastReceiver(
        intentFilter(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
    ) { _, _ ->
        state.value = alarmService.canScheduleExactAlarms()
    }

    return state
}