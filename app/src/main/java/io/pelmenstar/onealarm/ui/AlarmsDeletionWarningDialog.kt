package io.pelmenstar.onealarm.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.pelmenstar.onealarm.AlarmsDeletionReason
import io.pelmenstar.onealarm.R
import io.pelmenstar.onealarm.ui.theme.dialogContainer

@Composable
fun AlarmsDeletionWarningDialog(
    reason: Int,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        val reasonStr = when (reason) {
            AlarmsDeletionReason.DEVICE_OFF -> stringResource(R.string.device_off_deletion_reason)
            else -> ""
        }

        Column(
            modifier = Modifier
                .dialogContainer(MaterialTheme.colors.surface, MaterialTheme.shapes.large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
               imageVector = Icons.Rounded.WarningAmber,
               contentDescription = null
            )

            Text(
                text = stringResource(R.string.alarms_deletion_info, reasonStr)
            )

            Button(
                modifier = Modifier.padding(top = 7.dp),
                onClick = onDismiss
            ) {
                Text(text = stringResource(R.string.ok))
            }
        }

    }
}