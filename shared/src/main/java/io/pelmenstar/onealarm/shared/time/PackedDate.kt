@file:Suppress("NOTHING_TO_INLINE")

package io.pelmenstar.onealarm.shared.time

import androidx.compose.runtime.Stable
import io.pelmenstar.onealarm.shared.*
import java.util.*

fun PackedDate(year: Int, month: Int, dayOfMonth: Int): PackedDate {
    require(PackedDate.isValid(year, month, dayOfMonth)) { "illegal date" }

    return PackedDate(PackedDate.createBits(year, month, dayOfMonth))
}

@JvmInline
value class PackedDate(@JvmField val bits: Int) {
    @Stable
    val year: Int
        get() = getYear(bits)

    @Stable
    val month: Int
        get() = getMonth(bits)

    @Stable
    val dayOfMonth: Int
        get() = getDayOfMonth(bits)

    @Stable
    val dayOfWeek: Int
        get() {
            val dow0 = floorMod((toEpochDay() + 3), 7).toInt()

            return dow0 + 1
        }

    init {
        require(isValidBits(bits)) { "bits" }
    }

    inline operator fun component1() = year
    inline operator fun component2() = month
    inline operator fun component3() = dayOfMonth

    @Stable
    fun toEpochDay(): Long {
        val year = year
        val month = month
        val day = dayOfMonth

        var total = 0L
        total += 365 * year
        total += (year + 3) / 4 - (year + 99) / 100 + (year + 399) / 400
        total += (367 * month - 362) / 12
        total += day - 1

        if (month > 2) {
            total--
            if (!TimeUtils.isLeapYear(year)) {
                total--
            }
        }

        return total - DAYS_0000_TO_1970
    }

    @Stable
    override fun toString(): String {
        val buffer = CharArray(10)
        writeToCharBuffer(buffer, 0)

        return String(buffer, 0, 10)
    }


    fun writeToCharBuffer(buffer: CharArray, offset: Int) {
        buffer.writeFourDigits(offset, year)
        buffer[offset + 4] = '.'
        buffer.writePaddedTwoDigits(offset + 5, month)
        buffer[offset + 7] = '.'
        buffer.writePaddedTwoDigits(offset + 8, dayOfMonth)
    }

    fun append(sb: StringBuilder) {
        sb.appendPaddedFourDigits(year)
        sb.append('.')
        sb.appendPaddedTwoDigits(month)
        sb.append('.')
        sb.appendPaddedTwoDigits(dayOfMonth)
    }

    companion object {
        const val MAX_YEAR = Short.MAX_VALUE.toInt()
        const val MAX_EPOCH_DAY = 11248738L

        internal fun isValid(year: Int, month: Int, day: Int): Boolean {
            return year in 0..MAX_YEAR &&
                    month in 1..12 &&
                    day in 1..TimeUtils.getDaysInMonth(year, month)
        }

        private fun getYear(bits: Int): Int {
            return bits shr 16
        }

        private fun getMonth(bits: Int): Int {
            return (bits shr 8) and 0xFF
        }

        private fun getDayOfMonth(bits: Int): Int {
            return bits and 0xFF
        }

        internal fun isValidBits(bits: Int): Boolean {
            // date is invalid, if last unused bit is set
            return isValid(getYear(bits), getMonth(bits), getDayOfMonth(bits))
        }

        internal fun createBits(year: Int, month: Int, dayOfMonth: Int): Int {
            return year shl 16 or (month shl 8) or dayOfMonth
        }

        fun todayUtc(): PackedDate {
            return ofEpochDay(todayEpochDayUtc())
        }

        fun todayZoned(zone: TimeZone = TimeZone.getDefault()): PackedDate {
            return ofEpochDay(todayEpochDayZoned(zone))
        }

        fun todayEpochDayUtc(): Long {
            return System.currentTimeMillis() / MILLIS_IN_DAY
        }

        fun todayEpochDayZoned(zone: TimeZone = TimeZone.getDefault()): Long {
            return TimeUtils.currentZonedTimeMillis(zone) / MILLIS_IN_DAY
        }

        @Stable
        fun ofEpochDay(epochDay: Long): PackedDate {
            require(epochDay in 0..MAX_EPOCH_DAY) { "epochDay" }

            var zeroDay = epochDay + DAYS_0000_TO_1970
            zeroDay -= 60

            var yearEst = (400 * zeroDay + 591) / DAYS_PER_CYCLE
            var doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400)

            if (doyEst < 0) {
                yearEst--
                doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400)
            }

            val marchDoy0 = doyEst
            val marchMonth0 = (marchDoy0 * 5 + 2) / 153
            val month = (marchMonth0 + 2) % 12 + 1
            val dom = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1
            yearEst += marchMonth0 / 10

            return PackedDate(yearEst.toInt(), month.toInt(), dom.toInt())
        }
    }
}