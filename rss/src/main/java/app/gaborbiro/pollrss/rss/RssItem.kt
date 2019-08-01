package app.gaborbiro.pollrss.rss

import android.os.Parcelable
import java.text.ParseException
import java.text.SimpleDateFormat
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class RssItem(
    var feed: RssFeed? = null,
    var title: String? = null,
    var link: String? = null,
    var pubDate: Date? = null,
    var description: String? = null,
    var content: String? = null
) : Comparable<RssItem>, Parcelable {

    fun setPubDate(pubDate: String) {
        try {
            val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
            this.pubDate = dateFormat.parse(pubDate)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
    }

    override fun compareTo(other: RssItem): Int {
        return if (pubDate != null && other.pubDate != null) {
            pubDate!!.compareTo(other.pubDate)
        } else {
            0
        }
    }
}
