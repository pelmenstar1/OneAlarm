package io.pelmenstar.onealarm.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.icu.text.DateTimePatternGenerator
import android.os.Build
import android.text.format.DateFormat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.pelmenstar.onealarm.AlarmService
import io.pelmenstar.onealarm.R
import io.pelmenstar.onealarm.data.AlarmEntry

private const val CHANNEL_ID = "alarmChannel"
private const val CONTENT_FORMAT = "E HH:mm"

@RequiresApi(26)
private fun createChannel(context: Context): NotificationChannel {
    return NotificationChannel(
        CHANNEL_ID,
        context.getText(R.string.alarm),
        NotificationManager.IMPORTANCE_HIGH
    )
}

private fun createContentText(context: Context, alarmEntry: AlarmEntry): CharSequence {
    val format = if (Build.VERSION.SDK_INT >= 24) {
        val generator =
            DateTimePatternGenerator.getInstance(context.resources.configuration.locales[0])

        generator.getBestPattern(CONTENT_FORMAT)
    } else {
        CONTENT_FORMAT
    }

    return DateFormat.format(format, alarmEntry.epochSeconds * 1000)
}

fun showAlarmNofication(service: Service, alarmEntry: AlarmEntry) {
    val res = service.resources

    val builder = NotificationCompat.Builder(service, CHANNEL_ID)
        .setContentTitle(res.getText(R.string.alarm))
        .setContentText(createContentText(service, alarmEntry))
        .setSmallIcon(R.drawable.ic_alarm)
        .setAutoCancel(false)
        .setSilent(true)
        .setOngoing(true)
        .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setLocalOnly(true)
        .setShowWhen(false)
        .setWhen(0)

    val alarmId = alarmEntry.id

    var intentFlags = PendingIntent.FLAG_UPDATE_CURRENT
    if (Build.VERSION.SDK_INT >= 31) {
        intentFlags = intentFlags or PendingIntent.FLAG_MUTABLE
    }

    val dismissIntent = PendingIntent.getService(
        service,
        0,
        AlarmService.createIntent(service, alarmId, AlarmService.ACTION_DISMISS_ALARM),
        intentFlags
    )

    val snoozeIntent = PendingIntent.getService(
        service,
        0,
        AlarmService.createIntent(service, alarmId, AlarmService.ACTION_SNOOZE_ALARM),
        intentFlags
    )

    val contentIntent = PendingIntent.getActivity(
        service,
        0,
        AlarmActivity.createIntent(service, alarmId).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        },
        intentFlags
    )

    builder.run {
        addAction(R.drawable.ic_snooze, res.getText(R.string.snooze), snoozeIntent)
        addAction(R.drawable.ic_alarm_off, res.getText(R.string.dismiss), dismissIntent)

        setContentIntent(contentIntent)
        setPriority(NotificationCompat.PRIORITY_MAX)
    }

    val manager = NotificationManagerCompat.from(service)

    if (Build.VERSION.SDK_INT >= 26) {
        manager.createNotificationChannel(createChannel(service))
    }

    val notificationId = alarmEntry.id.toInt()
    val notification = builder.build()

    service.startForeground(notificationId, notification)
}

fun cancelAlarmNotification(service: Service) {
    service.stopForeground(true)
}