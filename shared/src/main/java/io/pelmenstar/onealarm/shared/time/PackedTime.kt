@file:Suppress("NOTHING_TO_INLINE")

package io.pelmenstar.onealarm.shared.time

import androidx.compose.runtime.Stable
import io.pelmenstar.onealarm.shared.appendPaddedTwoDigits
import io.pelmenstar.onealarm.shared.toPositiveInt
import io.pelmenstar.onealarm.shared.writePaddedTwoDigits

@Stable
fun PackedTime(hour: Int, minute: Int, second: Int): PackedTime {
    require(hour in 0..23) { "hour" }
    require(minute in 0..59) { "minute" }
    require(second in 0..59) { "second" }

    return PackedTime(hour * 3600 + minute * 60 + second)
}

/**
 * Responsible for manipulating seconds of day
 */
@JvmInline
value class PackedTime internal constructor(@JvmField val totalSeconds: Int) {
    val hour: Int
        get() = totalSeconds / 3600

    val minute: Int
        get() = (totalSeconds % 3600) / 60

    val second: Int
        get() = totalSeconds % 60

    init {
        if(!isValidTotalSeconds(totalSeconds)) {
            throw IllegalArgumentException("totalSeconds")
        }
    }

    inline operator fun component1() = hour
    inline operator fun component2() = minute
    inline operator fun component3() = second

    /**
     * Returns string representation of time-int in format 'HH:MM:SS'
     *
     * @throws IllegalArgumentException if time isn't invalid
     */
    @Stable
    override fun toString(): String {
        val buffer = CharArray(8)
        writeToCharBuffer(buffer, 0)

        return String(buffer)
    }

    /**
     * Writes string representation of time to char buffer starting from specified offset
     */
    fun writeToCharBuffer(buffer: CharArray, offset: Int) {
        var t = totalSeconds
        val hour = t / 3600
        t -= hour * 60
        val minute = t / 60
        t -= minute * 60

        buffer.writePaddedTwoDigits(offset, hour)
        buffer[offset + 2] = ':'
        buffer.writePaddedTwoDigits(offset + 3, minute)
        buffer[offset + 5] = ':'
        buffer.writePaddedTwoDigits(offset + 6, t)
    }

    fun writeOnlyHourMinutesToCharBuffer(buffer: CharArray, offset: Int) {
        var t = totalSeconds

        val hour = t / 3600
        t -= hour * 3600
        val minute = t / 60

        buffer.writePaddedTwoDigits(offset, hour)
        buffer[offset + 2] = ':'
        buffer.writePaddedTwoDigits(offset + 3, minute)
    }

    /**
     * Appends string representation of time to particular [StringBuilder]
     *
     * @throws IllegalArgumentException if time is not valid
     */
    fun append(sb: StringBuilder) {
        var t = totalSeconds

        val hour = t / 3600
        t -= hour * 3600
        val minute = t / 60
        t -= minute * 60

        sb.appendPaddedTwoDigits(hour)
        sb.append(':')
        sb.appendPaddedTwoDigits(minute)
        sb.append(':')
        sb.appendPaddedTwoDigits(t)
    }

    companion object {
        @Stable
        internal fun isValidTotalSeconds(totalSeconds: Int): Boolean {
            return totalSeconds in 0 until SECONDS_IN_DAY
        }
    }
}