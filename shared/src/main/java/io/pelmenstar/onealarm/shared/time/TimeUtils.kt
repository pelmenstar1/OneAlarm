package io.pelmenstar.onealarm.shared.time

import androidx.compose.runtime.Stable
import java.util.*

/**
 * Helper class that make it easier to work with date and time
 */
object TimeUtils {
    // Pretty strange constant but, nevertheless, it's not randomized
    // If you look at days in month of a year, you may notice that minimum is 28, meanwhile, maximum is 31.
    // Delta is 3, which perfectly fit into 2 bits.
    // Than we can write days in month of a year but represented as delta from 28 (first is february, last is december):
    // 31   28   31   30   31   30   31   31   30   31   30   31
    // \/   \/   \/   \/   \/   \/   \/   \/   \/   \/   \/   \/
    // 3    0    3    2    3    2    3    3    2    3    2    3
    // ------------------------------------------------------------
    // Than if we convert these to binary and take it from end, convert it to hex, we get exactly this constant:
    // 11 00 11 10 11 10 11 11 10 11 10 11 = 0xEEFBB3
    private const val daysInMonthBitTable = 0xEEFBB3
    private val firstDayOfMonth = shortArrayOf(
        1,
        32,
        60,
        91,
        121,
        152,
        182,
        213,
        244,
        274,
        305,
        335
    )

    /**
     * Gets count of days in specified month of specified year
     */
    @Stable
    fun getDaysInMonth(year: Int, month: Int): Int {
        // if year is leap and month is february
        return if (isLeapYear(year) && month == 2) {
            29
        } else {
            getDaysInMonthNoLeap(month)
        }
    }

    private fun getDaysInMonthNoLeap(month: Int): Int {
        // The nature of daysInMonthBitTable is described above.
        // There are described why expression below looks exactly like that.
        // month - 1: month is in range [1;12], but we need [0;11]
        // (month - 1) << 1: every offset is described in 2 bits, so we need to double our shift
        // (daysInMonthBitTable) >> ((month - 1) << 1): takes day offset of specified month
        // 28 + ((daysInMonthBitTable >> ((month - 1) << 1)) & 0x3): because of primary offset is 28,
        // we need to add it back.
        return 28 + (daysInMonthBitTable shr (month - 1 shl 1) and 0x3)
    }

    @Stable
    fun getDaysInPrevMonth(year: Int, month: Int): Int {
        return if (month == 1) {
            getDaysInMonthNoLeap(12)
        } else {
            getDaysInMonth(year, month - 1)
        }
    }

    /**
     * Returns day of year that represents first day of specified month of specified year
     */
    @Stable
    fun getFirstDayOfMonth(year: Int, month: Int): Int {
        val firstDay = firstDayOfMonth[month - 1].toInt()

        return if (isLeapYear(year) && month > 2) {
            firstDay + 1
        } else {
            firstDay
        }
    }

    /**
     * Determines whether specified year is leap.
     * Leap year is a year that divides by 4 and 400, but not 100
     * (1900 wasn't leap, but 2000 did, because it divides by 400)
     */
    @Stable
    fun isLeapYear(year: Int): Boolean {
        return year and 3 == 0 && (year % 100 != 0 || year % 400 == 0)
    }

    fun currentZonedTimeMillis(timeZone: TimeZone = TimeZone.getDefault()): Long {
        val millis = System.currentTimeMillis()
        val offset = timeZone.getOffset(millis)

        return millis + offset
    }
}