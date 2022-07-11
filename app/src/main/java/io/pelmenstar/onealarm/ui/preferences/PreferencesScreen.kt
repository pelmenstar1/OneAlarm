@file:OptIn(ExperimentalComposeUiApi::class)
@file:Suppress("NOTHING_TO_INLINE")

package io.pelmenstar.onealarm.ui.preferences

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import dagger.hilt.android.lifecycle.HiltViewModel
import io.pelmenstar.onealarm.AppPreferences
import io.pelmenstar.onealarm.BuildConfig
import io.pelmenstar.onealarm.R
import io.pelmenstar.onealarm.setTo
import io.pelmenstar.onealarm.shared.time.PackedHourMinute
import io.pelmenstar.onealarm.shared.withAdded
import io.pelmenstar.onealarm.shared.withRemoved
import io.pelmenstar.onealarm.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PreferencesScreenViewModel @Inject constructor(
    private val prefs: DataStore<Preferences>,
) : ViewModel() {
    fun <T> getPreferenceFlow(entry: AppPreferences.Entry<T>) = entry.toFlow(prefs)

    suspend fun <T> setPreferenceValue(entry: AppPreferences.Entry<T>, value: T) {
        entry.setTo(prefs, value)
    }
}

@Composable
fun PreferencesScreen(
    viewModel: PreferencesScreenViewModel = hiltViewModel(),
    navController: NavHostController
) {
    AppTheme(darkTheme = false) {
        Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                navController.popBackStack()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBack,
                                contentDescription = null
                            )
                        }

                        Text(
                            modifier = Modifier.padding(start = 5.dp),
                            text = stringResource(R.string.preferences),
                            style = MaterialTheme.typography.h5
                        )
                    }

                    var isTimeDialogShown by rememberSaveable {
                        mutableStateOf(false)
                    }

                    var timeDialogInitialMinutes by rememberSaveable {
                        mutableStateOf(0)
                    }

                    var timeDialogPrefEntry by rememberSaveable {
                        mutableStateOf<AppPreferences.Entry<Int>?>(null)
                    }

                    val scope = rememberCoroutineScope()

                    val showTimeDialog = remember {
                        { entry: AppPreferences.Entry<Int>, initialValue: Int ->
                            isTimeDialogShown = true
                            timeDialogInitialMinutes = initialValue
                            timeDialogPrefEntry = entry
                        }
                    }

                    val getFlow = remember<(AppPreferences.Entry<Int>) -> Flow<Int>> {
                        {
                            viewModel.getPreferenceFlow(it)
                        }
                    }

                    if (isTimeDialogShown) {
                        TimeDialogPicker(
                            timeDialogInitialMinutes,
                            onDismiss = {
                                isTimeDialogShown = false
                            },
                            onAccept = { time ->
                                scope.launch {
                                    viewModel.setPreferenceValue(
                                        timeDialogPrefEntry!!,
                                        time
                                    )
                                }
                            }
                        )
                    }

                    MinutesPreferenceElement(
                        modifier = Modifier.padding(top = 5.dp),
                        nameId = R.string.snooze_duration,
                        descriptionId = R.string.snooze_duration_desc,
                        prefEntry = AppPreferences.snoozeDuration,
                        getFlow,
                        showTimeDialog,
                    )

                    PreferenceDivier()

                    MinutesPreferenceElement(
                        nameId = R.string.silence_after_duration,
                        descriptionId = R.string.silence_after_duration_desc,
                        prefEntry = AppPreferences.silenceAfter,
                        getFlow = getFlow,
                        showDialog = showTimeDialog,
                    )

                    PreferenceDivier()

                    DropDownPreferenceElement(
                        nameId = R.string.volume_button_behaviour,
                        descriptionId = R.string.volume_button_behaviour_desc,
                        itemsId = R.array.volume_button_behaviour_items,
                        prefEntry = AppPreferences.volumeButtonBehaviour,
                        getFlow = getFlow,
                        setValue = viewModel::setPreferenceValue
                    )

                    PreferenceDivier()

                    EditMostUsedAlarmsPanel(
                        getFlow = { viewModel.getPreferenceFlow(AppPreferences.mostUsedAlarms) },
                        setValue = {
                            scope.launch {
                                viewModel.setPreferenceValue(AppPreferences.mostUsedAlarms, it)
                            }
                        }
                    )
                }

                Text(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 5.dp, bottom = 5.dp),
                    text = "${BuildConfig.BUILD_TYPE}-${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}

@Composable
private inline fun PreferenceDivier() {
    Divider(modifier = Modifier.padding(vertical = 5.dp))
}

