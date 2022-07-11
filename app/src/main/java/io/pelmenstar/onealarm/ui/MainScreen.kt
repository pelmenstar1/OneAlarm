@file:Suppress("NOTHING_TO_INLINE")

package io.pelmenstar.onealarm.ui

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.lifecycle.HiltViewModel
import io.pelmenstar.circularTimePicker.compose.CircularTimePicker
import io.pelmenstar.circularTimePicker.compose.rememberCircularPickerState
import io.pelmenstar.onealarm.*
import io.pelmenstar.onealarm.R
import io.pelmenstar.onealarm.data.*
import io.pelmenstar.onealarm.shared.android.localeCompat
import io.pelmenstar.onealarm.shared.compose.*
import io.pelmenstar.onealarm.shared.time.*
import io.pelmenstar.onealarm.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

private const val TAG = "MainScreen"

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val alarmsManager: InternalAlarmsManager,
    appDatabase: AppDatabase,
    private val preferences: DataStore<Preferences>
) : ViewModel() {
    private val alarmsDao: AlarmsDao = appDatabase.alarmsDao()

    val alarmsDeletionReason =
        AppPreferences.alarmsDeletionReason.toFlow(preferences)

    val isExactAlarmsDialogNeverShowAgain =
        AppPreferences.exactAlarmsNeverShowAgain.toFlow(preferences)

    val mostUsedAlarms =
        AppPreferences.mostUsedAlarms.toFlow(preferences)

    val alarms = MutableStateFlow<Array<AlarmEntry>>(emptyArray())

    init {
        viewModelScope.launch {
            alarms.value = alarmsDao.getAlarms()
        }
    }

    suspend fun schedule(totalMinutes: Int, alarmMode: AlarmMode) {
        AlarmHelper.scheduleAndSave(alarmsDao, alarmsManager, totalMinutes, alarmMode)

        refreshData()
    }

    suspend fun cancel(id: Long) {
        AlarmHelper.cancelAndDelete(alarmsDao, alarmsManager, id)

        refreshData()
    }

    suspend fun refreshData() {
        alarms.value = alarmsDao.getAlarms()
    }

    suspend fun setExactAlarmsDialogNeverShowAgain(value: Boolean) {
        AppPreferences.exactAlarmsNeverShowAgain.setTo(preferences, value)
    }

    suspend fun clearDeletionReason() {
        AppPreferences.alarmsDeletionReason.setTo(preferences, 0)
    }
}

