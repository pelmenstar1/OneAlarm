package io.pelmenstar.onealarm.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
@SuppressLint("StaticFieldLeak")
class DefaultAlarmKlaxon(context: Context): AlarmKlaxon {
    private var isStarted = false

    private val alarmPlayer = AlarmCompatRingtonePlayer(context)
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= 31) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager

        manager.defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private var playJob: Job? = null

    override fun start() {
        if(Build.VERSION.SDK_INT >= 26) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            vibrator.vibrate(VibrationEffect.createWaveform(VIBRATE_PATTERN, 0), attrs)
        } else {
            vibrator.vibrate(VIBRATE_PATTERN, 0)
        }

        playJob = scope.launch {
            alarmPlayer.play()
        }
    }

    override fun stop() {
        alarmPlayer.stop()
        playJob?.cancel()

        vibrator.cancel()
    }

    companion object {
        private val VIBRATE_PATTERN = longArrayOf(500, 500)

        private val scope = CoroutineScope(Dispatchers.Default)
    }
}