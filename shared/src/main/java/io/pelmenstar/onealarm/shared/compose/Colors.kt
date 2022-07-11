package io.pelmenstar.onealarm.shared.compose

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import io.pelmenstar.onealarm.shared.lerp

/**
 * Returns receiver color with the same R, G, B components and with new, specified [alpha].
 *
 * @param alpha alpha component which must be in range `0..1`, otherwise the result is undefined
 */
@Stable
fun Color.withAlpha(@FloatRange(from = 0.0, to = 1.0) alpha: Float): Color {
    return withAlpha255((alpha * 255f).toInt())
}

/**
 * Returns receiver color with the same R, G, B components and with new, specified [alpha].
 *
 *  @param alpha alpha component which must be in range `0..255`, otherwise the result is undefined
 */
@Stable
fun Color.withAlpha255(@IntRange(from = 0, to = 255) alpha: Int): Color {
    val value = value.toLong()

    val newValue = if ((value and 0x3fL) == 0L) {
        (value and 0x00FFFFFF_FFFFFFFFL) or (alpha.toLong() shl 56)
    } else {
        (value and (-65473L)) or (((alpha * 1023) / 255).toLong() shl 6)
    }

    return Color(newValue.toULong())
}

private fun getAlpha(colorInt: Int): Int = colorInt.ushr(24)
private fun getRed(colorInt: Int): Int = colorInt shr 16 and 0xFF
private fun getGreen(colorInt: Int): Int = colorInt shr 8 and 0xFF
private fun getBlue(colorInt: Int): Int = colorInt and 0xFF

/**
 * Linear interpolates color from the given [start] to [end] based on the [fraction].
 *
 * The main difference from Jetpack's color lerp is that this function does RGB interpolation which means
 * it simply interpolates each ARGB component from start to end. If consider RGB color as XYZ space, then
 * this type of interpolation is totally valid. But RGB color is a mix of RGB components which means
 * if colors are located far from each other in RGB space, unexpected transition can happen during interpolation.
 * This function give worse result than Jetpack's one, but it's more performant.
 */
@Stable
fun rgbColorLerp(start: Color, end: Color, fraction: Float): Color {
    return rgbColorLerp(start.toArgb(), end.toArgb(), fraction)
}


/**
 * Linear interpolates color from the given [rgbStart] to [rgbEnd] based on the [fraction].
 *
 * [rgbStart] and [rgbEnd] are encoded as ARGB color int in Android.
 */
@Stable
fun rgbColorLerp(rgbStart: Int, rgbEnd: Int, fraction: Float): Color {
    return Color(
        lerp(getRed(rgbStart), getRed(rgbEnd), fraction),
        lerp(getGreen(rgbStart), getGreen(rgbEnd), fraction),
        lerp(getBlue(rgbStart), getBlue(rgbEnd), fraction),
        lerp(getAlpha(rgbStart), getAlpha(rgbEnd), fraction)
    )
}