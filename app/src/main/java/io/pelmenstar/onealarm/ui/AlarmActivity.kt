package io.pelmenstar.onealarm.ui

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.minus
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import io.pelmenstar.onealarm.*
import io.pelmenstar.onealarm.R
import io.pelmenstar.onealarm.data.AppDatabase
import io.pelmenstar.onealarm.data.InternalAlarmsManager
import io.pelmenstar.onealarm.shared.compose.TextClock
import io.pelmenstar.onealarm.shared.compose.fractionInfititeTransition
import io.pelmenstar.onealarm.shared.compose.rgbColorLerp
import io.pelmenstar.onealarm.shared.compose.withAlpha
import io.pelmenstar.onealarm.shared.lerp
import io.pelmenstar.onealarm.shared.time.MILLIS_IN_MINUTE
import io.pelmenstar.onealarm.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {
    private lateinit var viewModel: AlarmScreenViewModel

    private var alarmId: Long = 0

    private var isServiceBound = false
    private var service: AlarmService? = null

    @Inject
    lateinit var appDatabase: AppDatabase

    @Inject
    lateinit var preferences: DataStore<Preferences>

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as AlarmService.LocalBinder?)?.getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent
        alarmId = intent.getLongExtra(EXTRA_ID, 0)

        volumeControlStream = AudioManager.STREAM_ALARM

        val window = window

        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)

            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
            )
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
            )
        }

        if (Build.VERSION.SDK_INT >= 26) {
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

            keyguardManager.requestDismissKeyguard(this, null)
        }

        window.run {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

            addFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION or
                        WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
            )

            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.TRANSPARENT
        }

        setContent {
            viewModel = hiltViewModel()

            AlarmScreen(viewModel, this::dismissAlarm, this::snoozeAlarm, this::finish, alarmId)
        }

        bindAlarmService()
    }

    override fun onDestroy() {
        super.onDestroy()

        unbindAlarmService()
    }

    private fun bindAlarmService() {
        if (!isServiceBound) {
            isServiceBound = bindService(
                AlarmService.createIntent(this, alarmId, AlarmService.ACTION_START),
                serviceConnection,
                BIND_AUTO_CREATE
            )
        }
    }

    private fun unbindAlarmService() {
        if (isServiceBound) {
            isServiceBound = false

            unbindService(serviceConnection)
        }
    }

    private fun dismissAlarm() {
        doAndUndindService { dismissAlarm(alarmId) }
    }

    private fun snoozeAlarm() {
        doAndUndindService { snoozeAlarm(alarmId) }
    }

    private inline fun doAndUndindService(action: AlarmService.() -> Unit) {
        service?.action()

        unbindAlarmService()
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE,
            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_CAMERA,
            KeyEvent.KEYCODE_FOCUS -> {
                lifecycleScope.launch {
                    when (AppPreferences.volumeButtonBehaviour.getFrom(preferences)) {
                        VolumeButtonBehaviour.SNOOZE -> {
                            snoozeAlarm()
                        }
                        VolumeButtonBehaviour.DISMISS -> {
                            dismissAlarm()
                        }
                        else -> {}
                    }
                }

                return true
            }
        }

        return super.dispatchKeyEvent(event)
    }

    override fun onBackPressed() {
    }

    companion object {
        private const val EXTRA_ID = "io.pelmenstar.onealarm.ui.AlarmActivity.alarm_id"

        fun createIntent(context: Context, alarmId: Long): Intent {
            return Intent(context, AlarmActivity::class.java).apply {
                putExtra(EXTRA_ID, alarmId)
            }
        }
    }
}

@HiltViewModel
private class AlarmScreenViewModel @Inject constructor(
    appDatabase: AppDatabase,
    preferences: DataStore<Preferences>,
    private val alarmsManager: InternalAlarmsManager
) : ViewModel() {
    private val alarmsDao = appDatabase.alarmsDao()

    val snoozeDurationMinutes = AppPreferences.snoozeDuration.toFlow(preferences)
    val silenceAfterMinutes = AppPreferences.silenceAfter.toFlow(preferences)
}

private enum class Action {
    DISMISS,
    SNOOZE
}

@Composable
private fun RowScope.ActionPickerChoice(@StringRes textRes: Int, selectionFraction: Float) {
    val colors = MaterialTheme.colors

    Box(
        modifier = Modifier
            .weight(0.5f)
            .scale(lerp(1.0f, 1.05f, selectionFraction))
            .align(Alignment.CenterVertically)
            .aspectRatio(1f)
            .padding(10.dp)
            .background(
                colors.primary.withAlpha(selectionFraction * 0.5f),
                CircleShape
            )
    ) {
        val textColor = rgbColorLerp(colors.onBackground, colors.onPrimary, selectionFraction)

        Text(
            modifier = Modifier.align(Alignment.Center),
            text = stringResource(textRes),
            color = textColor,
            style = MaterialTheme.typography.h6
        )
    }
}

