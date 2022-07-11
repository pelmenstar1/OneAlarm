package io.pelmenstar.onealarm.shared.compose

import android.net.Uri
import android.provider.Settings

/**
 * Convenience function for Settings.System.getUriFor([id])
 */
fun settingsUri(id: String): Uri = Settings.System.getUriFor(id)