package io.pelmenstar.onealarm.shared.android

import android.content.Intent
import android.os.Build
import android.os.Bundle
import java.util.*

/**
 * Retrieves [TimeZone] value from extras in broadcast receiver registered to timezone changes.
 */
fun Bundle?.getTimeZoneOnChanged(): TimeZone {
    var timeZone: TimeZone? = null

    if(this != null && Build.VERSION.SDK_INT >= 31) {
        val id = getString(Intent.EXTRA_TIMEZONE)

        timeZone = id?.let(TimeZone::getTimeZone)
    }

    return timeZone ?: TimeZone.getDefault()
}