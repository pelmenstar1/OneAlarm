@file:Suppress("NOTHING_TO_INLINE")

package io.pelmenstar.onealarm.shared.time

import io.pelmenstar.onealarm.shared.isValidPositiveInt
import io.pelmenstar.onealarm.shared.toPositiveInt
import io.pelmenstar.onealarm.shared.writePaddedTwoDigits

fun PackedHourMinute(hour: Int, minute: Int): PackedHourMinute {
    require(hour in 0..23) { "hour is out of 0..23 range" }
    require(minute in 0..59) { "minute is out of 0..59 range" }

    return PackedHourMinute(hour * 60 + minute)
}

/**
 * Represents tuple of hour and minute
 */
@JvmInline
value class PackedHourMinute(@JvmField val totalMinutes: Int) {
    val hour: Int
        get() = totalMinutes / 60

    val minute: Int
        get() = totalMinutes % 60

    inline operator fun component1() = hour
    inline operator fun component2() = minute

    override fun toString(): String {
        val buffer = CharArray(5)

        buffer.writePaddedTwoDigits(0, hour)
        buffer[2] = ':'
        buffer.writePaddedTwoDigits(3, minute)

        return String(buffer)
    }

    companion object {
        fun isValid(text: String): Boolean {
            return text.length == 5 &&
                    text[2] == ':' &&
                    text.isValidPositiveInt(0, 2) &&
                    text.isValidPositiveInt(3, 5)
        }

        /**
         * Parses text formatted as `hh:mm` to [PackedHourMinute] or returns null when the text's format is invalid.
         */
        fun parseOrNull(text: String): PackedHourMinute? {
            if (text[2] != ':') {
                return null
            }

            val hour = text.toPositiveInt(0, 2)
            val minute = text.toPositiveInt(3, 5)

            if ((hour or minute) < 0) {
                return null
            }

            return PackedHourMinute(hour, minute)
        }

        fun parseToTotalMinutesOrMinusOne(text: String): Int {
            if (text[2] != ':') {
                return -1
            }

            val hour = text.toPositiveInt(0, 2)
            val minute = text.toPositiveInt(3, 5)

            if ((hour or minute) < 0) {
                return -1
            }

            return hour * 60 + minute
        }
    }
}