@Composable
fun MainScreen(viewModel: MainScreenViewModel = hiltViewModel(), navController: NavHostController) {
    AppTheme(darkTheme = false) {
        val scaffoldState = rememberScaffoldState()

        val systemUiController = rememberSystemUiController()
        val useDarkIcons = MaterialTheme.colors.isLight
        val background = MaterialTheme.colors.background
        val surfaceColor = MaterialTheme.colors.surface

        val scope = rememberCoroutineScope()

        GlobalAlarmStateObserver { _, newState ->
            if (newState != AlarmState.SCHEDULED) {
                scope.launch {
                    viewModel.refreshData()
                }
            }
        }

        SideEffect {
            systemUiController.run {
                setStatusBarColor(background, useDarkIcons)
                setNavigationBarColor(surfaceColor, useDarkIcons)
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            scaffoldState = scaffoldState,
            snackbarHost = { MessageTypeSnackbar(it) }
        ) { padding ->
            val isExactAlarmsDialogNeverShowAgain by viewModel.isExactAlarmsDialogNeverShowAgain.collectAsState(
                initial = null
            )

            val alarmsDeletionReason by viewModel.alarmsDeletionReason.collectAsState(
                initial = null
            )

            val mostUsedAlarms by viewModel.mostUsedAlarms.collectAsState(initial = null)

            WarningDialogs(
                isExactAlarmsDialogNeverShowAgain,
                alarmsDeletionReason,
                viewModel::setExactAlarmsDialogNeverShowAgain,
                viewModel::clearDeletionReason
            )

            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 7.dp),
                ) {
                    val alarms by viewModel.alarms.collectAsState(emptyArray())

                    ScheduleAlarmPanel(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        scaffoldState.snackbarHostState,
                        mostUsedAlarms?.let { StableArray.wrap(it) },
                        viewModel::schedule
                    )
                    ScheduledAlarmsList(
                        scaffoldState.snackbarHostState,
                        StableArray.wrap(alarms),
                        viewModel::cancel,
                    )
                }

                Icon(
                    modifier = Modifier
                        .padding(top = 10.dp, end = 10.dp)
                        .align(Alignment.TopEnd)
                        .clickable(role = Role.Button) {
                            navController.navigate("preferences")
                        },
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private inline fun WarningDialogs(
    isExactAlarmsDialogNeverShowAgain: Boolean?,
    alarmsDeletionReason: Int?,
    noinline setExactAlarmsDialogNeverShowAgain: suspend (value: Boolean) -> Unit,
    noinline clearDeletionReason: suspend () -> Unit
) {
    var canShowAlarmsDeletionDialog by remember {
        mutableStateOf(Build.VERSION.SDK_INT < 31)
    }

    if (Build.VERSION.SDK_INT >= 31) {
        ManageAlarmExactPermissionDialogHandler(
            isExactAlarmsDialogNeverShowAgain,
            setExactAlarmsDialogNeverShowAgain,
            onSpareScreen = {
                canShowAlarmsDeletionDialog = true
            }
        )
    }

    if (canShowAlarmsDeletionDialog &&
        alarmsDeletionReason != null &&
        alarmsDeletionReason != AlarmsDeletionReason.NONE
    ) {
        var isShown by remember {
            mutableStateOf(true)
        }

        val scope = rememberCoroutineScope()

        if (isShown) {
            AlarmsDeletionWarningDialog(
                alarmsDeletionReason,
                onDismiss = {
                    isShown = false

                    scope.launch {
                        clearDeletionReason()
                    }
                }
            )
        }
    }
}

@Composable
@RequiresApi(31)
private fun ManageAlarmExactPermissionDialogHandler(
    isNeverShowAgain: Boolean?,
    setNeverShownAgain: suspend (value: Boolean) -> Unit,
    onSpareScreen: () -> Unit
) {
    var isManageAlarmExactPermissionDialogShown by remember {
        mutableStateOf(true)
    }

    val scope = rememberCoroutineScope()

    if (isManageAlarmExactPermissionDialogShown) {
        if (isNeverShowAgain == true) {
            onSpareScreen()
        } else if (isNeverShowAgain == false) {
            val canSheduleExactAlarms by canSheduleExactAlarmsObserver()

            if (!canSheduleExactAlarms) {
                ManageAlarmExactPermissionDialog(
                    onDismiss = { neverShowAgain ->
                        isManageAlarmExactPermissionDialogShown = false

                        scope.launch {
                            setNeverShownAgain(neverShowAgain)
                        }
                    }
                )
            } else {
                onSpareScreen()
            }
        }
    } else {
        onSpareScreen()
    }
}

@Composable
private fun MostUsedAlarmButton(
    modifier: Modifier = Modifier,
    time: PackedHourMinute,
    onClick: () -> Unit
) {
    OutlinedButton(
        modifier = modifier.padding(5.dp),
        onClick = onClick
    ) {
        Text(
            modifier = Modifier.padding(5.dp),
            text = time.toString()
        )
    }
}

@Composable
private fun MostUsedAlarmsRow(
    modifier: Modifier = Modifier,
    mostUsedAlarms: StableArray<PackedHourMinute>,
    onButtonClick: (PackedHourMinute) -> Unit
) {
    Row(
        modifier = modifier
    ) {
        mostUsedAlarms.forEach {
            MostUsedAlarmButton(time = it) {
                onButtonClick(it)
            }
        }
    }
}

private fun AlarmMode.getMaxHoursForScheduleAlarmPicker(): Int {
    return if(this == AlarmMode.FROM_NOW) 4 else 24
}

private fun isScheduleAlarmEnabled(totalMinutes: Int, alarmMode: AlarmMode): Boolean {
    return !(totalMinutes == 0 && alarmMode == AlarmMode.FROM_NOW)
}

@Composable
private fun ScheduleAlarmPanel(
    modifier: Modifier,
    snackbarHostState: SnackbarHostState,
    mostUsedAlarms: StableArray<PackedHourMinute>?,
    scheduleAlarm: suspend (Int, AlarmMode) -> Unit
) {
    var alarmMode by rememberSaveable { mutableStateOf(AlarmMode.FROM_NOW) }
    val scope = rememberCoroutineScope { Dispatchers.Default }

    var isScheduleEnabled by remember {
        mutableStateOf(true)
    }

    val configuration = LocalConfiguration.current

    val timePickerSize = remember(configuration) {
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp

        if (screenWidth < screenHeight) {
            (screenWidth - 10).dp
        } else {
            (screenHeight - 50).dp
        }
    }

    val timePickerState = rememberCircularPickerState(totalMinutes = 15, maxHours = 4)

    var isTimePickerDragging by remember {
        mutableStateOf(false)
    }

    val sheduleButtonScale by animateFloatAsState(targetValue = if (isTimePickerDragging) 0f else 1f)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AlarmModeSwitch(
            modifier = Modifier.fillMaxWidth(0.5f),
            mode = alarmMode,
            onModeChanged = {
                alarmMode = it

                isScheduleEnabled = isScheduleAlarmEnabled(timePickerState.getTime(), it)
                timePickerState.setMaxHours(it.getMaxHoursForScheduleAlarmPicker())
            }
        )

        Box(
            Modifier
                .size(timePickerSize, timePickerSize)
                .padding(top = 10.dp)
        ) {
            CircularTimePicker(
                modifier = Modifier.matchParentSize(),
                state = timePickerState,
                onTimeChanged = { totalMinutes ->
                    isScheduleEnabled = isScheduleAlarmEnabled(totalMinutes, alarmMode)
                },
                onDragStarted = {
                    isTimePickerDragging = true
                },
                onDragEnded = {
                    isTimePickerDragging = false
                }
            )

            OutlinedButton(
                modifier = Modifier
                    .padding(top = timePickerSize * 0.25f)
                    .align(Alignment.Center)
                    .scale(sheduleButtonScale),
                enabled = isScheduleEnabled,
                onClick = {
                    scope.launch {
                        var msgType = SnackbarMessageType.PLAIN
                        var msgId = 0

                        try {
                            scheduleAlarm(timePickerState.getTime(), alarmMode)

                            msgType = SnackbarMessageType.SUCCESS
                            msgId = R.string.alarm_sucessfully_set
                        } catch (_: AlarmAlreadyScheduledException) {
                            msgType = SnackbarMessageType.ERROR
                            msgId = R.string.alarm_is_already_set
                        } catch (_: Throwable) {
                            msgType = SnackbarMessageType.ERROR
                            msgId = R.string.error_happened
                        } finally {
                            snackbarHostState.showSnackbar(createSnackbarMessage(msgType, msgId))
                        }
                    }
                }
            ) {
                Text(
                    text = stringResource(R.string.schedule_alarm).uppercase(configuration.localeCompat),
                )
            }
        }

        if (mostUsedAlarms != null) {
            MostUsedAlarmsRow(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                mostUsedAlarms = mostUsedAlarms,
                onButtonClick = { hm ->
                    timePickerState.setTime(hm.totalMinutes)

                    alarmMode = AlarmMode.FROM_NOW
                    isScheduleEnabled = isScheduleAlarmEnabled(hm.totalMinutes, AlarmMode.FROM_NOW)
                }
            )
        }
    }
}

@Composable
private fun AlarmListItem(
    entry: AlarmEntry,
    nowEpochSeconds: Long,
    hourFormat: HourFormat,
    timeZone: TimeZone,
    prettyDateFormatter: PrettyDateFormatter,
    cancelAlarm: suspend (Long) -> Unit,
    onCancelError: suspend (Exception) -> Unit
) {
    val scope = rememberCoroutineScope { Dispatchers.IO }

    val configuration = LocalConfiguration.current

    val fromNowPattern = stringResource(R.string.alarm_list_item_from_now)
    val fromNowText by remember(
        fromNowPattern,
        nowEpochSeconds,
        configuration
    ) {
        derivedStateOf {
            val timeDiff = entry.epochSeconds - nowEpochSeconds

            String.format(
                configuration.localeCompat,
                fromNowPattern,
                prettyDateFormatter.formatSecondsDifference(timeDiff)
            )
        }
    }

    Row(
        modifier = Modifier
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .weight(1f)
                .padding(start = 5.dp)
        ) {
            val todayEpochDay = nowEpochSeconds / SECONDS_IN_DAY

            DateTimeText(
                dateTime = PackedDateTime.ofEpochSecond(entry.epochSeconds),
                hourFormat = hourFormat,
                todayEpochDay = todayEpochDay,
                timeZone = timeZone,
                formatter = prettyDateFormatter
            )

            Text(
                text = fromNowText,
                fontStyle = FontStyle.Italic
            )
        }

        IconButton(
            modifier = Modifier
                .align(Alignment.CenterVertically),
            onClick = {
                scope.launch {
                    try {
                        cancelAlarm(entry.id)
                    } catch (e: Exception) {
                        onCancelError(e)
                    }
                }
            }
        ) {
            Icon(
                imageVector = Icons.Filled.Clear,
                contentDescription = stringResource(R.string.cancel)
            )
        }
    }
}

@Composable
private fun ColumnScope.ScheduledAlarmsList(
    snackbarHostState: SnackbarHostState,
    alarms: StableArray<AlarmEntry>,
    cancelAlarm: suspend (Long) -> Unit,
) {
    if (alarms.isNotEmpty()) {
        val context = LocalContext.current
        val configuration = LocalConfiguration.current

        val prettyDateFormatter by remember(context, configuration) {
            derivedStateOf { PrettyDateFormatter.create(context) }
        }

        val timeZone by defaultTimeZoneObserver()

        val nowEpochSeconds by epochSecondsUtcObserver(60 /* every minute */)
        val hourFormat by hourFormatObserver()

        val errorHappenedStr = stringResource(R.string.error_happened)

        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 30.dp, bottom = 10.dp),
            text = stringResource(R.string.scheduled_alarms),
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold
        )

        Column {
            alarms.forEachIndexed { i, alarm ->
                key(alarm.id) {
                    AlarmListItem(
                        alarm,
                        nowEpochSeconds,
                        hourFormat,
                        timeZone,
                        prettyDateFormatter,
                        cancelAlarm,
                        onCancelError = {
                            Log.e(TAG, "cancel alarm error", it)

                            snackbarHostState.showSnackbar(
                                createSnackbarMessage(SnackbarMessageType.ERROR, errorHappenedStr),
                                duration = SnackbarDuration.Long
                            )
                        }
                    )

                    if (i < alarms.size - 1) {
                        Divider()
                    }
                }
            }
        }
    }
}