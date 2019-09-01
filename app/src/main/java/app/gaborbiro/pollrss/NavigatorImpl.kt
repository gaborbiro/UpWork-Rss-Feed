package app.gaborbiro.pollrss

import android.content.Intent
import app.gaborbiro.pollrss.AppContextProvider.appContext
import app.gaborbiro.pollrss.jobs.JobsActivity
import app.gaborbiro.pollrss.receiver.NotificationBroadcastReceiver
import app.gaborbiro.utils.Navigator

class NavigatorImpl : Navigator {
    override fun getMainActivityIntent() = Intent(appContext, JobsActivity::class.java)

    override fun getBroadcastIntent() =
        Intent(appContext, NotificationBroadcastReceiver::class.java)
}