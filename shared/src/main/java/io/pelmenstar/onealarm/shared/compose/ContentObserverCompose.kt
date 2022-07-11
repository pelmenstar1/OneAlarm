package io.pelmenstar.onealarm.shared.compose

import android.database.ContentObserver
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Registers a [ContentObserver] which immediatly calls [onChange] lambda when resource specified by [Uri] is changed.
 *
 * When the function leaves the composition, the [ContentObserver] is unregistered automatically.
 */
@Composable
fun ContentObserver(uri: Uri, onChange: () -> Unit) {
    val context = LocalContext.current

    DisposableEffect(context) {
        val resolver = context.contentResolver
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) { onChange() }
        }

        resolver.registerContentObserver(uri, true, observer)

        onDispose {
            resolver.unregisterContentObserver(observer)
        }
    }
}