package io.pelmenstar.circularTimePicker.compose

import android.graphics.Paint
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import io.pelmenstar.onealarm.shared.*
import io.pelmenstar.onealarm.shared.compose.manualAnimation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.*

private val LABELS_24 = arrayOf("0", "6", "12", "18")
private val LABELS_12 = arrayOf("12", "3", "6", "9")
private val LABELS_4 = arrayOf("0", "1", "2", "3")
private val LABELS_1 = arrayOf("0", "15", "30", "45")

private const val LABEL_COUNT = 4

private const val TICK_COUNT = 120
private const val ANGLE_PER_TICK = (2 * PI_F) / TICK_COUNT

private const val MAX_HOURS_CHANGE_ANIMATION_DURATION = 250

private const val TAG = "TimePicker"

private fun getLabels(maxHours: Int): Array<out String> {
    return when (maxHours) {
        24 -> LABELS_24
        12 -> LABELS_12
        4 -> LABELS_4
        1 -> LABELS_1
        else -> throw IllegalArgumentException("maxHours")
    }
}

private suspend inline fun PointerInputScope.detectPrimaryTouchEvents(
    crossinline onDown: (Offset) -> Unit,
    crossinline onMove: (Offset) -> Boolean,
    crossinline onUp: () -> Unit
) {
    forEachGesture {
        awaitPointerEventScope {
            val change = awaitFirstDown(requireUnconsumed = false)
            onDown(change.position)

            drag(change.id) {
                val toConsume = onMove(it.position)
                if (toConsume) {
                    it.consume()
                }
            }

            onUp()
        }
    }
}

private fun ((hour: Int, minute: Int) -> Unit).invokeIfNeeded(minutes: Int) {
    val hour = minutes / 60
    val minute = minutes - hour * 60

    invoke(hour, minute)
}

@Stable
private fun Int.adjustedMinutes(maxHours: Int): Int {
    return if (this >= maxHours * 60) {
        0
    } else {
        this
    }
}

class CircularTimePickerState internal constructor (
    private val animationScope: CoroutineScope,
    initialTotalMinutes: Int,
    initialMaxHours: Int
) {
    internal var angle by mutableStateOf(MathUtils.minutesToAngle(initialTotalMinutes, initialMaxHours))
    internal var maxHours by mutableStateOf(initialMaxHours)

    fun getMaxHours(): Int {
        return maxHours
    }

    fun setMaxHours(value: Int) {
        val newAngle = MathUtils.minutesToAngle(
            MathUtils.angleToMinutes(angle, maxHours),
            value
        )

        maxHours = value

        startAngleChangeAnimation(angle, newAngle)
    }

    fun getTime(): Int {
        return MathUtils.angleToMinutes(angle, maxHours)
    }

    fun setTime(hour: Int, minute: Int) {
        setTime(hour * 60 + minute)
    }

    fun setTime(totalMinutes: Int) {
        val newAngle = MathUtils.minutesToAngle(totalMinutes, maxHours)

        startAngleChangeAnimation(angle, newAngle)
    }

    private fun startAngleChangeAnimation(oldValue: Float, newValue: Float) {
        animationScope.launch {
            manualAnimation(
                MAX_HOURS_CHANGE_ANIMATION_DURATION,
                LinearEasing,
                setValue = { fraction ->
                    angle = lerp(oldValue, newValue, fraction)
                }
            )
        }
    }
}

@Composable
fun rememberCircularPickerState(totalMinutes: Int = 0, maxHours: Int = 24): CircularTimePickerState {
    val scope = rememberCoroutineScope()

    return remember { CircularTimePickerState(scope, totalMinutes, maxHours) }
}

