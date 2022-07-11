package io.pelmenstar.onealarm.shared.compose

import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import io.pelmenstar.onealarm.shared.time.HourFormat
import io.pelmenstar.onealarm.shared.time.PackedDateTime
import io.pelmenstar.onealarm.shared.time.PrettyDateFormatter
import java.util.*

/**
 * High-level element which displays date-time text.
 *
 * @param modifier a [Modifier] to apply to this node
 * @param dateTime a [PackedDateTime] instance to display (in UTC)
 * @param hourFormat specifies either to display text in  12-hour format or 24-hour one
 * @param todayEpochDay epoch day of today (in [timeZone] zone!).
 * It's not actually used, this is rather an indicator that today date is changed and text should be refreshed
 * @param timeZone time zone which should be used in the formatting.
 * @param formatter a [PrettyDateFormatter] with which the text should be formatted
 * @param style text style
 * @param color color of text
 * @param fontSize text size
 */
@Composable
fun DateTimeText(
    modifier: Modifier = Modifier,
    dateTime: PackedDateTime,
    hourFormat: HourFormat,
    todayEpochDay: Long,
    timeZone: TimeZone,
    formatter: PrettyDateFormatter,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified
) {
    val text by remember(dateTime, hourFormat, todayEpochDay, timeZone) {
        derivedStateOf {
            formatter.prettyFormat(dateTime, hourFormat, timeZone)
        }
    }

    Text(
        modifier = modifier,
        style = style,
        color = color,
        fontSize = fontSize,
        text = text
    )
}

/**
 * High-level element which displays date-time text.
 *
 * @param modifier a [Modifier] to apply to this node
 * @param dateTime a [PackedDateTime] instance to display
 * @param formatter a [PrettyDateFormatter] with which the text should be formatted
 * @param style text style
 * @param color color of text
 * @param fontSize text size
 */
@Composable
fun DateTimeText(
    modifier: Modifier = Modifier,
    dateTime: PackedDateTime,
    formatter: PrettyDateFormatter,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified
) {
    val hourFormat by hourFormatObserver()
    val todayEpochDay by epochDayObserver()
    val timeZone by defaultTimeZoneObserver()

    DateTimeText(modifier, dateTime, hourFormat, todayEpochDay, timeZone, formatter, style, color, fontSize)
}