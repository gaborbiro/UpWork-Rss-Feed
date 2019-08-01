package app.gaborbiro.pullrss

import android.os.Bundle
import android.text.Html
import androidx.appcompat.app.AppCompatActivity
import app.gaborbiro.pullrss.rss.RssFeed
import app.gaborbiro.pullrss.rss.RssReader
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import java.net.URL

class MainActivity : AppCompatActivity() {

    private var disposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

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
                    .joinToString(separator = "<br><br>") {
                        "<b>Title:</b> " + it.title + "<br><b>Description:</b> " + it.description + "<br>"
                    }
                content.text = Html.fromHtml(str, 0)
                AppPreferences.lastSeenDate = it.rssItems.maxBy { it.pubDate!! }!!.pubDate!!.time
            }, {
                it.printStackTrace()
                it.message?.let(this::longToast) ?: toast("Oops. Something went wrong!")
            })
    }

    override fun onPause() {
        super.onPause()
        disposable?.dispose()
    }
}

private const val UPWORK_RSS_URL =
    "https://www.upwork.com/ab/feed/topics/rss?securityToken=6cb37a9e960ed9e0cc7e0bbef8480b89fc9c6d394f866ccf805cfffb02b2f57167360c1f4c38622baa11c78a12b07de5c89ca10de1774204bbf3597e98312894&userUid=1130704448110034944&orgUid=1130704448118423553&sort=local_jobs_on_top&topic=4394157"