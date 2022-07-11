package io.pelmenstar.onealarm.shared.time

import android.content.Context
import android.content.res.Resources
import android.icu.text.RelativeDateTimeFormatter
import android.os.Build
import android.text.format.DateFormat
import androidx.annotation.PluralsRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import io.pelmenstar.onealarm.shared.*
import io.pelmenstar.onealarm.shared.android.localeCompat
import java.text.FieldPosition
import java.text.SimpleDateFormat
import java.util.*

/**
 * Responsible for formatting dates to a pretty view.
 * The class is also able to format dates as relative (emit today, tommorrow).
 */
sealed class PrettyDateFormatter(protected val context: Context) {
    protected val resources: Resources = context.resources

    protected val tempDate = Date()

    @Volatile
    private var lessThanMinuteStringRaw: String? = null

    /**
     * Returns string representation of given [dateTime].
     *
     * @param dateTime datetime to format, should be in UTC
     * @param hourFormat hour format of time
     * @param timeZone timezone which should be used in formatting the datetime.
     */
    @Stable
    abstract fun prettyFormat(
        dateTime: PackedDateTime,
        hourFormat: HourFormat,
        timeZone: TimeZone
    ): String

    private fun StringBuilder.appendProp(value: Int, @PluralsRes pluralRes: Int) {
        if (value > 0) {
            append(value)
            append(' ')
            append(resources.getQuantityString(pluralRes, value))
            append(' ')
        }
    }

    fun formatSecondsDifference(totalSeconds: Long): String {
        if (totalSeconds < 60) {
            return resources.getCachedString(
                R.string.less_than_minute,
                lessThanMinuteStringRaw
            ) { lessThanMinuteStringRaw = it }
        }

        return buildString(32) {
            var tempSeconds = totalSeconds

            val weeks = getTimeProperty(tempSeconds, SECONDS_IN_WEEK) { tempSeconds = it }
            val days = getTimeProperty(tempSeconds, SECONDS_IN_DAY) { tempSeconds = it }
            val hours = getTimeProperty(tempSeconds, SECONDS_IN_HOUR) { tempSeconds = it }
            var minutes = (tempSeconds / 60).toInt()

            // If second value is greater than 30, then round up to the next minute.
            if (tempSeconds % 60 >= 30) {
                minutes++
            }

            appendProp(weeks, R.plurals.week)
            appendProp(days, R.plurals.day)
            appendProp(hours, R.plurals.hour)
            appendProp(minutes, R.plurals.minute)

            val length = length

            // Delete last space.
            if (length > 0) {
                deleteCharAt(length - 1)
            }
        }
    }

    protected inline fun useTemporaryDate(epochDay: Long, block: (Date) -> Unit) {
        synchronized(tempDate) {
            tempDate.time = epochDay * MILLIS_IN_DAY
            block(tempDate)
        }
    }

    private class Impl21(context: Context) : PrettyDateFormatter(context) {
        private val dateFormatter: SimpleDateFormat

        @Volatile
        private var todayStringRaw: String? = null

        @Volatile
        private var yesterdayStringRaw: String? = null

        @Volatile
        private var tomorrowStringRaw: String? = null

        private val todayString: String
            get() = resources.getCachedString(R.string.today, todayStringRaw) {
                todayStringRaw = it
            }

        private val yesterdayString: String
            get() = resources.getCachedString(R.string.yesterday, yesterdayStringRaw) {
                yesterdayStringRaw = it
            }

        private val tomorrowString: String
            get() = resources.getCachedString(R.string.tomorrow, tomorrowStringRaw) {
                tomorrowStringRaw = it
            }

        init {
            val locale = resources.configuration.localeCompat

            dateFormatter = SimpleDateFormat(
                DateFormat.getBestDateTimePattern(locale, DATE_FORMAT_PATTERN),
                locale
            )
        }