@Composable
fun CircularTimePicker(
    modifier: Modifier = Modifier,
    sliderRangeColor: Color = MaterialTheme.colors.primary,
    sliderColor: Color = Color(0xFFE1E1E1),
    sliderWidth: Dp = 8.dp,

    thumbSize: Dp = 28.dp,
    thumbColor: Color = MaterialTheme.colors.primary,
    thumbSizeActiveGrow: Float = 1.2f,

    isClockVisible: Boolean = true,
    clockLabelSize: TextUnit = 15.sp,
    clockLabelColor: Color = MaterialTheme.colors.onBackground,
    clockTickColor: Color = MaterialTheme.colors.onBackground,

    timeTextSize: TextUnit = 22.sp,
    timeTextColor: Color = MaterialTheme.colors.onBackground,

    state: CircularTimePickerState,

    onDragStarted: (() -> Unit)? = null,
    onDragEnded: (() -> Unit)? = null,
    onTimeChanged: ((totalMinutes: Int) -> Unit)? = null
) {
    val density = LocalDensity.current

    var size by remember {
        mutableStateOf(IntSize(0, 0))
    }

    // Radius of a whole picker without the area which takes half of a thumb.
    val radius by remember(size, thumbSize, thumbSizeActiveGrow, sliderWidth) {
        derivedStateOf {
            val maxComponent = min(size.width, size.height)

            max(0.5f * (maxComponent - thumbSize.value * thumbSizeActiveGrow * density.density), 0f)
        }
    }

    // Radius of the clock.
    val clockRadius by remember(radius, thumbSize, thumbSizeActiveGrow, sliderWidth) {
        derivedStateOf {
            max(radius - (thumbSize.value * thumbSizeActiveGrow - 4f) * density.density, 0f)
        }
    }

    val thumbPosition by remember(size, radius, state.angle) {
        derivedStateOf {
            val center = size.center.toOffset()
            val angle = state.angle

            Offset(
                center.x + radius * cos(angle),
                center.y - radius * sin(angle),
            )
        }
    }

    val timeBuffer = remember {
        CharArray(5).also { it[2] = ':' }
    }

    val timePaint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }

    val timeSize = remember(timeTextColor, timeTextSize, state.maxHours) {
        timePaint.let {
            it.color = timeTextColor.toArgb()
            it.textSize = timeTextSize.value * density.density * density.fontScale

            // If maxHours = 1, then hour value is not shown.
            it.getTextSizeToIntSize(if (state.maxHours == 1) "00" else "00:00")
        }
    }

    var isThumbActiveInitialized by remember {
        mutableStateOf(false)
    }

    // When maxHours is changed, thumb should become inactive
    var isThumbActive by remember(state.maxHours) {
        mutableStateOf(false)
    }

    var animatedThumbGrow by remember {
        mutableStateOf(1f)
    }

    LaunchedEffect(isThumbActive, thumbSizeActiveGrow) {
        if (isThumbActiveInitialized) {
            val initialValue: Float
            val targetValue: Float

            if (isThumbActive) {
                initialValue = 1f
                targetValue = thumbSizeActiveGrow
            } else {
                initialValue = thumbSizeActiveGrow
                targetValue = 1f
            }

            manualAnimation(
                250,
                FastOutSlowInEasing,
                setValue = { fraction ->
                    animatedThumbGrow = lerp(initialValue, targetValue, fraction)
                }
            )
        }
    }

    Box(modifier = modifier.onSizeChanged { size = it }) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state.maxHours, thumbSize, onDragStarted, onDragEnded) {
                    val maxHours = state.maxHours

                    detectPrimaryTouchEvents(
                        onDown = {
                            val activeRadius = thumbSize.toPx() * 2f

                            // Check if a pointer is near thumb position, if it's, make thumb active.
                            if (MathUtils.isPointInCircle(it, thumbPosition, activeRadius)) {
                                isThumbActiveInitialized = true
                                isThumbActive = true

                                onDragStarted?.invoke()
                            }
                        },
                        onMove = {
                            if (isThumbActive) {
                                val center = size.center.toOffset()

                                var angle = atan2(center.y - it.y, it.x - center.x)

                                // Handle special case when minutes may be wrong.
                                var minutes = MathUtils.angleToMinutes(angle, maxHours)
                                if (minutes >= maxHours * 60 - 1) {
                                    minutes = 0
                                    angle = MathUtils.minutesToAngle(0, maxHours)
                                }

                                state.angle = angle

                                onTimeChanged?.invoke(minutes)

                                true
                            } else {
                                false
                            }
                        },
                        onUp = {
                            if (isThumbActive) {
                                isThumbActive = false

                                onDragEnded?.invoke()
                            }
                        }
                    )
                }
        ) {
            val center = size.center.toOffset()
            val sliderWidthPx = sliderWidth.toPx()

            val sweepAngle = MathUtils.angleTo2Pi((PI_F / 2) - state.angle) * R2D

            val areaTopLeft = Offset(center.x - radius, center.y - radius)
            val areaSizeComponent = radius * 2f
            val areaSize = Size(areaSizeComponent, areaSizeComponent)

            drawOval(
                color = sliderColor,
                topLeft = areaTopLeft,
                size = areaSize,
                style = Stroke(sliderWidthPx)
            )

            drawArc(
                color = sliderRangeColor,
                startAngle = 270f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = areaTopLeft,
                size = areaSize,
                style = Stroke(sliderWidthPx, cap = StrokeCap.Round)
            )

            drawCircle(
                color = thumbColor,
                center = thumbPosition,
                radius = thumbSize.toPx() * 0.5f * animatedThumbGrow
            )

            drawContext.canvas.nativeCanvas.run {
                val x = center.x - timeSize.width * 0.5f
                val y = center.y + timeSize.height * 0.5f

                val minutes = state.getTime()

                val textLength: Int

                if (state.maxHours == 1) {
                    textLength = 2

                    timeBuffer.writePaddedTwoDigits(0, minutes)
                } else {
                    textLength = 5

                    val hour = minutes / 60
                    val minute = minutes - hour * 60

                    timeBuffer.writePaddedTwoDigits(0, hour)
                    // We don't need to set ':' at 2 position, because it was set before.
                    timeBuffer.writePaddedTwoDigits(3, minute)
                }

                drawText(timeBuffer, 0, textLength, x, y, timePaint)
            }
        }

        if (isClockVisible) {
            Clock(
                modifier = Modifier.fillMaxSize(),
                maxHours = state.maxHours,
                clockRadius = clockRadius,
                clockLabelSize = clockLabelSize,
                clockLabelColor = clockLabelColor,
                clockTickColor = clockTickColor
            )
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Float.inRangeOffset(base: Float, offset: Float): Boolean {
    return abs(this - base) <= offset
}

// If ostensible tick is near the hour text, then it shouldn't be shown.
private fun Float.isNotTick(offsetAngle: Float): Boolean {
    return inRangeOffset(PI_F / 2, offsetAngle) ||
            inRangeOffset(PI_F, offsetAngle) ||
            inRangeOffset(PI_F * 1.5f, offsetAngle) ||
            this >= 2 * PI_F - offsetAngle || this <= offsetAngle
}

private fun drawTicks(
    nativeCanvas: NativeCanvas,
    paint: NativePaint,
    clockTickColor: Color,
    ticks: FloatArray,
    offsetAngle: Float,
    center: Offset,
    clockRadius: Float,
    density: Float,
    maxHours: Int
) {
    val endRadius = clockRadius - 4f * density /* -4dp */

    // Period of hour ticks, it tick is 'hour', then it's renderered thicker
    val hourTick = TICK_COUNT / maxHours

    // Position of 'hour' ticks in 'ticks' array
    var hourTickInitialOffset = 0

    // Count of 'hour' ticks at all
    var hourTickPointsCount = 0

    for (i in 0 until TICK_COUNT) {
        val angle = ANGLE_PER_TICK * i

        if (angle.isNotTick(offsetAngle)) {
            continue
        }

        // If it's not 'hour' tick, then it's 'minute' one and initialOffset should be moved for 4 elements
        if (i % hourTick != 0) {
            hourTickInitialOffset += 4
        } else {
            hourTickPointsCount += 4
        }
    }

    var hourTickOffset = hourTickInitialOffset

    // 'minute' ticks starts from 0 position
    var minuteTickOffset = 0

    for (i in 0 until TICK_COUNT) {
        val angle = ANGLE_PER_TICK * i

        if (angle.isNotTick(offsetAngle)) {
            continue
        }

        val sinCosPair = SIN_COS_FOR_TICKS[i]
        val sin = FloatPair.getFirst(sinCosPair) // sin(angle)
        val cos = FloatPair.getSecond(sinCosPair) // cos(angle)

        val startX = center.x + cos * clockRadius
        val startY = center.y + sin * clockRadius

        val endX = center.x + cos * endRadius
        val endY = center.y + sin * endRadius

        if (i % hourTick == 0) {
            ticks[hourTickOffset++] = startX
            ticks[hourTickOffset++] = startY
            ticks[hourTickOffset++] = endX
            ticks[hourTickOffset++] = endY
        } else {
            ticks[minuteTickOffset++] = startX
            ticks[minuteTickOffset++] = startY
            ticks[minuteTickOffset++] = endX
            ticks[minuteTickOffset++] = endY
        }
    }

    paint.color = clockTickColor.toArgb()

    nativeCanvas.drawLines(ticks, 0, hourTickInitialOffset, paint.apply {
        alpha = 100
        strokeWidth = density
    })

    nativeCanvas.drawLines(ticks, hourTickInitialOffset, hourTickPointsCount, paint.apply {
        alpha = 180
        strokeWidth = density * 2f
    })
}

@Composable
private fun Clock(
    modifier: Modifier,
    maxHours: Int,

    clockRadius: Float,
    clockLabelSize: TextUnit,
    clockLabelColor: Color,
    clockTickColor: Color
) {
    val densityObj = LocalDensity.current
    val density = densityObj.density
    val fontScale = densityObj.fontScale

    var currentMaxHours by remember {
        mutableStateOf(maxHours)
    }

    val labelPaint by remember(clockLabelSize, clockLabelColor) {
        derivedStateOf {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = clockLabelColor.toArgb()
                textSize = clockLabelSize.value * density * fontScale
            }
        }
    }

    val tickPaint by remember {
        derivedStateOf {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                // When caps are round, it looks prettier
                strokeCap = Paint.Cap.ROUND
            }
        }
    }

    var maxLabelSizeComponent by remember {
        mutableStateOf(0)
    }

    val halfLabelSizes by remember(clockLabelSize, currentMaxHours) {
        derivedStateOf {
            val labels = getLabels(currentMaxHours)

            var maxSizeComponent = 0
            val sizes = LongArray(LABEL_COUNT)

            for (i in 0 until LABEL_COUNT) {
                val label = labels[i]

                val labelSize = labelPaint.getTextSizeToLong(label)
                val width = IntPair.getFirst(labelSize)
                val height = IntPair.getSecond(labelSize)

                // Compute max label size component as well, in one pass.
                maxSizeComponent = max(maxSizeComponent, width)
                maxSizeComponent = max(maxSizeComponent, height)

                sizes[i] = FloatPair.create(width * 0.5f, height * 0.5f)
            }

            maxLabelSizeComponent = maxSizeComponent

            sizes
        }
    }

    val offsetAngle by remember(clockRadius, maxLabelSizeComponent) {
        derivedStateOf {
            val innerClockRadius = clockRadius - 4 * density /* -4dp */
            val offset = maxLabelSizeComponent * 0.5f

            asin(offset / innerClockRadius) + 3 * D2R
        }
    }

    val ticksPoints = remember {
        // It takes 4 values to draw a line (x0, y0, x1, y1)
        FloatArray(TICK_COUNT * 4)
    }

    var scale by remember {
        mutableStateOf(1f)
    }

    LaunchedEffect(maxHours) {
        // Animation when maxHours value is changed
        if (currentMaxHours != maxHours) {
            manualAnimation(
                MAX_HOURS_CHANGE_ANIMATION_DURATION,
                LinearEasing,
                setValue = { fraction ->
                    scale = abs(2 * fraction - 1f)
                },
                onFrame = { time ->
                    // Switch to new maxHours value when half of the animation passed.
                    if (time >= MAX_HOURS_CHANGE_ANIMATION_DURATION / 2) {
                        currentMaxHours = maxHours
                    }
                }
            )

            scale = 1f
        }
    }

    if (clockRadius > 0f) {
        Canvas(
            modifier = modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        ) {
            val labels = getLabels(currentMaxHours)

            val center = size.center
            val nativeCanvas = drawContext.canvas.nativeCanvas

            drawTicks(
                nativeCanvas,
                tickPaint,
                clockTickColor,
                ticksPoints,
                offsetAngle,
                center,
                clockRadius,
                density,
                currentMaxHours
            )

            for (i in labels.indices) {
                val label = labels[i]

                val labelSize = halfLabelSizes[i]
                val xyFractionPair = X_Y_FRACTION_TABLE_FOR_LABELS[i]
                val xFraction = FloatPair.getFirst(xyFractionPair)
                val yFraction = FloatPair.getSecond(xyFractionPair)

                val halfWidth = FloatPair.getFirst(labelSize)
                val halfHeight = FloatPair.getSecond(labelSize)

                var posX = center.x + xFraction * clockRadius - halfWidth
                var posY = center.y + yFraction * clockRadius + halfHeight

                when (i) {
                    1 -> {
                        posX -= halfWidth
                    }
                    2 -> {
                        posY -= halfHeight
                    }
                }

                nativeCanvas.drawText(label, posX, posY, labelPaint)
            }
        }
    }
}