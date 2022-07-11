package io.pelmenstar.onealarm.shared.audio

import android.media.MediaPlayer
import kotlinx.coroutines.suspendCancellableCoroutine

private val UNIT_RESULT = Result.success(Unit)

/**
 * Does the same as [MediaPlayer.prepareAsync] but 'suspendable' way.
 */
suspend fun MediaPlayer.prepareAsyncSuspend() {
    suspendCancellableCoroutine<Unit> { cont ->
        cont.invokeOnCancellation {
            setOnPreparedListener(null)
        }

        setOnPreparedListener {
            cont.resumeWith(UNIT_RESULT)
            setOnPreparedListener(null)
        }

        prepareAsync()
    }
}