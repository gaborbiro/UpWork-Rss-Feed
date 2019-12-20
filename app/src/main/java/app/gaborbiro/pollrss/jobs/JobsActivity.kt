package app.gaborbiro.pollrss.jobs

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.work.*
import app.gaborbiro.pollrss.*
import app.gaborbiro.pollrss.R
import app.gaborbiro.pollrss.data.JobMapper
import app.gaborbiro.pollrss.favorites.FavoritesActivity
import app.gaborbiro.pollrss.model.Job
import app.gaborbiro.pollrss.rss.RssReader
import app.gaborbiro.pollrss.settings.SettingsActivity
import app.gaborbiro.pollrss.utils.epochMillis
import app.gaborbiro.pollrss.utils.openLink
import app.gaborbiro.pollrss.utils.share
import app.gaborbiro.pollrss.utils.toZDT
import app.gaborbiro.utils.LocalNotificationManager
import com.google.android.material.snackbar.Snackbar
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_jobs.*
import kotlinx.android.synthetic.main.content_jobs.*
import org.jetbrains.anko.*
import java.time.Duration
import java.time.ZonedDateTime


class JobsActivity : AppCompatActivity() {

    private var jobsLoaderDisposable: Disposable? = null
    private var adapter: JobsAdapter? = null
    private var newJobsSnackbar: Snackbar? = null
    private var messageToShowOnLoad: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jobs)
        setSupportActionBar(toolbar)

        swipe_refresh_layout.setOnRefreshListener {
            loadJobs()
            swipe_refresh_layout.isRefreshing = true
        }
        unread_switch.isChecked = AppPreferences.showAll
        unread_switch.setOnCheckedChangeListener { _, isChecked ->
            AppPreferences.showAll = isChecked
            loadJobs()
            val message = if (isChecked) {
                "Showing all jobs"
            } else {
                "Showing new jobs only"
            }
            makeBottomSnackBar(message, Snackbar.LENGTH_SHORT).show()
            updateUnreadSwitchText()
        }
        updateUnreadSwitchText()

        hourly_switch.isChecked = AppPreferences.showHourly
        hourly_switch.setOnCheckedChangeListener { _, isChecked ->
            AppPreferences.showHourly = isChecked
            loadJobs()
            val message = if (isChecked) {
                "Showing hourly jobs"
            } else {
                "Showing all jobs"
            }
            makeBottomSnackBar(message, Snackbar.LENGTH_LONG).show()
        }
        AppPreferences.cleanupJobs()
    }

    private fun updateUnreadSwitchText() {
        unread_switch.text = if (AppPreferences.showAll) "All" else "New"
    }

    override fun onResume() {
        super.onResume()
        loadJobs {
            startBackgroundPolling()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.jobs, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_mark_all -> {
                alert {
                    message = "Mark all unread jobs as read?"
                    yesButton {
                        AppPreferences.lastMarkAllReadTimestamp = epochMillis()
                        AppPreferences.markedAsRead.clear()
                        AppPreferences.cleanupJobs()
                        loadJobs()
                        if (AppPreferences.showAll) {
                            messageToShowOnLoad =
                                "You're in \"Show all\" mode. Toggle the switch at the top of the " +
                                        "screen if you only want to see new jobs."
                        }
                    }
                    cancelButton { }
                }.show()
                return true
            }
            R.id.action_favorites -> {
                FavoritesActivity.start(this)
                return true
            }
            R.id.action_settings -> {
                SettingsActivity.launch(this)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == ACTION_NEW_JOBS_WARNING && (jobsLoaderDisposable == null ||
                    jobsLoaderDisposable?.isDisposed == true) && intent.getLongExtra(
                EXTRA_MAX_BACKGROUND_JOB_TIMESTAMP, 0L
            ) > AppPreferences.lastDisplayedTimestamp
        ) {
            newJobsSnackbar?.dismiss()
            newJobsSnackbar = makeTopSnackBar("New jobs are available", Snackbar.LENGTH_INDEFINITE)
                .setAction("Refresh") {
                    loadJobs()
                }
            newJobsSnackbar?.show()
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        newJobsSnackbar?.dismiss()
        newJobsSnackbar = null
    }

    override fun onPause() {
        super.onPause()
        jobsLoaderDisposable?.dispose()
    }

    private fun loadJobs(onFinish: (() -> Unit)? = null) {
        if (!swipe_refresh_layout.isRefreshing) {
            setProgressVisible(true)
        }
        jobsLoaderDisposable?.dispose()
        jobsLoaderDisposable = Maybe.create<List<Job>> { emitter ->
            try {
                AppPreferences.lastRefresh = epochMillis()
                RssReader.read(UPWORK_RSS_URL)?.let {
                    if (!emitter.isDisposed) {
                        emitter.onSuccess(it.rssItems.mapNotNull(JobMapper::map))
                    }
                }
            } catch (t: Throwable) {
                if (!emitter.isDisposed) {
                    emitter.onError(t)
                }
            }
            if (!emitter.isDisposed) {
                emitter.onComplete()
            }
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnTerminate {
                setProgressVisible(false)
                swipe_refresh_layout.isRefreshing = false
            }
            .doAfterTerminate {
                onFinish?.invoke()
            }
            .subscribe({ jobs: List<Job> ->
                val filteredSortedJobs = jobs
                    .filter {
                        val showAll =
                            AppPreferences.showAll || (AppPreferences.markedAsRead[it.id] != true)
                                    && it.localDateTime.epochMillis() > AppPreferences.lastMarkAllReadTimestamp
                        val hourly = !AppPreferences.showHourly || it.budget == null
                        showAll && hourly
                    }
                    .sortedByDescending { it.localDateTime }
                if (filteredSortedJobs.isNotEmpty()) {
                    filteredSortedJobs.forEach {
                        AppPreferences.jobs[it.id] = it
                    }
                    adapter = JobsAdapter(
                        filteredSortedJobs.map { JobsUIMapper.map(it) }.toMutableList(),
                        jobAdapterCallback
                    )
                    recycle_view.adapter = adapter
                    recycle_view.visibility = View.VISIBLE
                    empty.visibility = View.GONE
                    AppPreferences.lastDisplayedTimestamp =
                        filteredSortedJobs[0].localDateTime.epochMillis()
                } else {
                    recycle_view.visibility = View.GONE
                    empty.visibility = View.VISIBLE
                }
                if (BuildConfig.DEBUG) {
                    Toast.makeText(
                        this,
                        "Jobs stored: " + AppPreferences.jobs.size,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                empty.text = if (AppPreferences.showAll) "No jobs" else "No new jobs"
                messageToShowOnLoad?.let {
                    makeBottomSnackBar(it, Snackbar.LENGTH_LONG).show()
                    messageToShowOnLoad = null
                }
            },
                {
                    it.printStackTrace()
                    it.message?.let(this::longToast) ?: toast("Oops. Something went wrong!")
                })
    }

    private fun setProgressVisible(visible: Boolean) {
        if (adapter?.itemCount ?: 0 == 0) {
            unread_switch.visibility = View.VISIBLE
            hourly_switch.visibility = View.VISIBLE
            progress_indicator.visibility = if (visible) View.VISIBLE else View.GONE
            progress_indicator_toolbar.visibility = View.GONE
        } else {
            unread_switch.visibility = if (visible) View.GONE else View.VISIBLE
            hourly_switch.visibility = if (visible) View.GONE else View.VISIBLE
            progress_indicator.visibility = View.GONE
            progress_indicator_toolbar.visibility = if (visible) View.VISIBLE else View.GONE
        }
    }

    private val jobAdapterCallback = object : JobsAdapter.JobAdapterCallback {
        override fun onBodyClicked(job: JobUIModel) {
            openLink(job.link)
        }

        override fun onMarkedAsRead(job: JobUIModel) {
            if (!AppPreferences.showAll) {
                adapter?.removeItem(job)
                AppPreferences.markedAsRead[job.id] = true
                makeBottomSnackBar("Marked as read", Snackbar.LENGTH_SHORT).show()
                if (adapter?.itemCount == 0) {
                    recycle_view.visibility = View.GONE
                    empty.visibility = View.VISIBLE
                    empty.text = "No more jobs for now"
                }
            } else {
                adapter?.markItemAsRead(job)
            }
        }

        override fun onMarkedAsUnread(job: JobUIModel) {
            AppPreferences.markedAsRead.remove(job.id)
            adapter?.markItemAsUnread(job)
        }

        override fun onShare(job: JobUIModel) {
            share(job.link)
        }

        override fun onFavorite(job: JobUIModel) {
            AppPreferences.markedAsRead[job.id] = true
            AppPreferences.favorites.add(0, job.id)
            adapter?.removeItem(job)
            makeBottomSnackBar("Marked as favorite", Snackbar.LENGTH_SHORT).show()
            if (adapter?.itemCount == 0) {
                recycle_view.visibility = View.GONE
                empty.visibility = View.VISIBLE
                empty.text = "No more jobs for now"
            }
        }
    }

    private fun startBackgroundPolling() {
        val allConstraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
        val fetchRequest =
            PeriodicWorkRequest.Builder(
                PollRssWorker::class.java,
                Duration.ofMinutes(REFRESH_INTERVAL_MINS)
            )
                .addTag("PollRss")
                .setConstraints(allConstraints)
                .build()
        WorkManager.getInstance().cancelAllWorkByTag("PollRss")
        WorkManager.getInstance().enqueue(fetchRequest)
        val wifiConstraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .build()
        val wifiFetchRequest =
            PeriodicWorkRequest.Builder(
                PollRssWorker::class.java,
                Duration.ofMinutes(REFRESH_INTERVAL_WIFI_MINS)
            )
                .addTag("PollRssWifiOnly")
                .setConstraints(wifiConstraints)
                .build()
        WorkManager.getInstance().cancelAllWorkByTag("PollRssWifiOnly")
        WorkManager.getInstance().enqueue(wifiFetchRequest)
    }

    private fun makeTopSnackBar(message: String, duration: Int): Snackbar {
        return Snackbar.make(snackbar_host, message, duration)
            .apply {
                (view.layoutParams as CoordinatorLayout.LayoutParams).apply {
                    gravity = Gravity.TOP
                }.also {
                    view.layoutParams = it
                    view.rotation = 180f
                }
            }
    }

    private fun makeBottomSnackBar(message: String, duration: Int): Snackbar {
        return Snackbar.make(recycle_view, message, duration)
    }

    companion object {
        fun sendNewJobsWarningIntent(context: Context, maxBackgroundJobTimestamp: Long) {
            Intent(context, JobsActivity::class.java).apply {
                action = ACTION_NEW_JOBS_WARNING
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(EXTRA_MAX_BACKGROUND_JOB_TIMESTAMP, maxBackgroundJobTimestamp)
            }.also {
                context.startActivity(it)
            }
        }

        private const val EXTRA_MAX_BACKGROUND_JOB_TIMESTAMP = "MAX_BACKGROUND_JOB_TIMESTAMP"
    }
}

private const val ACTION_NEW_JOBS_WARNING = "NEW_JOBS_WARNING"

class PollRssWorker(appContext: Context, workerParams: WorkerParameters) :
    androidx.work.Worker(appContext, workerParams) {

    override fun doWork(): Result {
        if (Duration.between(
                AppPreferences.lastRefresh.toZDT(),
                ZonedDateTime.now()
            ) < Duration.ofMinutes(REFRESH_INTERVAL_MINS)
        ) {
            return Result.failure()
        }
        try {
            AppPreferences.lastRefresh = epochMillis()
            RssReader.read(UPWORK_RSS_URL)?.let { rssFeed ->
                val filteredJobs = rssFeed.rssItems
                    .mapNotNull(JobMapper::map)
                    .filter {
                        AppPreferences.markedAsRead[it.id] != true
                                && it.localDateTime.epochMillis() > AppPreferences.lastMarkAllReadTimestamp
                    }
                if (filteredJobs.isNotEmpty()) {
                    if (App.appIsInForeground) {
                        JobsActivity.sendNewJobsWarningIntent(
                            applicationContext,
                            filteredJobs.maxBy { it.localDateTime }!!.localDateTime.epochMillis()
                        )
                    } else {
                        filteredJobs.sortedByDescending { it.localDateTime }
                            .take(5)
                            .reversed().forEach { job ->
                                AppPreferences.jobs[job.id] = job
                                LocalNotificationManager.showNewJobNotification(
                                    id = job.id,
                                    title = job.title,
                                    messageBody = JobsUIMapper.formatDescriptionForNotification(job)
                                )
                            }
                    }
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
}

private const val REFRESH_INTERVAL_WIFI_MINS = 15L
private const val REFRESH_INTERVAL_MINS = 50L