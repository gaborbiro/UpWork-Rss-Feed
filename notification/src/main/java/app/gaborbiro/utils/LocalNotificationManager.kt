package app.gaborbiro.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
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
        index: Int,
        title: String?,
        messageBody: String?,
        url: String
    ) {
        notificationManager.notify(
            "job_$index", index, buildNotification(
                pendingIntent = getLaunchIntent(),
                channel = CHANNEL_NEW_JOBS,
                title = title,
                message = messageBody,
                autoCancel = false
            ).apply {
                addAction(
                    R.drawable.ic_launcher_foreground,
                    "View in browser",
                    getViewIntent(url)
                )
                val shareActionName =
                    if (isPackageInstalled("com.pushbullet.android")) "Push" else "Share"
                addAction(
                    R.drawable.ic_launcher_foreground,
                    shareActionName,
                    getShareIntent(url)
                )
            }.build().also {
                it.deleteIntent = getDeleteIntent(url)
            }
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

    private fun getViewIntent(url: String) = PendingIntent.getActivity(
        appContext,
        1,
        Intent(Intent.ACTION_VIEW, Uri.parse(url)),
        0
    )

    private fun getDeleteIntent(jobId: String): PendingIntent {
        val intent = NavigatorProvider.navigator.getDismissJobIntent()
        intent.action = ACTION_DISMISS + jobId
        return PendingIntent.getBroadcast(appContext, 0, intent, 0)
    }

    private fun getShareIntent(jobId: String): PendingIntent {
        val intent = NavigatorProvider.navigator.getDismissJobIntent()
        intent.action = ACTION_SHARE + jobId
        return PendingIntent.getBroadcast(appContext, 0, intent, 0)
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

    private fun isPackageInstalled(packageName: String): Boolean {
        var found = true
        try {
            appContext.packageManager.getPackageInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            found = false
        }
        return found
    }
}

val CHANNEL_NEW_JOBS = "New Jobs"

val ACTION_DISMISS = "DISMISS"
val ACTION_SHARE = "SHARE"