@Composable
private fun ActionPicker(
    modifier: Modifier,
    onActionSelected: (Action) -> Unit
) {
    val iconSize = 48.dp

    val selectThreshold: Float
    val halfIconSizePx: Float
    val halfIconOffset: Offset

    with(LocalDensity.current) {
        halfIconSizePx = iconSize.toPx() * 0.5f
        halfIconOffset = Offset(halfIconSizePx, halfIconSizePx)
        selectThreshold = 20.dp.toPx()
    }

    var size by remember { mutableStateOf(IntSize(0, 0)) }

    val originIconOffset by remember(size) {
        derivedStateOf {
            size.center - halfIconOffset
        }
    }

    var movedIconOffsetX by remember(originIconOffset) {
        mutableStateOf(originIconOffset.x)
    }

    val selectionFraction by remember(originIconOffset, movedIconOffsetX, size) {
        derivedStateOf {
            val delta = abs(movedIconOffsetX + halfIconSizePx - size.width * 0.5f)

            if (delta > selectThreshold) {
                (delta - selectThreshold) / (size.width * 0.25f - selectThreshold)
            } else {
                0f
            }
        }
    }

    val selectionCandidate by remember(originIconOffset, movedIconOffsetX) {
        derivedStateOf {
            val delta = movedIconOffsetX - originIconOffset.x

            if (abs(delta) > selectThreshold) {
                if (delta > 0) Action.DISMISS else Action.SNOOZE
            } else {
                null
            }
        }
    }

    val infiniteIconFraction by fractionInfititeTransition(durationMillis = 200)

    Box(
        modifier = modifier.onSizeChanged {
            size = it
        }
    ) {
        Icon(
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    val scale = lerp(1f, 1.3f, infiniteIconFraction)
                    val rotation = lerp(-10f, 10f, infiniteIconFraction)

                    alpha = 1f - selectionFraction

                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation

                    translationX = movedIconOffsetX
                    translationY = originIconOffset.y
                }
                .draggable(
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        if (selectionFraction >= 0.5f) {
                            onActionSelected(selectionCandidate!!)
                        } else {
                            movedIconOffsetX = originIconOffset.x
                        }
                    },
                    state = rememberDraggableState(onDelta = { delta ->
                        val min = size.width * 0.25f - halfIconSizePx
                        val max = size.width * 0.75f - halfIconSizePx

                        movedIconOffsetX = (movedIconOffsetX + delta).coerceIn(min, max)
                    })
                ),
            imageVector = Icons.Filled.Alarm,
            contentDescription = null
        )

        Row(modifier = Modifier.fillMaxSize()) {
            ActionPickerChoice(
                textRes = R.string.snooze,
                if (selectionCandidate == Action.SNOOZE) selectionFraction else 0f
            )
            ActionPickerChoice(
                textRes = R.string.dismiss,
                if (selectionCandidate == Action.DISMISS) selectionFraction else 0f
            )
        }
    }
}

@Composable
private fun ActionAppliedScreen(alpha: Float, text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
    ) {
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = text,
            style = MaterialTheme.typography.h5
        )
    }
}

@Composable
private fun AlarmDismissedScreen(alpha: Float) {
    ActionAppliedScreen(alpha, text = stringResource(R.string.alarm_activity_apply_dismiss))
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AlarmSnoozedScreen(alpha: Float, snoozeDuration: Int) {
    ActionAppliedScreen(
        alpha,
        text = stringResource(
            id = R.string.alarm_activity_apply_snooze,
            buildString {
                append(snoozeDuration)
                append(' ')
                append(
                    pluralStringResource(
                        id = io.pelmenstar.onealarm.shared.R.plurals.minute,
                        count = snoozeDuration
                    )
                )
            }
        )
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AlarmScreen(
    viewModel: AlarmScreenViewModel = hiltViewModel(),
    dismiss: () -> Unit,
    snooze: () -> Unit,
    finishActivity: () -> Unit,
    alarmId: Long,
) {
    AppTheme(darkTheme = true) {
        val background = MaterialTheme.colors.background

        LaunchedEffect(Unit) {
            val silenceAfterMinutes = viewModel.silenceAfterMinutes.firstOrNull()

            if (silenceAfterMinutes != null) {
                delay((silenceAfterMinutes * MILLIS_IN_MINUTE).toLong())

                dismiss()
                finishActivity()
            }
        }

        var selectedAction by rememberSaveable {
            mutableStateOf<Action?>(null)
        }

        GlobalAlarmStateObserver { changedId, newState ->
            if (changedId == alarmId) {
                when (newState) {
                    AlarmState.DISMISSED -> {
                        selectedAction = Action.DISMISS
                    }
                    AlarmState.SNOOZED -> {
                        selectedAction = Action.SNOOZE
                    }
                    else -> {}
                }
            }
        }

        val appliedActionScreenAlpha by animateFloatAsState(
            targetValue = if (selectedAction != null) 1f else 0f,
            animationSpec = tween(durationMillis = 300)
        )

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = background
        ) {
            AnimatedVisibility(
                visible = selectedAction != null,
                enter = scaleIn(spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessMediumLow))
            ) {
                LaunchedEffect(Unit) {
                    delay(3000)

                    finishActivity()
                }

                when (selectedAction) {
                    Action.DISMISS -> {
                        AlarmDismissedScreen(appliedActionScreenAlpha)
                    }
                    Action.SNOOZE -> {
                        val snoozeDuration by viewModel.snoozeDurationMinutes.collectAsState(0)
                        if (snoozeDuration != 0) {
                            AlarmSnoozedScreen(appliedActionScreenAlpha, snoozeDuration)
                        }
                    }
                    else -> {}
                }
            }

            AnimatedVisibility(
                visible = selectedAction == null,
                exit = scaleOut(tween(durationMillis = 400))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 20.dp)
                ) {
                    Text(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        text = stringResource(R.string.alarm),
                        style = MaterialTheme.typography.h4
                    )

                    TextClock(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 20.dp),
                        style = MaterialTheme.typography.h2
                    )

                    ActionPicker(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.5f)
                            .padding(bottom = 20.dp)
                            .height(200.dp),
                        onActionSelected = {
                            selectedAction = it

                            viewModel.run {
                                when (it) {
                                    Action.DISMISS -> dismiss()
                                    Action.SNOOZE -> snooze()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}