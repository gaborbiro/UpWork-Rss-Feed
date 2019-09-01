package app.gaborbiro.pollrss.jobs

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Constraints
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.gaborbiro.pollrss.AppPreferences
import app.gaborbiro.pollrss.R
import app.gaborbiro.pollrss.data.JobMapper
import app.gaborbiro.pollrss.favorite.FavoritesActivity
import app.gaborbiro.pollrss.model.Job
import app.gaborbiro.pollrss.model.formatDescriptionForNotification
import app.gaborbiro.pollrss.rss.RssReader
import app.gaborbiro.pollrss.utils.share
import app.gaborbiro.utils.LocalNotificationManager
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_jobs.*
import kotlinx.android.synthetic.main.content_jobs.*
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import java.net.URL
import java.time.Duration
import java.time.ZoneOffset

class JobsActivity : AppCompatActivity() {

    private var disposable: Disposable? = null
    private var adapter: JobAdapter? = null
    private var pendingMarkAsReadId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jobs)
        setSupportActionBar(toolbar)

        loadJobs()

        swipe_refresh_layout.setOnRefreshListener {
            loadJobs()
            swipe_refresh_layout.isRefreshing = true
        }
        AppPreferences.cleanupJobs()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_favorites -> {
                FavoritesActivity.start(this)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        disposable?.dispose()
        pendingMarkAsReadId?.let {
            AppPreferences.markedAsRead[it] = true
            pendingMarkAsReadId = null
        }
    }

    private fun loadJobs() {
        pendingMarkAsReadId?.let {
            AppPreferences.markedAsRead[it] = true
            pendingMarkAsReadId = null
        }
        if (!swipe_refresh_layout.isRefreshing) {
            progress_indicator.visibility = View.VISIBLE
        }
        LocalNotificationManager.hideNotifications()
        disposable?.dispose()
        disposable = Maybe.create<List<Job>> { emitter ->
            try {
                RssReader.read(UPWORK_RSS_URL)?.let {
                    emitter.onSuccess(it.rssItems.mapNotNull(JobMapper::map))
                }
            } catch (t: Throwable) {
                emitter.onError(t)
            }
            emitter.onComplete()
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnTerminate {
                progress_indicator.visibility = View.GONE
                swipe_refresh_layout.isRefreshing = false
            }
            .subscribe({ jobs: List<Job> ->
                jobs.forEach {
                    AppPreferences.jobs[it.id] = it
                }
                val filteredSortedJobs = jobs
                    .filter { AppPreferences.markedAsRead[it.id] != true }
                    .sortedByDescending { it.localDateTime }
                if (filteredSortedJobs.isNotEmpty()) {
                    adapter = JobAdapter(
                        filteredSortedJobs.toMutableList(),
                        jobAdapterCallback
                    )
                    recycle_view.adapter = adapter
                    AppPreferences.lastSeenDate =
                        filteredSortedJobs[0].localDateTime.toInstant(ZoneOffset.UTC)
                            .toEpochMilli()
                    recycle_view.visibility = View.VISIBLE
                    empty.visibility = View.GONE
                } else {
                    recycle_view.visibility = View.GONE
                    empty.visibility = View.VISIBLE
                }
                startBackgroundPolling()
            },
                {
                    it.printStackTrace()
                    it.message?.let(this::longToast) ?: toast("Oops. Something went wrong!")
                })
    }

    private val jobAdapterCallback = object : JobAdapter.JobAdapterCallback {
        override fun onMarkedAsRead(job: Job) {
            val position = adapter?.removeItem(job)
            pendingMarkAsReadId = job.id
            Snackbar.make(recycle_view, "Marked as read", Snackbar.LENGTH_LONG)
                .setAction("Undo") {
                    adapter?.addItem(position!!, job)
                }
                .addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar?>() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        if (event == Snackbar.Callback.DISMISS_EVENT_TIMEOUT || event == DISMISS_EVENT_CONSECUTIVE) {
                            AppPreferences.markedAsRead[job.id] = true
                            pendingMarkAsReadId = null
                        }
                    }
                })
                .show()
        }

        override fun onShare(job: Job) {
            share(job.link)
        }

        override fun onFavorite(job: Job) {
            AppPreferences.markedAsRead[job.id] = true
            AppPreferences.favorites.add(job.id)
            adapter?.removeItem(job)
            Snackbar.make(recycle_view, "Marked as favorite", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun startBackgroundPolling() {
        WorkManager.getInstance().cancelAllWorkByTag("PollRss")
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
        val saveRequest =
            PeriodicWorkRequest.Builder(PollRssWorker::class.java, Duration.ofMinutes(15))
                .addTag("PollRss")
                .setConstraints(constraints)
                .build()
        WorkManager.getInstance().enqueue(saveRequest)
    }
}

class PollRssWorker(appContext: Context, workerParams: WorkerParameters) :
    androidx.work.Worker(appContext, workerParams) {

    override fun doWork(): Result {
        try {
            RssReader.read(UPWORK_RSS_URL)?.let { rssFeed ->
                rssFeed.rssItems
                    .mapNotNull(JobMapper::map)
                    .filter {
                        AppPreferences.markedAsRead[it.id] != true && it.localDateTime.toInstant(
                            ZoneOffset.UTC
                        ).toEpochMilli() > AppPreferences.lastSeenDate
                    }
                    .sortedByDescending { it.localDateTime }
                    .take(5)
                    .reversed()
                    .forEach { job ->
                        LocalNotificationManager.showNewJobNotification(
                            id = job.id,
                            title = job.title,
                            messageBody = job.formatDescriptionForNotification()
                        )
                    }
                return Result.success()
            } ?: run {
                return Result.failure()
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            return Result.failure()
        }
    }

    override fun onStopped() {
        super.onStopped()
        LocalNotificationManager.hideNotifications()
    }
}

private val UPWORK_RSS_URL =
    URL("https://www.upwork.com/ab/feed/topics/rss?securityToken=6cb37a9e960ed9e0cc7e0bbef8480b89fc9c6d394f866ccf805cfffb02b2f57167360c1f4c38622baa11c78a12b07de5c89ca10de1774204bbf3597e98312894&userUid=1130704448110034944&orgUid=1130704448118423553&sort=local_jobs_on_top&topic=4394157")