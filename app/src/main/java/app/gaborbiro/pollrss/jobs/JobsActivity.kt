package app.gaborbiro.pollrss.jobs

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.work.Constraints
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.gaborbiro.pollrss.App
import app.gaborbiro.pollrss.AppPreferences
import app.gaborbiro.pollrss.BuildConfig
import app.gaborbiro.pollrss.R
import app.gaborbiro.pollrss.UPWORK_RSS_URL
import app.gaborbiro.pollrss.data.JobsMapper
import app.gaborbiro.pollrss.favorites.FavoritesActivity
import app.gaborbiro.pollrss.model.Job
import app.gaborbiro.pollrss.rss.RssReader
import app.gaborbiro.pollrss.utils.*
import app.gaborbiro.utils.LocalNotificationManager
import com.google.android.material.snackbar.Snackbar
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_jobs.*
import kotlinx.android.synthetic.main.content_jobs.*
import kotlinx.android.synthetic.main.dialog_filters.view.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.cancelButton
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import org.jetbrains.anko.yesButton
import java.time.Duration
import java.time.ZonedDateTime


class JobsActivity : AppCompatActivity() {

    private var jobsLoaderDisposable: Disposable? = null
    private lateinit var adapter: JobsAdapter
    private var newJobsSnackbar: Snackbar? = null
    private var messageToShowOnLoad: String? = null
    private var filtersMenu: MenuItem? = null
    private var favoritesMenu: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jobs)
        setSupportActionBar(toolbar)

        swipe_refresh_layout.setOnRefreshListener {
            loadJobs()
        }
        AppPreferences.cleanupJobs()
        AppPreferences.observeFavorites.subscribe({
            if (it.isNotEmpty()) {
                favoritesMenu?.iconTintList = getColorStateList(R.color.colorAccent)
            } else {
                favoritesMenu?.iconTintList = null
            }
        }, {
            it.printStackTrace()
        })
        adapter = JobsAdapter(jobAdapterCallback)
    }

    override fun onResume() {
        super.onResume()
        loadJobs(intent.getLongExtra(EXTRA_JOB_ID, 0L)) { startBackgroundPolling() }
        LocalNotificationManager.hideNotifications()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.jobs, menu)
        filtersMenu = menu.findItem(R.id.action_filters)
        if (progress_indicator.visibility == View.VISIBLE) {
            filtersMenu?.isVisible = false
            progress_indicator_toolbar.visibility = View.VISIBLE
        }
        favoritesMenu = menu.findItem(R.id.action_favorites)
        if (AppPreferences.favorites.isNotEmpty()) {
            favoritesMenu?.iconTintList = getColorStateList(R.color.colorAccent)
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_filters -> {
                alert {
                    title = "Filters"
                    customView = layoutInflater.inflate(R.layout.dialog_filters, null).apply {
                        switch_show_hourly_only.isChecked = AppPreferences.showHourlyOnly
                        label_hourly.text = Html.fromHtml(getString(R.string.filter_hourly))
                        label_unread.text = Html.fromHtml(getString(R.string.filter_unread))
                        edit_text_min_pay.setText(AppPreferences.minPay.toString())
                        edit_text_min_pay.addTextChangedListener(object : TextChangeListener() {
                            override fun onTextChanged(
                                s: CharSequence,
                                start: Int,
                                before: Int,
                                count: Int
                            ) {
                                AppPreferences.minPay = s.toString().let {
                                    if (it.isBlank()) {
                                        0
                                    } else {
                                        it.toInt()
                                    }
                                }
                            }
                        })
                        group_min_pay.visibility =
                            if (!AppPreferences.showHourlyOnly) View.VISIBLE else View.GONE
                        switch_show_hourly_only.setOnCheckedChangeListener { _, isChecked ->
                            AppPreferences.showHourlyOnly = isChecked
                            group_min_pay.visibility =
                                if (AppPreferences.showHourlyOnly) View.GONE else View.VISIBLE
                        }
                        switch_show_unread_only.isChecked = AppPreferences.showUnreadOnly
                        switch_show_unread_only.setOnCheckedChangeListener { _, isChecked ->
                            AppPreferences.showUnreadOnly = isChecked
                        }
                    }
                    positiveButton("Apply") {
                        loadJobs()
                    }
                    onCancelled {
                        loadJobs()
                    }
                }.show()
            }
            R.id.action_mark_all -> {
                alert {
                    message = "Mark all jobs as read?"
                    yesButton {
                        AppPreferences.lastMarkAllReadTimestamp = epochMillis()
                        AppPreferences.markedAsRead.clear()
                        AppPreferences.cleanupJobs()
                        loadJobs()
                        if (!AppPreferences.showUnreadOnly) {
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
//            R.id.action_settings -> {
//                SettingsActivity.launch(this)
//            }
            R.id.action_debug_notif -> {
                Thread {
                    checkNewJobsAndShowNotification(applicationContext, forceNotification = true)
                }.start()
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
                .setAction("Refresh") { loadJobs() }
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

    private fun loadJobs(selectedJobId: Long? = null, onFinish: (() -> Unit)? = null) {
        setProgressVisible(true)
        jobsLoaderDisposable?.dispose()
        jobsLoaderDisposable = Maybe.create<List<Job>> { emitter ->
            try {
                AppPreferences.lastRefresh = epochMillis()
                RssReader.read(UPWORK_RSS_URL)?.let {
                    if (!emitter.isDisposed) {
                        emitter.onSuccess(it.rssItems.mapNotNull(JobsMapper::map))
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
            .subscribe(
                { handleJobs(it, selectedJobId) },
                {
                    it.printStackTrace()
                    it.message?.let(this::longToast) ?: toast("Oops. Something went wrong!")
                })
    }

    private fun handleJobs(jobs: List<Job>, selectJobId: Long? = null) {
        val filteredSortedJobs = jobs
            .filter(::shouldShowJob)
            .sortedByDescending { it.localDateTime }
        if (filteredSortedJobs.isNotEmpty()) {
            filteredSortedJobs.forEach {
                AppPreferences.jobs[it.id] = it
            }
            val jobUIModels = filteredSortedJobs.map { JobsUIMapper.map(it) }
            adapter.submitList(jobUIModels)
            recycle_view.adapter = adapter
            recycle_view.visibility = View.VISIBLE
            if (selectJobId != null) {
                val position = jobUIModels.indexOfFirst {
                    it.id == selectJobId
                }
                if (position > 0) {
                    Toast.makeText(this@JobsActivity, "Scroll to: $position", Toast.LENGTH_SHORT)
                        .show()
                    recycle_view.scrollToPosition(position)
                }
            }
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
        empty.text = if (!AppPreferences.showUnreadOnly) "No jobs" else
            (if(AppPreferences.favorites.isEmpty()) "No new jobs" else "No new jobs\n\nCheck your Favorites")
        messageToShowOnLoad?.let {
            showSnackBar(it, Snackbar.LENGTH_LONG).show()
            messageToShowOnLoad = null
        }
    }

    private fun setProgressVisible(visible: Boolean) {
        filtersMenu?.isVisible = !visible
        if (adapter.itemCount == 0) {
            progress_indicator.visibility = if (visible) View.VISIBLE else View.GONE
            progress_indicator_toolbar.visibility = View.GONE
        } else {
            progress_indicator.visibility = View.GONE
            progress_indicator_toolbar.visibility = if (visible) View.VISIBLE else View.GONE
        }
    }

    private val jobAdapterCallback = object : JobsAdapter.JobAdapterCallback {
        override fun onBodyClicked(job: JobUIModel) {
            openLink(job.link)
        }

        override fun onMarkedAsRead(job: JobUIModel) {
            AppPreferences.markedAsRead[job.id] = true
            if (AppPreferences.showUnreadOnly) {
                adapter?.removeItem(job)
                showSnackBar("Marked as read", Snackbar.LENGTH_SHORT).show()
                if (adapter?.itemCount == 0) {
                    recycle_view.visibility = View.GONE
                    empty.visibility = View.VISIBLE
                    empty.text = "No more jobs for now"
                }
            } else {
                adapter.markItemAsRead(job)
            }
        }

        override fun onMarkedAsUnread(job: JobUIModel) {
            AppPreferences.markedAsRead.remove(job.id)
            adapter.markItemAsUnread(job)
        }

        override fun onShare(job: JobUIModel) {
            share(job.link)
        }

        override fun onFavorite(job: JobUIModel) {
            AppPreferences.markedAsRead[job.id] = true
            AppPreferences.favorites.add(0, job.id)
            adapter?.removeItem(job)
            showSnackBar("Marked as favorite", Snackbar.LENGTH_SHORT).show()
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

    private fun showSnackBar(message: String, duration: Int): Snackbar {
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

        const val EXTRA_JOB_ID = "EXTRA_JOB_ID"
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
        return try {
            AppPreferences.lastRefresh = epochMillis()
            checkNewJobsAndShowNotification(applicationContext, forceNotification = false)
        } catch (t: Throwable) {
            t.printStackTrace()
            Result.failure()
        }
    }
}

fun checkNewJobsAndShowNotification(
    applicationContext: Context,
    forceNotification: Boolean
): ListenableWorker.Result {
    RssReader.read(UPWORK_RSS_URL)?.let { rssFeed ->
        val filteredJobs = rssFeed.rssItems
            .mapNotNull(JobsMapper::map)
            .filter(::shouldShowJob)
        if (filteredJobs.isNotEmpty()) {
            if (App.appIsInForeground && !forceNotification) {
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
        return ListenableWorker.Result.success()
    } ?: run {
        return ListenableWorker.Result.failure()
    }
}

fun shouldShowJob(job: Job): Boolean {
    val showAll =
        !AppPreferences.showUnreadOnly || (AppPreferences.markedAsRead[job.id] != true)
                && job.localDateTime.epochMillis() > AppPreferences.lastMarkAllReadTimestamp
    val hourly = !AppPreferences.showHourlyOnly || job.fixedBudgetValue == null
    val minPay = AppPreferences.minPay
    val payGoodEnough =
        AppPreferences.showHourlyOnly || job.fixedBudgetValue == null || job.fixedBudgetValue >= minPay
    return showAll && hourly && payGoodEnough
}

private const val REFRESH_INTERVAL_WIFI_MINS = 60L
private const val REFRESH_INTERVAL_MINS = 120L