package io.pelmenstar.circularTimePicker.compose

import androidx.compose.ui.geometry.Offset
import io.pelmenstar.onealarm.shared.PI_F
import kotlin.math.*

internal object MathUtils {
    fun angleTo2Pi(angle: Float): Float {
        var result = angle % (2 * PI_F)
        if (result < 0) {
            result += (2 * PI_F)
        }

        return result
    }

    fun minutesToAngle(minutes: Int, maxHours: Int): Float {
        return angleTo2Pi(
            (PI_F / 2) - minutes.toFloat() * ((2 * PI_F) / 60f) / maxHours.toFloat()
        )
    }

    fun angleToMinutes(angle: Float, maxHours: Int): Int {
        val precise = angleTo2Pi((PI_F / 2) - angle) * (maxHours * 60).toFloat() * (1f / (2 * PI_F))

        return precise.roundToInt() % (24 * 60)
    }

    fun isPointInCircle(
        point: Offset,
        center: Offset,
        radius: Float
    ): Boolean {
        val dx = point.x - center.x
        val dy = point.y - center.y

        return dx * dx + dy * dy < radius * radius
    }
}