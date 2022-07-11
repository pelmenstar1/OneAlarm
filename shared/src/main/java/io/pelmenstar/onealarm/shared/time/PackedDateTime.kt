@file:Suppress("NOTHING_TO_INLINE")

package io.pelmenstar.onealarm.shared.time

import androidx.compose.runtime.Stable
import java.util.*

@Stable
fun PackedDateTime(date: PackedDate, time: PackedTime): PackedDateTime {
    return PackedDateTime(PackedDateTime.createBits(date.bits, time.totalSeconds))
}

@Stable
fun PackedDateTime(year: Int, month: Int, dayOfMonth: Int, time: PackedTime): PackedDateTime {
    return PackedDateTime(PackedDate(year, month, dayOfMonth), time)
}

@JvmInline
value class PackedDateTime(@JvmField val bits: Long) {
    val date: PackedDate
        get() = PackedDate(getDate(bits))

    val time: PackedTime
        get() = PackedTime(getTime(bits))

    init {
        require(isValid(bits)) { "bits" }
    }

    inline operator fun component1() = date
    inline operator fun component2() = time

    @Stable
    fun toEpochSecond(): Long {
        return date.toEpochDay() * SECONDS_IN_DAY + time.totalSeconds
    }

    fun zoned(timeZone: TimeZone): PackedDateTime {
        return ofEpochSecond(zonedEpochSeconds(toEpochSecond(), timeZone))
    }

    @Stable
    override fun toString(): String {
        return buildString { this@PackedDateTime.append(this) }
    }

    fun append(sb: StringBuilder) {
        date.append(sb)
        sb.append(' ')
        time.append(sb)
    }

    companion object {
        fun nowEpochSecondsUtc(): Long {
            return System.currentTimeMillis() / 1000
        }

        fun nowEpochSecondsZoned(timeZone: TimeZone = TimeZone.getDefault()): Long {
            return TimeUtils.currentZonedTimeMillis(timeZone) / 1000
        }

        fun zonedEpochSeconds(epochSeconds: Long, zone: TimeZone = TimeZone.getDefault()): Long {
            return epochSeconds + zone.getOffset(epochSeconds * 1000) / 1000
        }

        @Stable
        fun ofEpochSecond(epochSecond: Long): PackedDateTime {
            require(epochSecond >= 0) { "epochSecond" }

            val epochDay = epochSecond / SECONDS_IN_DAY
            val secsOfDay = (epochSecond - epochDay * SECONDS_IN_DAY).toInt()

            return PackedDateTime(PackedDate.ofEpochDay(epochDay), PackedTime(secsOfDay))
        }

        internal fun getDate(bits: Long): Int {
            return (bits shr 32).toInt()
        }

        internal fun getTime(bits: Long): Int {
            return bits.toInt()
        }

        internal fun isValid(bits: Long): Boolean {
            return PackedDate.isValidBits(getDate(bits)) && PackedTime.isValidTotalSeconds(getTime(bits))
        }

        internal fun createBits(date: Int, time: Int): Long {
            return date.toLong() shl 32 or time.toLong()
        }
    }
}