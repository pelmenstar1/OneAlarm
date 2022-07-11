@file:Suppress("NOTHING_TO_INLINE")

package io.pelmenstar.onealarm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

typealias OnAlarmStateChanged = (alarmId: Long, newState: AlarmState) -> Unit

object GlobalAlarmStateManager {
    private val lock = Any()
    private val onStateChangedListeners = mutableListOf<OnAlarmStateChanged>()

    fun addListener(listener: OnAlarmStateChanged) {
        synchronized(lock) {
            onStateChangedListeners.add(listener)
        }
    }

    fun removeListener(listener: OnAlarmStateChanged) {
        synchronized(lock) {
            onStateChangedListeners.remove(listener)
        }
    }

    fun notify(alarmId: Long, newState: AlarmState) {
        synchronized(lock) {
            val listeners = onStateChangedListeners

            for(i in listeners.indices) {
                listeners[i](alarmId, newState)
            }
        }
    }
}

@Composable
inline fun GlobalAlarmStateObserver(noinline onStateChanged: OnAlarmStateChanged) {
    DisposableEffect(onStateChanged) {
        GlobalAlarmStateManager.addListener(onStateChanged)

        onDispose {
            GlobalAlarmStateManager.removeListener(onStateChanged)
        }
    }
}