package io.pelmenstar.onealarm.shared.compose

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

private inline fun intentFilter(block: IntentFilter.() -> Unit) = IntentFilter().apply(block)

fun intentFilter(action: String) = intentFilter { addAction(action) }
fun intentFilter(action1: String, action2: String) = intentFilter {
    addAction(action1)
    addAction(action2)
}

fun intentFilter(action1: String, action2: String, action3: String) = intentFilter {
    addAction(action1)
    addAction(action2)
    addAction(action3)
}

fun intentFilter(vararg actions: String) = intentFilter {
    actions.forEach(::addAction)
}

/**
 * Composable function which registers a [BroadcastReceiver] with specified filter.
 * The receiver will be unregistered when the function leaves the composition.
 */
@Composable
fun BroadcastReceiver(
    filter: IntentFilter,
    onReceived: (action: String?, extras: Bundle?) -> Unit,
) {
    val context = LocalContext.current

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                onReceived(intent.action, intent.extras)
            }
        }

        context.registerReceiver(receiver, filter)

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }
}