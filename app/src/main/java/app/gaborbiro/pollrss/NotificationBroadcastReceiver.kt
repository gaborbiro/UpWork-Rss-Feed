package app.gaborbiro.pollrss

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import app.gaborbiro.utils.ACTION_DISMISS
import app.gaborbiro.utils.ACTION_SHARE

class NotificationBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        intent.urlFromDismissIntent()?.let {
            AppPreferences.markedAsViewed[it] = true
            Toast.makeText(context, "Marked as read", Toast.LENGTH_SHORT).show()
        }
        intent.urlFromShareIntent()?.let {
            AppPreferences.markedAsViewed[it] = true
            try {
                Intent(Intent.ACTION_SEND).apply {
                    data = Uri.parse(it)
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
                    data = Uri.parse(it)
                    type = "text/plain"
                    putExtra("android.intent.extra.TEXT", it)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }.also {
                    val chooserIntent = Intent.createChooser(it, "Share URL")
                    chooserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(chooserIntent)
                }
            }
        }
    }

    private fun Intent?.urlFromDismissIntent() =
        if (this?.action?.startsWith(ACTION_DISMISS) == true) action!!.substring(ACTION_DISMISS.length) else null

    private fun Intent?.urlFromShareIntent() =
        if (this?.action?.startsWith(ACTION_SHARE) == true) action!!.substring(ACTION_SHARE.length) else null
}