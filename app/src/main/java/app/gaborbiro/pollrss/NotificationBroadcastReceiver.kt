package app.gaborbiro.pollrss

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.gaborbiro.utils.ACTION_DISMISS

class NotificationBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action?.startsWith(ACTION_DISMISS) == true) {
            intent.action?.substring(ACTION_DISMISS.length)?.let {
                AppPreferences.dismissedJobs[it] = true
            }
        }
    }
}