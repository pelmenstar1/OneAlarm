package io.pelmenstar.circularTimePicker.compose

import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.ui.unit.IntSize
import io.pelmenstar.onealarm.shared.IntPair

private val rect = Rect()

fun Paint.getTextSizeToIntSize(buffer: CharArray): IntSize {
    return packRectToIntSize { getTextBounds(buffer, 0, buffer.size, rect) }
}

fun Paint.getTextSizeToIntSize(text: String): IntSize {
    return packRectToIntSize { getTextBounds(text, 0, text.length, rect) }
}

fun Paint.getTextSizeToLong(buffer: CharArray): Long {
    return packRectToLong { getTextBounds(buffer, 0, buffer.size, rect) }
}

fun Paint.getTextSizeToLong(text: String): Long {
    return packRectToLong { getTextBounds(text, 0, text.length, rect) }
}

private inline fun packRectToIntSize(block: () -> Unit): IntSize {
    rect.setEmpty()
    block()

    return IntSize(rect.width(), rect.height())
}

private inline fun packRectToLong(block: () -> Unit): Long {
    rect.setEmpty()
    block()

    return IntPair.create(rect.width(), rect.height())
}