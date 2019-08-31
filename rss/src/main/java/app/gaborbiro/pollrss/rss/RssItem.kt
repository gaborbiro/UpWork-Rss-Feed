package app.gaborbiro.pollrss.rss

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class RssItem(
    var feed: RssFeed? = null,
    var title: String? = null,
    var link: String? = null,
    var pubDate: String? = null,
    var description: String? = null,
    var content: String? = null,
    var guid: String? = null
) : Parcelable