package app.gaborbiro.pullrss

import android.content.Context
import android.os.Bundle
import android.text.Html
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import app.gaborbiro.pullrss.model.Job
import app.gaborbiro.pullrss.rss.RssFeed
import app.gaborbiro.pullrss.rss.RssReader
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

        LocalNotificationManager.hideNotifications()

        disposable = Maybe.create<RssFeed> { emitter ->
            try {
                RssReader.read(URL(UPWORK_RSS_URL))?.let {
                    emitter.onSuccess(it)
                }
            } catch (t: Throwable) {
                emitter.onError(t)
            }
            emitter.onComplete()
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                val str = it.rssItems.filter { it.pubDate!!.time > AppPreferences.lastSeenDate }
                    .sortedByDescending { it.pubDate }
                    .joinToString(separator = "<br><br>") {
                        "<b>Title:</b> " + it.title + "<br><b>Description:</b> " + it.description + "<br>"
                    }
                content.text = Html.fromHtml(str, 0)
                if (it.rssItems.isNotEmpty()) {
                    AppPreferences.lastSeenDate =
                        it.rssItems.maxBy { it.pubDate!! }!!.pubDate!!.time
                    AppPreferences.lastNotifiedDate = AppPreferences.lastSeenDate
                }

                startBackgroundPolling()
            }, {
                it.printStackTrace()
                it.message?.let(this::longToast) ?: toast("Oops. Something went wrong!")
            })
    }

    override fun onPause() {
        super.onPause()
        disposable?.dispose()
    }

    private fun startBackgroundPolling() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val saveRequest =
            PeriodicWorkRequest.Builder(PullRssWorker::class.java, Duration.ofMinutes(15))
                .setConstraints(constraints)
                .build()
        WorkManager.getInstance().enqueue(saveRequest)
    }
}

class PullRssWorker(appContext: Context, workerParams: WorkerParameters) :
    androidx.work.Worker(appContext, workerParams) {

    override fun doWork(): Result {
        try {
            RssReader.read(URL(UPWORK_RSS_URL))?.let { rssFeed ->
                val newJobs =
                    rssFeed.rssItems.filter { it.pubDate!!.time > AppPreferences.lastNotifiedDate }
                        .map(JobMapper::map)
                if (newJobs.isNotEmpty()) {
                    AppPreferences.lastNotifiedDate =
                        rssFeed.rssItems.maxBy { it.pubDate!! }!!.pubDate!!.time
                }
                newJobs.sortedByDescending { it.pubDate }.take(5).reversed()
                    .forEachIndexed { index, job ->
                        LocalNotificationManager.showNewJobNotification(
                            id = index,
                            title = job.title,
                            messageBody = formatDescription(job)
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

    private fun formatDescription(job: Job): String {
        return StringBuilder().apply {
            append(job.description)
            appendln()
            job.budget?.let { append("\nBudget: ${job.budget}") }
            job.skills?.let { append("\nSkills: ${job.skills}") }
            job.country?.let { append("\nCountry: ${job.country}") }
            job.category?.let { append("\nCategory: ${Html.fromHtml(job.category, 0)}") }
        }.toString()
    }
}

private const val UPWORK_RSS_URL =
    "https://www.upwork.com/ab/feed/topics/rss?securityToken=6cb37a9e960ed9e0cc7e0bbef8480b89fc9c6d394f866ccf805cfffb02b2f57167360c1f4c38622baa11c78a12b07de5c89ca10de1774204bbf3597e98312894&userUid=1130704448110034944&orgUid=1130704448118423553&sort=local_jobs_on_top&topic=4394157"