package app.gaborbiro.pollrss

import android.content.Context
import android.os.Bundle
import android.text.Html
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Constraints
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.gaborbiro.pollrss.model.Job
import app.gaborbiro.pollrss.model.exactFormattedTime
import app.gaborbiro.pollrss.model.simpleFormattedTime
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
    }

    override fun onPause() {
        super.onPause()
        disposable?.dispose()
    }

    private fun loadJobs(replace: Boolean = true) {
        if (!swipe_refresh_layout.isRefreshing) {
            progress_indicator.visibility = View.VISIBLE
            if (replace) content.visibility = View.GONE
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
            .subscribe({ jobs ->
                val contentStr = jobs
                    .filter { AppPreferences.markedAsViewed[it.id] != true }
                    .sortedByDescending { it.localDateTime }
                    .joinToString(separator = "<br><br>", transform = Job::formatDescriptionForList)
                if (replace) {
                    if (contentStr.isNotEmpty()) {
                        content.text = Html.fromHtml(contentStr, 0)
                    } else {
                        content.text = "No new jobs"
                    }
                } else {
                    if (contentStr.isNotEmpty()) {
                        content.text =
                            TextUtils.concat(content.text, Html.fromHtml("<br><br>$contentStr", 0))
                    }
                }
                startBackgroundPolling()
            }, {
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
                    .filter { AppPreferences.markedAsViewed[it.id] != true }
                    .sortedByDescending { it.localDateTime }
                    .take(5)
                    .reversed()
                    .forEachIndexed { index, job ->
                        LocalNotificationManager.showNewJobNotification(
                            index = index,
                            title = job.title,
                            messageBody = job.formatDescriptionForNotification(),
                            url = job.link
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

private fun Job.formatDescriptionForNotification(): String {
    val job = this
    return StringBuilder().apply {
        append(job.simpleFormattedTime() + ", ")
        appendln()
        append("${job.budget ?: "Hourly"}: ")
        append(Html.fromHtml(job.description, 0))
        appendln()
        job.skills?.let { append("Skills: ${job.skills}") }
        job.country?.let { append(", from ${job.country}") }
        job.category?.let { append("\nCategory: ${Html.fromHtml(job.category, 0)}") }
    }.toString()
}

private fun Job.formatDescriptionForList(): String {
    return "<b>Title:</b> ${this.title}" +
            "<br><b>Description:</b> ${this.description}" +
            "<br><b>Posted:</b> ${this.exactFormattedTime()}" +
            "<br><a href='${this.link}'>View in browser</a>"
}

private val UPWORK_RSS_URL =
    URL("https://www.upwork.com/ab/feed/topics/rss?securityToken=6cb37a9e960ed9e0cc7e0bbef8480b89fc9c6d394f866ccf805cfffb02b2f57167360c1f4c38622baa11c78a12b07de5c89ca10de1774204bbf3597e98312894&userUid=1130704448110034944&orgUid=1130704448118423553&sort=local_jobs_on_top&topic=4394157")