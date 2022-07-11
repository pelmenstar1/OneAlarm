@file:Suppress("NOTHING_TO_INLINE")

package io.pelmenstar.onealarm.shared

import androidx.compose.runtime.Stable
import java.util.*
import kotlin.text.StringBuilder

/**
 * Parses receiver [CharSequence] to positive integer. If format is invalid, returns `-1`
 */
@Stable
fun CharSequence.toPositiveInt(start: Int = 0, end: Int = length): Int {
    if (start == end) {
        return -1
    }

    var n = 0

    for (i in start until end) {
        val c = this[i]
        val d = c - '0'

        if(d in 0..9) {
            n = n * 10 + d
        } else {
            return -1
        }
    }
    return n
}

/**
 * Determines whether format of receiver [CharSequence] is valid for parsing by [toPositiveInt]
 */
@Stable
fun CharSequence.isValidPositiveInt(start: Int, end: Int): Boolean {
    if (start == end) {
        return false
    }

    for (i in start until end) {
        val c = this[i]

        if (c !in '0'..'9') {
            return false
        }
    }

    return true
}

private fun checkRangeTwoDigits(number: Int) {
    require(number in 0..99) { "number is out of range" }
}

private fun checkRangeFourDigits(number: Int) {
    require(number in 0..9999) { "number is out of range" }
}

/**
 * Simply appends [number] to [StringBuilder] if it's >= 10. If it's < 10, then appends '0' first.
 * For example: StringBuilder().appendPaddedTwoDigits(1).toString() == "01"
 */
fun Appendable.appendPaddedTwoDigits(number: Int) {
    checkRangeTwoDigits(number)

    appendTwoDigitsInternal(number)
}

/**
 * Writes [number] as is to [CharArray] if [number] >= 10. If it's < 10, then writes '0' first.
 */
fun CharArray.writePaddedTwoDigits(offset: Int, number: Int) {
    checkRangeTwoDigits(number)

    val ten = number / 10
    val one = number - ten * 10

    this[offset] = '0' + ten
    this[offset + 1] = '0' + one
}

private fun Appendable.appendTwoDigitsInternal(number: Int) {
    val ten = number / 10
    val one = number - ten * 10

    append('0' + ten)
    append('0' + one)
}

/**
 * Appends number padded with '0' to be 4 digits in length.
 */
fun Appendable.appendPaddedFourDigits(number: Int) {
    checkRangeFourDigits(number)

    val d = number / 100
    val r = number - d * 100

    appendTwoDigitsInternal(d)
    appendTwoDigitsInternal(r)
}

/**
 * Writes number padded with '0' to be 2 ditis in length.
 */
fun CharArray.writeFourDigits(offset: Int, number: Int) {
    checkRangeFourDigits(number)

    val d = number / 100
    val r = number - d * 100

    writePaddedTwoDigits(offset, d)
    writePaddedTwoDigits(offset + 2, r)
}

inline fun buildStringWithStringBuffer(block: StringBuffer.() -> Unit): String {
    return StringBuffer().apply(block).toString()
}

inline fun buildStringWithStringBuffer(capacity: Int, block: StringBuffer.() -> Unit): String {
    return StringBuffer(capacity).apply(block).toString()
}

inline fun Appendable.appendCapitalized(text: String, locale: Locale) {
    if(text.isNotEmpty()) {
        val firstChar = text[0]

        if(firstChar.isLowerCase()) {
            val titleChar = firstChar.titlecaseChar()

            if(titleChar != firstChar.uppercaseChar()) {
                append(titleChar)
            } else {
                append(text.substring(0, 1).uppercase(locale))
            }

            append(text, 1, text.length)
        }
    }
}