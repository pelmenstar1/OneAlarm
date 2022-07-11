package io.pelmenstar.onealarm.shared.compose

import android.text.format.DateFormat
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import io.pelmenstar.onealarm.shared.android.localeCompat
import io.pelmenstar.onealarm.shared.time.HourFormat
import io.pelmenstar.onealarm.shared.time.MILLIS_IN_MINUTE
import java.util.*

private const val TIME_FORMAT_12 = "hh:mm a"
private const val TIME_FORMAT_24 = "HH:mm"

/**
 * High-level elements which dispays digital text clock.
 * Current time, format, hour format are determined automatically
 * and when one of these values changes, the element is refreshed.
 */
@Composable
fun TextClock(
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val epochMillis by epochMillisObserver(MILLIS_IN_MINUTE)
    val timeZone by defaultTimeZoneObserver()
    val hourFormat by hourFormatObserver()

    val format by remember(context, hourFormat, configuration) {
        val skeleton = if (hourFormat == HourFormat.Format24) {
            TIME_FORMAT_24
        } else {
            TIME_FORMAT_12
        }

        derivedStateOf {
            DateFormat.getBestDateTimePattern(configuration.localeCompat, skeleton)
        }
    }

    val calendar by remember(timeZone) {
        derivedStateOf {
            GregorianCalendar(timeZone)
        }
    }

    val text by remember(calendar, format, epochMillis) {
        derivedStateOf {
            calendar.timeInMillis = epochMillis

            DateFormat.format(format, calendar).toString()
        }
    }

    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        style = style,
    )
}