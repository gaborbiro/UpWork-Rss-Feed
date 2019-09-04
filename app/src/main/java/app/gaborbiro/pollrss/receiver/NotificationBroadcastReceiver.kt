package app.gaborbiro.pollrss.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import app.gaborbiro.pollrss.AppPreferences
import app.gaborbiro.pollrss.utils.share
import app.gaborbiro.utils.ACTION_FAVORITE
import app.gaborbiro.utils.ACTION_MARK_READ
import app.gaborbiro.utils.ACTION_SHARE
import app.gaborbiro.utils.LocalNotificationManager

class NotificationBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        intent.idFromMarkReadIntent()?.let {
            AppPreferences.markedAsRead[it] = true
            Toast.makeText(context, "Marked as read", Toast.LENGTH_SHORT).show()
            LocalNotificationManager.hideNotification(it)
        }
        intent.idFromShareIntent()?.let {
            if (!AppPreferences.jobs.containsKey(it)) {
                return
            }
            context.share(AppPreferences.jobs[it]!!.link)
        }
        intent.idFromFavoriteIntent()?.let {
            AppPreferences.markedAsRead[it] = true
            if (!AppPreferences.favorites.contains(it)) {
                AppPreferences.favorites.add(0, it)
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