        override fun prettyFormat(
            dateTime: PackedDateTime,
            hourFormat: HourFormat,
            timeZone: TimeZone
        ): String {
            return buildStringWithStringBuffer(32) {
                val zonedDateTime = dateTime.zoned(timeZone)
                val dateEpochDay = zonedDateTime.date.toEpochDay()
                val todayEpochDay = PackedDate.todayEpochDayZoned(timeZone)

                val diff = dateEpochDay - todayEpochDay

                if (diff in -1L..1L) {
                    append(
                        when (diff) {
                            1L -> tomorrowString
                            0L -> todayString
                            -1L -> yesterdayString
                            else -> throw RuntimeException("Impossible")
                        }
                    )
                } else {
                    useTemporaryDate(dateEpochDay) {
                        dateFormatter.format(it, this, DEFAULT_FIELD_POSITION)
                    }
                }

                append(' ')

                val (hour, minute) = dateTime.time

                if (hourFormat == HourFormat.Format24) {
                    appendPaddedTwoDigits(hour)
                    append(':')
                    appendPaddedTwoDigits(minute)
                } else {
                    if (hour > 12) {
                        appendPaddedTwoDigits(hour - 12)
                    } else {
                        appendPaddedTwoDigits(hour)
                    }

                    append(':')
                    appendPaddedTwoDigits(minute)

                    if (hour < 12) {
                        append(" AM")
                    } else {
                        append(" PM")
                    }
                }
            }
        }
    }

    @RequiresApi(24)
    private class Impl24(context: Context): PrettyDateFormatter(context) {
        private val locale: Locale = resources.configuration.localeCompat
        private val relativeDateTimeFormatter = RelativeDateTimeFormatter.getInstance(locale)
        private val dateFormatter = android.icu.text.SimpleDateFormat(
            DateFormat.getBestDateTimePattern(locale, DATE_FORMAT_PATTERN),
            locale
        )

        @Volatile
        private var todayStringRaw: String? = null

        @Volatile
        private var yesterdayStringRaw: String? = null

        @Volatile
        private var tomorrowStringRaw: String? = null

        private val todayString: String
            get() = resources.getCachedString(R.string.today, todayStringRaw) {
                todayStringRaw = it
            }

        private val yesterdayString: String
            get() = resources.getCachedString(R.string.yesterday, yesterdayStringRaw) {
                yesterdayStringRaw = it
            }

        private val tomorrowString: String
            get() = resources.getCachedString(R.string.tomorrow, tomorrowStringRaw) {
                tomorrowStringRaw = it
            }

        override fun prettyFormat(
            dateTime: PackedDateTime,
            hourFormat: HourFormat,
            timeZone: TimeZone
        ): String {
            val (date, time) = dateTime.zoned(timeZone)

            val dateStr = buildStringWithStringBuffer(32) {
                val dateEpochDay = date.toEpochDay()
                val todayEpochDay = PackedDate.todayEpochDayZoned(timeZone)

                val diff = dateEpochDay - todayEpochDay

                if (diff in -1L..1L) {
                    val result = when (diff) {
                        1L -> tomorrowString
                        0L -> todayString
                        -1L -> yesterdayString
                        else -> throw RuntimeException("Impossible")
                    }

                    append(result)
                } else {
                    useTemporaryDate(dateEpochDay) {
                        dateFormatter.format(it, this, DEFAULT_FIELD_POSITION)
                    }
                }
            }

            val timeStr = prettyFormatTime(time, hourFormat)

            return relativeDateTimeFormatter.combineDateAndTime(dateStr, timeStr)
        }
    }

    @RequiresApi(28)
    private class Impl28(context: Context) : PrettyDateFormatter(context) {
        private val locale: Locale = resources.configuration.localeCompat
        private val relativeDateTimeFormatter = RelativeDateTimeFormatter.getInstance(locale)
        private val dateFormatter = android.icu.text.SimpleDateFormat(
            DateFormat.getBestDateTimePattern(locale, DATE_FORMAT_PATTERN),
            locale
        )

