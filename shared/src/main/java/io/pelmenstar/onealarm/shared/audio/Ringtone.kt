package io.pelmenstar.onealarm.shared.audio

import android.media.Ringtone
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import java.lang.reflect.Method

private object ReflectionMethodsHolder {
    @JvmField
    val setVolumeMethod: Method?

    @JvmField
    val setLoopingMethod: Method?

    init {
        setVolumeMethod = getRingtoneMethod("setVolume", Float::class.javaPrimitiveType)
        setLoopingMethod = getRingtoneMethod("setLooping", Boolean::class.javaPrimitiveType)
    }

    private fun getRingtoneMethod(name: String, argClass: Class<*>?): Method? {
        return try {
            Ringtone::class.java.getDeclaredMethod(name, argClass)
        } catch (e: Throwable) {
            null
        }
    }
}

/**
 * Sets volume of [Ringtone] in compatible with all supported API levels way.
 */
fun Ringtone.setVolumeCompat(value: Float): Boolean {
    return invoke28ApiCompat({ volume = value }, { setVolumeMethod }, value)
}

/**
 * Sets whether [Ringtone] is looping or not in compatible with all supported API levels way.
 */
fun Ringtone.setLoopingCompat(value: Boolean): Boolean {
    return invoke28ApiCompat({ isLooping = value }, { setLoopingMethod }, value)
}

@ChecksSdkIntAtLeast(api = 28, lambda = 0)
private inline fun Ringtone.invoke28ApiCompat(
    newMethod: () -> Unit,
    lazyMethod: ReflectionMethodsHolder.() -> Method?,
    arg: Any
): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= 28) {
            newMethod()

            true
        } else {
            val method = ReflectionMethodsHolder.lazyMethod()

            if (method != null) {
                method.invoke(this, arg)

                true
            } else {
                false
            }
        }
    } catch (e: Exception) {
        false
    }
}