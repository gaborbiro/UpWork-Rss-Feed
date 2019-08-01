package app.gaborbiro.pollrss.rss

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

@Parcelize
class RssFeed(
    var title: String? = null,
    var link: String? = null,
    var description: String? = null,
    var language: String? = null,
    var pubDate: Date? = null,
    var rssItems: List<RssItem> = mutableListOf()
) : Parcelable {

    fun addRssItem(rssItem: RssItem) {
        (rssItems as MutableList).add(rssItem)
    }

    fun setPubDate(pubDate: String) {
        try {
            val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
            this.pubDate = dateFormat.parse(pubDate)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
    }
}