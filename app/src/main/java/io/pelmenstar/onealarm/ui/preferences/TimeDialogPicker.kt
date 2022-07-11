package io.pelmenstar.onealarm.ui.preferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.pelmenstar.circularTimePicker.compose.CircularTimePicker
import io.pelmenstar.circularTimePicker.compose.rememberCircularPickerState
import io.pelmenstar.onealarm.R
import io.pelmenstar.onealarm.ui.theme.dialogContainer

@Composable
fun TimeDialogPicker(
    minutes: Int,
    onDismiss: () -> Unit,
    onAccept: (minutes: Int) -> Unit,
    validateValue: ((minutes: Int) -> Boolean)? = null,
    maxHours: Int = 1,
) {
    var selectedTotalMinutes by rememberSaveable {
        mutableStateOf(minutes)
    }

    val timePickerState = rememberCircularPickerState(totalMinutes = minutes, maxHours = maxHours)

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.dialogContainer(
                MaterialTheme.colors.surface,
                MaterialTheme.shapes.large
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularTimePicker(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(horizontal = 10.dp),
                state = timePickerState,
                onTimeChanged = { selectedTotalMinutes = it }
            )

            Button(
                enabled = validateValue?.invoke(selectedTotalMinutes) ?: true,
                onClick = {
                    onDismiss()
                    onAccept(selectedTotalMinutes)
                }
            ) {
                Text(
                    text = stringResource(R.string.ok)
                )
            }
        }
    }
}