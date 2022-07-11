package io.pelmenstar.onealarm.shared.android

import android.content.res.Configuration
import android.os.Build
import java.util.*

/**
 * Retrieves information about [Locale] from [Configuration] in compatible with all supported API levels way.
 */
@Suppress("DEPRECATION")
val Configuration.localeCompat: Locale
    get() = if(Build.VERSION.SDK_INT >= 24) locales[0] else locale