        @Volatile
        private var yesterdayStringRaw: String? = null

        @Volatile
        private var todayStringRaw: String? = null

        @Volatile
        private var tomorrowStringRaw: String? = null

        private val yesterdayString: String
            get() = getCachedRelativeString(
                yesterdayStringRaw,
                -1.0,
            ) { yesterdayStringRaw = it }

        private val todayString: String
            get() = getCachedRelativeString(
                todayStringRaw,
                0.0,
            ) { todayStringRaw = it }

        private val tomorrowString: String
            get() = getCachedRelativeString(
                tomorrowStringRaw,
                1.0,
            ) { tomorrowStringRaw = it }

        private inline fun getCachedRelativeString(
            currentValue: String?,
            offset: Double,
            set: (String) -> Unit
        ) = getLazyValue(
            currentValue,
            create = {
                relativeDateTimeFormatter
                    .format(offset, RelativeDateTimeFormatter.RelativeDateTimeUnit.DAY)
                    .replaceFirstChar { it.titlecase(locale) }
            },
            set
        )

        override fun prettyFormat(
            dateTime: PackedDateTime,
            hourFormat: HourFormat,
            timeZone: TimeZone
        ): String {
            val (date, time) = dateTime.zoned(timeZone)
            val todayEpochDay = PackedDate.todayEpochDayZoned(timeZone)

            val dateStr = buildStringWithStringBuffer(32) {
                val dateEpochDay = date.toEpochDay()
                val diff = dateEpochDay - todayEpochDay

                if (diff in -1L..1L) {
                    val result = when(diff) {
                        1L -> tomorrowString
                        0L -> todayString
                        -1L -> yesterdayString
                        else -> throw RuntimeException("Impossible")
                    }

                    append(result)
                } else {
                    useTemporaryDate(dateEpochDay) {
                        dateFormatter.format(it, this, DEFAULT_FIELD_POSITION)
                    }
                }
            }

            val timeStr = prettyFormatTime(time, hourFormat)

            return relativeDateTimeFormatter.combineDateAndTime(dateStr, timeStr)
        }
    }

    companion object {
        private const val DATE_FORMAT_PATTERN = "dd MMMM yyyy"
        private val DEFAULT_FIELD_POSITION = FieldPosition(0)

        fun create(context: Context): PrettyDateFormatter {
            return when {
                Build.VERSION.SDK_INT >= 28 -> Impl28(context)
                Build.VERSION.SDK_INT >= 24 -> Impl24(context)
                else -> Impl21(context)
            }
        }

        private inline fun Resources.getCachedString(
            @StringRes id: Int,
            value: String?,
            setValue: (String) -> Unit
        ) = getLazyValue(value, { getString(id) }, setValue)

        private inline fun getTimeProperty(
            currentSeconds: Long,
            timeUnit: Int,
            setCurrentSeconds: (Long) -> Unit
        ): Int {
            val timeProp = currentSeconds / timeUnit

            setCurrentSeconds(currentSeconds - timeProp * timeUnit)

            return timeProp.toInt()
        }

        private fun prettyFormatTime(time: PackedTime, hourFormat: HourFormat): String {
            val (hour, minute) = time
            val buffer: CharArray

            if (hourFormat == HourFormat.Format24) {
                buffer = CharArray(5).also {
                    it.writePaddedTwoDigits(0, hour)
                    it[2] = ':'
                    it.writePaddedTwoDigits(3, minute)
                }
            } else {
                buffer = CharArray(8).also {
                    if (hour > 12) {
                        it.writePaddedTwoDigits(0, hour - 12)
                    } else {
                        it.writePaddedTwoDigits(0, hour)
                    }

                    it[2] = ':'
                    it.writePaddedTwoDigits(3, minute)
                    it[5] = ' '
                    it[6] = if (hour < 12) 'A' else 'P'
                    it[7] = 'M'
                }
            }

            return String(buffer)
        }
    }
}