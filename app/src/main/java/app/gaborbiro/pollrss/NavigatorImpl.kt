package app.gaborbiro.pollrss

import android.content.Intent
import app.gaborbiro.pollrss.AppContextProvider.appContext
import app.gaborbiro.pollrss.jobs.JobsActivity
import app.gaborbiro.pollrss.receiver.NotificationBroadcastReceiver
import app.gaborbiro.utils.Navigator

class NavigatorImpl : Navigator {
    override fun getMainActivityIntent(jobId: Long) =
        Intent(appContext, JobsActivity::class.java).apply {
            putExtra(JobsActivity.EXTRA_JOB_ID, jobId)
        }

    override fun getBroadcastIntent() =
        Intent(appContext, NotificationBroadcastReceiver::class.java)
}