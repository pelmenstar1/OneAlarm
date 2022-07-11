package io.pelmenstar.onealarm.shared.compose

import androidx.compose.runtime.*

/**
 * Represents an infinite transition which will continue until the function leaves the composition.
 *
 * It returs a state which holds an animation fraction in range `0..1` and it's changed on each animation frame.
 * The animation is looped which means that when forward animation ends, reverse starts.
 * In other words, animation fraction will rise from 0 -> 1 and then descends from 1 -> 0 and then rise from 0 -> 1 and so on.
 */
@Composable
fun fractionInfititeTransition(durationMillis: Long): State<Float> {
    val state = remember {
        mutableStateOf(0f)
    }

    LaunchedEffect(durationMillis) {
        val durationNanos = durationMillis * 1_000_000

        var startTime = withFrameNanos { it }
        var isReversed = false

        while(true) {
            withFrameNanos { currentTime ->
                val timeDiff = currentTime - startTime

                if(timeDiff >= durationNanos) {
                    startTime = currentTime

                    if(isReversed) {
                        isReversed = false
                        state.value = 0f
                    } else {
                        isReversed = true
                        state.value = 1f
                    }
                } else {
                    var fraction = timeDiff.toFloat() / durationNanos
                    if(isReversed) {
                        fraction = 1f - fraction
                    }

                    state.value = fraction
                }
            }
        }
    }

    return state
}