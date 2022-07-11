package io.pelmenstar.onealarm.audio

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.AnyRes
import androidx.annotation.RequiresApi
import io.pelmenstar.onealarm.R
import io.pelmenstar.onealarm.shared.audio.prepareAsyncSuspend
import io.pelmenstar.onealarm.shared.audio.setLoopingCompat
import io.pelmenstar.onealarm.shared.audio.setVolumeCompat
import kotlin.math.pow

@Suppress("DEPRECATION")
class AlarmCompatRingtonePlayer(private val context: Context) {
    private val delegateLock = Any()

    @Volatile
    private var _playbackDelegate: Delegate? = null

    suspend fun play() {
        playbackDelegate.play(context)
    }

    fun stop() {
        playbackDelegate.stop()
    }

    private val playbackDelegate: Delegate
        get() {
            if (_playbackDelegate == null) {
                synchronized(delegateLock) {
                    if (_playbackDelegate == null) {
                        _playbackDelegate = if (Build.VERSION.SDK_INT >= 24) {
                            RingtoneDelegate()
                        } else {
                            MediaPlayerDelegate()
                        }
                    }
                }
            }

            return _playbackDelegate!!
        }

    private interface Delegate {
        /**
         * Starts playback using default ringtone for alarms.
         */
        suspend fun play(context: Context)

        /**
         * Stops playback.
         */
        fun stop()
    }

    private class MediaPlayerDelegate : Delegate {
        @Volatile
        private var audioManager: AudioManager? = null

        @Volatile
        private var mediaPlayer: MediaPlayer? = null

        @Volatile
        private var audioFocusRequest: AudioFocusRequest? = null

        private val lock = Any()

        override suspend fun play(context: Context) {
            val inTelephoneCall = isInTelephoneCall(context)

            val dataSource = if(isInTelephoneCall(context)) {
                getFallbackRingtoneUri(context)
            } else {
                Settings.System.DEFAULT_ALARM_ALERT_URI
            }

            val player = MediaPlayer()

            player.setOnErrorListener { _, _, _ ->
                Log.e(TAG, "Error occurred while playing ringtone")

                stop()
                true
            }

            synchronized(lock) {
                mediaPlayer = player

                if (audioManager == null) {
                    audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                }
            }

            try {
                player.setDataSource(context, dataSource)

                startPlayback(inTelephoneCall)
            } catch (t: Throwable) {
                Log.e(TAG, "Using the fallback ringtone, could not play default one", t)

                try {
                    player.reset()
                    player.setDataSource(context, getFallbackRingtoneUri(context))

                    startPlayback(inTelephoneCall)
                } catch (t2: Throwable) {
                    // We've gone so far and there's exception again. Something is really wrong.
                    Log.e(TAG, "Failed to play fallback ringtone", t2)
                }
            }
        }

        private suspend fun startPlayback(inTelephoneCall: Boolean) {
            val audioManager = audioManager!!
            val mediaPlayer = mediaPlayer!!

            if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) == 0) {
                return
            }

            val audioAttributes = createAudioAttributes()
            mediaPlayer.setAudioAttributes(audioAttributes)

            if (inTelephoneCall) {
                mediaPlayer.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME)
            }

            mediaPlayer.run {
                setAudioAttributes(audioAttributes)

                isLooping = true

                prepareAsyncSuspend()

                if (Build.VERSION.SDK_INT >= 26) {
                    val focusRequest = createAudioFocusRequest(audioAttributes)
                    audioFocusRequest = focusRequest

                    audioManager.requestAudioFocus(focusRequest)
                } else {
                    audioManager.requestAudioFocus(
                        null,
                        AudioManager.STREAM_ALARM,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                    )
                }

                start()
            }
        }

        override fun stop() {
            synchronized(lock) {
                mediaPlayer?.let {
                    it.stop()
                    it.release()

                    mediaPlayer = null
                }

                audioManager?.let {
                    if (Build.VERSION.SDK_INT >= 26) {
                        it.abandonAudioFocusRequest(audioFocusRequest!!)
                    } else {
                        it.abandonAudioFocus(null)
                    }
                }
            }
        }
    }

    @RequiresApi(24)
    private class RingtoneDelegate : Delegate {
        @Volatile
        private var audioManager: AudioManager? = null

        @Volatile
        private var ringtone: Ringtone? = null

        @Volatile
        private var audioFocusRequest: AudioFocusRequest? = null

        private val lock = Any()

        override suspend fun play(context: Context){
            if (audioManager == null) {
                audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            }

            val inTelephoneCall = isInTelephoneCall(context)
            val dataSourceUri = if (inTelephoneCall) {
                getFallbackRingtoneUri(context)
            } else {
                Settings.System.DEFAULT_ALARM_ALERT_URI!!
            }

            // Attempt to fetch the specified ringtone.
            var ringtone = RingtoneManager.getRingtone(context, dataSourceUri)

            if(ringtone != null) {
                val success = ringtone.setLoopingCompat(true)
                if(!success) {
                    ringtone = null
                }
            }

            // If no ringtone exists at this point there isn't much recourse.
            if (ringtone == null) {
                Log.i(TAG, "Unable to locate alarm ringtone, using internal fallback ringtone.")

                ringtone = RingtoneManager.getRingtone(context, getFallbackRingtoneUri(context))
            }

            this.ringtone = ringtone

            try {
                startPlayback(inTelephoneCall)
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to play fallback ringtone", t)
            }
        }

        private fun startPlayback(inTelephoneCall: Boolean) {
            val ringtone = ringtone
            val audioManager = audioManager

            require(ringtone != null && audioManager != null)

            val audioAttributes = createAudioAttributes()

            ringtone.audioAttributes = audioAttributes

            if (inTelephoneCall) {
                ringtone.setVolumeCompat(IN_CALL_VOLUME)
            }

            if (Build.VERSION.SDK_INT >= 26) {
                val focusRequest = createAudioFocusRequest(audioAttributes)
                audioFocusRequest = focusRequest

                audioManager.requestAudioFocus(focusRequest)
            } else {
                audioManager.requestAudioFocus(
                    null, AudioManager.STREAM_ALARM,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }

            ringtone.play()
        }

        override fun stop() {
            synchronized(lock) {
                ringtone?.let {
                    if (it.isPlaying) {
                        it.stop()
                    }

                    ringtone = null
                }

                audioManager?.let {
                    if (Build.VERSION.SDK_INT >= 26) {
                        it.abandonAudioFocusRequest(audioFocusRequest!!)
                    } else {
                        it.abandonAudioFocus(null)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "CompatRingtonePlayer"
        private const val IN_CALL_VOLUME = 0.125f

        private fun isInTelephoneCall(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= 23) {
                if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED) {
                    return false
                }
            }

            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val state = if (Build.VERSION.SDK_INT >= 31) {
                tm.callStateForSubscription
            } else {
                tm.callState
            }

            return state != TelephonyManager.CALL_STATE_IDLE
        }

        private fun createAudioAttributes(): AudioAttributes {
            return AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        }

        @RequiresApi(26)
        private fun createAudioFocusRequest(attrs: AudioAttributes): AudioFocusRequest {
            return AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attrs)
                .build()
        }

        private fun getFallbackRingtoneUri(context: Context): Uri {
            return Uri.Builder()
                .scheme(ContentResolver.SCHEME_FILE)
                .authority(context.packageName)
                .path(R.raw.alarm_expire.toString())
                .build()
        }
    }
}