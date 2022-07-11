package io.pelmenstar.onealarm.shared

import androidx.compose.runtime.Stable
import kotlin.math.PI

const val PI_F = PI.toFloat()
const val R2D = 180f / PI_F
const val D2R = PI_F / 180f

fun Int.hasFlag(flag: Int): Boolean {
    return (this and flag) != 0
}

/**
 * Does the same as [Math.floorMod] but it's available on all API levels.
 */
@Stable
fun floorMod(x: Long, y: Long): Long {
    val r = x / y
    var aligned = r * y

    // if the signs are different and modulo not zero, round down
    if (x xor y < 0 && aligned != x) {
        aligned -= y
    }

    return x - aligned
}

@Stable
fun lerp(start: Float, end: Float, fraction: Float): Float {
    return start + (end - start) * fraction
}

@Stable
fun lerp(start: Int, end: Int, fraction: Float): Int {
    return start + ((end - start).toFloat() * fraction).toInt()
}