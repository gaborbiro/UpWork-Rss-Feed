package app.gaborbiro.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.gaborbiro.pollrss.AppContextProvider
import com.piottechnologies.stationmaster.notifications.R

object LocalNotificationManager {

    private val appContext = AppContextProvider.appContext
    private val notificationManager =
        appContext.getSystemService(NotificationManager::class.java)
    private val notificationColor: Int = ContextCompat.getColor(appContext, android.R.color.white)

    init {
        createChannels()
    }

    fun showNewJobNotification(
        id: Int,
        title: String?,
        messageBody: String?,
        subscribeUrl: String? = null
    ) {
        val subscribeIntent =
            subscribeUrl?.let {
                PendingIntent.getActivity(
                    appContext,
                    1,
                    Intent(Intent.ACTION_VIEW, Uri.parse(subscribeUrl)),
                    0
                )
            }
        notificationManager.notify(
            "job_$id", id, buildNotification(
                pendingIntent = getLaunchIntent(),
                channel = CHANNEL_NEW_JOBS,
                title = title,
                message = messageBody,
                autoCancel = false
            ).apply {
                subscribeIntent?.let {
                    addAction(
                        R.drawable.ic_launcher_foreground,
                        "Subscribe",
                        it
                    )
                }
            }
                .build()
        )
    }

    fun hideNotifications() {
        notificationManager.cancelAll()
    }

    private fun getLaunchIntent(): PendingIntent {
        return PendingIntent.getActivity(
            appContext,
            0,
            NavigatorProvider.navigator.getMainActivityIntent(),
            0
        )
    }

    private fun buildNotification(
        pendingIntent: PendingIntent,
        channel: String,
        title: String?,
        message: String?,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        @ColorInt color: Int = notificationColor,
        @DrawableRes smallIcon: Int = R.drawable.ic_launcher_foreground,
        alertOnlyOnce: Boolean = true,
        autoCancel: Boolean = true,
        sound: Uri? = RingtoneManager.getDefaultUri(
            RingtoneManager.TYPE_NOTIFICATION
        ),
        ongoing: Boolean = false,
        vibrate: LongArray? = null
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(appContext, channel)
            .setSmallIcon(smallIcon)
            .setColor(color)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setPriority(priority)
            .setOnlyAlertOnce(alertOnlyOnce)
            .setAutoCancel(autoCancel)
            .setSound(sound)
            .setOngoing(ongoing)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setVibrate(vibrate)
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alertChannel = NotificationChannel(
                CHANNEL_NEW_JOBS, CHANNEL_NEW_JOBS,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(alertChannel)
        }
    }
}

val CHANNEL_NEW_JOBS = "New Jobs"