package io.pelmenstar.onealarm.ui

import androidx.annotation.StringRes
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.pelmenstar.onealarm.AlarmMode
import io.pelmenstar.onealarm.R
import io.pelmenstar.onealarm.shared.compose.manualAnimation
import io.pelmenstar.onealarm.shared.compose.rgbColorLerp

private const val SIDE_START = 0
private const val SIDE_END = 1

@Suppress("UNCHECKED_CAST")
private inline fun <reified From, reified To> spec(spec: AnimationSpec<From>): AnimationSpec<To> {
    return spec as AnimationSpec<To>
}

@Composable
private fun RowScope.AlarmModeChoice(
    side: Int,
    @StringRes textRes: Int,
    choiceMode: AlarmMode,
    selectionFraction: Float,
    onModeChanged: (AlarmMode) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val topStart: CornerSize
    val bottomStart: CornerSize

    val topEnd: CornerSize
    val bottomEnd: CornerSize

    if (side == SIDE_START) {
        topStart = CornerSize(50)
        bottomStart = CornerSize(50)

        topEnd = CornerSize(0f)
        bottomEnd = CornerSize(0f)
    } else {
        topStart = CornerSize(0f)
        bottomStart = CornerSize(0f)

        topEnd = CornerSize(50)
        bottomEnd = CornerSize(50)
    }

    val shape = RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)

    Text(
        modifier = Modifier
            .background(
                rgbColorLerp(
                    MaterialTheme.colors.background,
                    MaterialTheme.colors.primary,
                    selectionFraction
                ),
                shape
            )
            .weight(0.5f)
            .fillMaxHeight()
            .clickable(interactionSource = interactionSource, indication = null) {
                onModeChanged(choiceMode)
            }
            .border(
                (1f - selectionFraction).dp,
                MaterialTheme.colors.onBackground,
                shape
            )
            .padding(vertical = 5.dp)
            .align(Alignment.CenterVertically),
        text = stringResource(textRes),
        textAlign = TextAlign.Center,
        color = rgbColorLerp(
            MaterialTheme.colors.onBackground,
            MaterialTheme.colors.onPrimary,
            selectionFraction
        )
    )
}

@Composable
fun AlarmModeSwitch(
    modifier: Modifier = Modifier,
    mode: AlarmMode = AlarmMode.FROM_NOW,
    onModeChanged: (AlarmMode) -> Unit = {},
) {
    Row(modifier = modifier) {
        var isModeInitialized by remember {
            mutableStateOf(false)
        }

        var fromNowSelectionFraction by remember {
            mutableStateOf(if (mode == AlarmMode.FROM_NOW) 1f else 0f)
        }

        LaunchedEffect(mode) {
            if (isModeInitialized) {
                manualAnimation(
                    250,
                    LinearEasing,
                    setValue = {
                        var fraction = it

                        if (mode == AlarmMode.EXACT_AT) {
                            fraction = 1f - fraction
                        }

                        fromNowSelectionFraction = fraction
                    }
                )
            }
        }

        val onModeChangedInternal: (AlarmMode) -> Unit = remember(onModeChanged) {
            {
                isModeInitialized = true

                onModeChanged(it)
            }
        }

        AlarmModeChoice(
            SIDE_START,
            R.string.alarm_mode_from_now,
            AlarmMode.FROM_NOW,
            fromNowSelectionFraction,
            onModeChangedInternal
        )
        AlarmModeChoice(
            SIDE_END,
            R.string.alarm_mode_exactly_at,
            AlarmMode.EXACT_AT,
            1f - fromNowSelectionFraction,
            onModeChangedInternal
        )
    }
}