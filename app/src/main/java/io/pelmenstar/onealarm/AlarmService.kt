package io.pelmenstar.onealarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.hilt.android.AndroidEntryPoint
import io.pelmenstar.onealarm.audio.AlarmKlaxon
import io.pelmenstar.onealarm.data.AppDatabase
import io.pelmenstar.onealarm.data.InternalAlarmsManager
import io.pelmenstar.onealarm.ui.AlarmActivity
import io.pelmenstar.onealarm.ui.cancelAlarmNotification
import io.pelmenstar.onealarm.ui.showAlarmNofication
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class AlarmService : Service() {
    inner class LocalBinder : Binder() {
        fun getService() = this@AlarmService
    }

    private val binder = LocalBinder()

    @Inject
    lateinit var preferences: DataStore<Preferences>

    @Inject
    lateinit var appDatabase: AppDatabase

    @Inject
    lateinit var alarmsManager: InternalAlarmsManager

    @Inject
    lateinit var alarmKlaxon: AlarmKlaxon

    private var currentAlarmId: Long = 0

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val alarmId = intent.getLongExtra(EXTRA_ID, -1)

        if(alarmId >= 0) {
            Log.i(TAG, "Command from outside: id=$alarmId action=${intent.action}")

            when (intent.action) {
                ACTION_START -> {
                    startAlarm(alarmId)
                }
                ACTION_DISMISS_ALARM -> {
                    dismissAlarmInternal(alarmId)
                }
                ACTION_SNOOZE_ALARM -> {
                    snoozeAlarmInternal(alarmId)
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun startAlarm(alarmId: Long) {
        currentAlarmId = alarmId

        alarmKlaxon.start()

        startActivity(AlarmActivity.createIntent(this, alarmId).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })

        GlobalAlarmStateManager.notify(alarmId, AlarmState.FIRED)

        scope.launch {
            val alarmEntry = appDatabase.alarmsDao().getAlarmById(alarmId)

            if (alarmEntry != null) {
                withContext(Dispatchers.Main) {
                    showAlarmNofication(this@AlarmService, alarmEntry)
                }
            }
        }
    }

    fun dismissAlarm(alarmId: Long) {
        dismissAlarmInternal(alarmId)
    }

    private fun dismissAlarmInternal(alarmId: Long) {


        scope.launch {
            appDatabase.alarmsDao().deleteAlarmById(alarmId)

            GlobalAlarmStateManager.notify(alarmId, AlarmState.DISMISSED)
            stopAlarm()
        }
    }

    fun snoozeAlarm(alarmId: Long) {
        snoozeAlarmInternal(alarmId)
    }

    private fun snoozeAlarmInternal(alarmId: Long) {
        scope.launch {
            val snoozeDuration = AppPreferences.snoozeDuration.getFrom(preferences)

            AlarmHelper.rescheduleForFromNow(
                appDatabase.alarmsDao(),
                alarmsManager,
                alarmId,
                snoozeDuration
            )

            GlobalAlarmStateManager.notify(alarmId, AlarmState.SNOOZED)
            stopAlarm()
        }
    }

    private fun stopAlarm() {
        stopAlarmNoStop()

        stopSelf()
    }

    private fun stopAlarmNoStop() {
        Log.i(TAG,"Stopping alarm klaxon and cancelling notification")

        alarmKlaxon.stop()

        cancelAlarmNotification(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.i(TAG, "onDestoy()")

        stopAlarmNoStop()
    }

    companion object {
        private val scope = CoroutineScope(Dispatchers.Default + CoroutineName("AlarmActionService"))
        private const val TAG = "AlarmService"

        const val ACTION_START = "io.pelmenstar.onealarm.START_ALARM"
        const val ACTION_DISMISS_ALARM = "io.pelmenstar.onealarm.CANCEL_ALARM"
        const val ACTION_SNOOZE_ALARM = "io.pelmenstar.onealarm.SNOOZE_ALARM"

        const val EXTRA_ID = "io.pelmenstar.onealarm.AlarmService:extra:id"

        fun createIntent(context: Context, alarmId: Long, action: String): Intent {
            return Intent(context, AlarmService::class.java).apply {
                this.action = action

                putExtra(EXTRA_ID, alarmId)
            }
        }
    }
}