@Composable
private fun EditMostUsedAlarmsPanel(
    getFlow: () -> Flow<Array<PackedHourMinute>>,
    setValue: suspend (Array<PackedHourMinute>) -> Unit
) {
    val mostUsedAlarms by getFlow().collectAsState(initial = emptyArray())

    var isMinuteDialogPickerShown by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope { Dispatchers.IO }

    if (isMinuteDialogPickerShown) {
        TimeDialogPicker(
            minutes = 0,
            onDismiss = { isMinuteDialogPickerShown = false },
            onAccept = { totalMinutes ->
                val newArray = mostUsedAlarms.withAdded(PackedHourMinute(totalMinutes))

                scope.launch {
                    setValue(newArray)
                }
            },
            validateValue = { value ->
                value > 0 && mostUsedAlarms.indexOfFirst { it.totalMinutes == value } < 0
            },
            maxHours = 4
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 5.dp, bottom = 5.dp),
            text = stringResource(R.string.most_used_alarms),
            style = MaterialTheme.typography.h6
        )

        mostUsedAlarms.forEach { hm ->
            key(hm.totalMinutes) {
                Row {
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                            .align(Alignment.CenterVertically),
                        text = hm.toString()
                    )

                    IconButton(onClick = {
                        val newArray = mostUsedAlarms.withRemoved(hm)

                        scope.launch {
                            setValue(newArray)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Remove,
                            contentDescription = stringResource(R.string.remove)
                        )
                    }
                }
                Divider()
            }
        }


        if (mostUsedAlarms.size < 4) {
            IconButton(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = {
                    isMinuteDialogPickerShown = true
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.add)
                )
            }
        }
    }
}

@Composable
private fun DropDownPreferenceElement(
    modifier: Modifier = Modifier,
    @StringRes nameId: Int,
    @StringRes descriptionId: Int,
    @ArrayRes itemsId: Int,
    prefEntry: AppPreferences.Entry<Int>,
    getFlow: (AppPreferences.Entry<Int>) -> Flow<Int>,
    setValue: suspend (AppPreferences.Entry<Int>, Int) -> Unit,
) {
    val items = stringArrayResource(itemsId)
    val interactionSource = remember {
        MutableInteractionSource()
    }

    var isExpanded by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()

    PreferenceElement(
        modifier = modifier,
        nameId = nameId,
        descriptionId = descriptionId,
        prefEntry = prefEntry,
        getFlow = getFlow
    ) { itemIndex ->
        val selectedItem = items[itemIndex]

        Column {
            Text(
                modifier = Modifier
                    .padding(horizontal = 5.dp)
                    .clickable(interactionSource, indication = null) {
                        isExpanded = true
                    },
                text = selectedItem
            )

            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = {
                    isExpanded = false
                }
            ) {
                Column {
                    for (i in items.indices) {
                        key(i) {
                            Text(
                                modifier = Modifier
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                                    .fillMaxWidth()
                                    .clickable {
                                        isExpanded = false

                                        scope.launch {
                                            setValue(prefEntry, i)
                                        }
                                    },
                                text = items[i],
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private inline fun MinutesPreferenceElement(
    modifier: Modifier = Modifier,
    @StringRes nameId: Int,
    @StringRes descriptionId: Int,
    prefEntry: AppPreferences.Entry<Int>,
    noinline getFlow: (AppPreferences.Entry<Int>) -> Flow<Int>,
    crossinline showDialog: (AppPreferences.Entry<Int>, Int) -> Unit,
) {
    PreferenceElement(
        modifier = modifier,
        nameId = nameId,
        descriptionId = descriptionId,
        prefEntry = prefEntry,
        onClick = { showDialog(prefEntry, it) },
        getFlow = getFlow
    ) {
        Text(text = buildString {
            append(it)
            append(' ')
            append(pluralStringResource(io.pelmenstar.onealarm.shared.R.plurals.minute, it))
        })
    }
}

@Composable
private fun <T> PreferenceElement(
    modifier: Modifier = Modifier,
    @StringRes nameId: Int,
    @StringRes descriptionId: Int,
    prefEntry: AppPreferences.Entry<T>,
    onClick: ((T) -> Unit)? = null,
    getFlow: (AppPreferences.Entry<T>) -> Flow<T>,
    content: @Composable RowScope.(value: T) -> Unit
) {
    val value by getFlow(prefEntry).collectAsState(initial = prefEntry.defaultValue)
    val interactionSource = remember {
        MutableInteractionSource()
    }

    Column(
        modifier
            .fillMaxWidth()
            .clickable(
                interactionSource,
                indication = null,
                enabled = onClick != null
            ) {
                onClick?.invoke(value)
            }
            .padding(bottom = 5.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp)
        ) {
            Text(
                modifier = Modifier
                    .weight(1f, fill = true),
                text = stringResource(nameId)
            )

            content(value)
        }

        Row(
            modifier = Modifier
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null
            )

            Text(
                modifier = Modifier
                    .padding(start = 10.dp),
                text = stringResource(descriptionId),
                style = MaterialTheme.typography.caption
            )
        }
    }
}