package app.gaborbiro.pollrss

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import app.gaborbiro.utils.ACTION_FAVORITE
import app.gaborbiro.utils.ACTION_MARK_READ
import app.gaborbiro.utils.ACTION_SHARE
import app.gaborbiro.utils.LocalNotificationManager

class NotificationBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        intent.idFromMarkReadIntent()?.let {
            AppPreferences.markedAsViewed[it] = true
            Toast.makeText(context, "Marked as read", Toast.LENGTH_SHORT).show()
            LocalNotificationManager.hideNotification(it)
        }
        intent.idFromShareIntent()?.let {
            AppPreferences.markedAsViewed[it] = true
            if (AppPreferences.jobs.containsKey(it)) {
                return
            }
            try {
                Intent(Intent.ACTION_SEND).apply {
                    data = Uri.parse(AppPreferences.jobs[it]!!.link)
                    type = "text/plain"
                    putExtra("android.intent.extra.TEXT", it)
                    setClassName(
                        "com.pushbullet.android",
                        "com.pushbullet.android.ui.ShareActivity"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }.also {
                    context.startActivity(it)
                }
            } catch (e: Throwable) {
                e.printStackTrace()

                Intent(Intent.ACTION_SEND).apply {
                    data = Uri.parse(AppPreferences.jobs[it]!!.link)
                    type = "text/plain"
                    putExtra("android.intent.extra.TEXT", it)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }.also {
                    val chooserIntent = Intent.createChooser(it, "Share URL")
                    chooserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(chooserIntent)
                }
            }
            LocalNotificationManager.hideNotification(it)
        }
        intent.idFromFavoriteIntent()?.let {
            AppPreferences.markedAsViewed[it] = true
            if (!AppPreferences.favorites.contains(it)) {
                AppPreferences.favorites.add(it)
            }
            LocalNotificationManager.hideNotification(it)
        }
    }

    private fun Intent?.idFromMarkReadIntent() =
        if (this?.action?.startsWith(ACTION_MARK_READ) == true) action!!.substring(ACTION_MARK_READ.length).toLong() else null

    private fun Intent?.idFromShareIntent() =
        if (this?.action?.startsWith(ACTION_SHARE) == true) action!!.substring(ACTION_SHARE.length).toLong() else null

    private fun Intent?.idFromFavoriteIntent() =
        if (this?.action?.startsWith(ACTION_FAVORITE) == true) action!!.substring(ACTION_FAVORITE.length).toLong() else null
}