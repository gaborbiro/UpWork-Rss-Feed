package app.gaborbiro.pollrss

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Constraints
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.gaborbiro.pollrss.model.*
import app.gaborbiro.pollrss.rss.RssReader
import app.gaborbiro.utils.LocalNotificationManager
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import java.net.URL
import java.time.Duration
import java.time.ZoneOffset

class MainActivity : AppCompatActivity() {

    private var disposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        content.movementMethod = LinkMovementMethod.getInstance()
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.extras.get("com.android.browser.application_id") != BuildConfig.APPLICATION_ID) {
            return
        }
        when (intent.data?.path) {
            "/$PATH_MARK_READ" -> {
                val id = intent.data.getQueryParameter(QUERY_PARAM_ID)!!.toLong()
                AppPreferences.markedAsViewed[id] = true
                loadJobs()
            }
            "/$PATH_FAVORITE" -> {
                val id = intent.data.getQueryParameter(QUERY_PARAM_ID)!!.toLong()
                AppPreferences.markedAsViewed[id] = true
                AppPreferences.favorites.add(id)
                Toast.makeText(this, "Marked as favorite", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        disposable?.dispose()
    }

    private fun loadJobs() {
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
                content.visibility = View.VISIBLE
                progress_indicator.visibility = View.GONE
                swipe_refresh_layout.isRefreshing = false
            }
            .subscribe({ jobs: List<Job> ->
                jobs.forEach {
                    AppPreferences.jobs[it.id] = it
                }
                val filteredSortedJobs = jobs
                    .filter { AppPreferences.markedAsViewed[it.id] != true }
                    .sortedByDescending { it.localDateTime }
                val contentStr = filteredSortedJobs
                    .joinToString(
                        separator = "<br><br>",
                        transform = Job::formatDescriptionForList
                    )
                if (contentStr.isNotEmpty()) {
                    content.text = Html.fromHtml(contentStr, 0)
                } else {
                    content.text = "No new jobs"
                }
                AppPreferences.lastSeenDate =
                    filteredSortedJobs[0].localDateTime.toInstant(ZoneOffset.UTC)
                        .toEpochMilli()
                startBackgroundPolling()
            },
                {
                    it.printStackTrace()
                    it.message?.let(this::longToast) ?: toast("Oops. Something went wrong!")
                })
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
                        AppPreferences.markedAsViewed[it.id] != true && it.localDateTime.toInstant(
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