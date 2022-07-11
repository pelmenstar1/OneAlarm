package io.pelmenstar.onealarm.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.pelmenstar.onealarm.R
import io.pelmenstar.onealarm.shared.compose.annotatedStingResource
import io.pelmenstar.onealarm.ui.theme.dialogContainer

@Composable
@RequiresApi(31)
fun ManageAlarmExactPermissionDialog(onDismiss: (neverShownAgain: Boolean) -> Unit = {}) {
    val context = LocalContext.current

    var isNeverShownAgainChecked by remember {
        mutableStateOf(false)
    }

    Dialog(
        onDismissRequest = {
            onDismiss(isNeverShownAgainChecked)
        }
    ) {
        Column(
            modifier = Modifier.dialogContainer(MaterialTheme.colors.surface, MaterialTheme.shapes.large)
        ) {
            Icon(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(40.dp),
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = null
            )

            Text(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 5.dp),
                text = annotatedStingResource(R.string.alarm_exact_permission_importance),
            )

            Row(
                modifier = Modifier.padding(top = 5.dp)
            ) {
                val interactionSource = remember {
                    MutableInteractionSource()
                }

                Checkbox(
                    modifier = Modifier,
                    interactionSource = interactionSource,
                    checked = isNeverShownAgainChecked,
                    onCheckedChange = { isNeverShownAgainChecked = it }
                )

                Text(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .clickable(interactionSource, null) {
                            isNeverShownAgainChecked = !isNeverShownAgainChecked
                        },
                    text = stringResource(R.string.never_show_again)
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 5.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    modifier = Modifier,
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        )
                    }
                ) {
                    Text(text = stringResource(R.string.go_to_settings))
                }

                OutlinedButton(
                    modifier = Modifier.padding(start = 5.dp),
                    onClick = {
                        onDismiss(isNeverShownAgainChecked)
                    }
                ) {
                    Text(text = stringResource(R.string.dismiss))
                }
            }
        }
    }
}