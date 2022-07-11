package io.pelmenstar.onealarm.shared.time

import android.content.Context
import android.text.format.DateFormat
import io.pelmenstar.onealarm.shared.PI_F

enum class HourFormat(@JvmField val maxHours: Int) {
    Format12(12),
    Format24(24);

    @JvmField
    val maxMinutes = maxHours * 60

    @JvmField
    val invMaxMinutes = 1f / maxMinutes

    @JvmField
    val minutesToAngle = (2 * PI_F) / maxMinutes

    @JvmField
    val angleToMinutes = maxMinutes / (2 * PI_F)
}

fun Context.getHourFormat(): HourFormat {
    return if (DateFormat.is24HourFormat(this)) HourFormat.Format24 else HourFormat.Format12
}