package io.pelmenstar.onealarm.shared.compose

import androidx.compose.animation.core.Easing
import androidx.compose.runtime.withFrameNanos

/**
 * This function allows to build 'manual' animations
 * where you get simply a fraction of the animation in range `0..1` and make your animation whatever you want based on the fraction.
 *
 * @param duration duration of animation measured in milliseconds
 * @param easing time interpolator which is applied to the fraction on each frame
 * @param setValue lambda which is called on each frame. The fraction of the animation is passed as an argument
 * @param onFrame lambda which is called on each frame. Elapsed time since the start is passed as an argument
 */
suspend inline fun manualAnimation(
    duration: Int,
    easing: Easing,
    setValue: (fraction: Float) -> Unit,
    onFrame: (elapsedTime: Long) -> Unit = {}
) {
    val startTime = withFrameNanos { it }

    val durationNanos = duration * 1_000_000L
    var elapsedTime = 0L

    while(elapsedTime < durationNanos) {
        elapsedTime = withFrameNanos { it } - startTime

        onFrame(elapsedTime)

        val raw = elapsedTime.coerceIn(0, durationNanos).toFloat() / durationNanos
        val interpolated = easing.transform(raw)

        setValue(interpolated)
    }
}