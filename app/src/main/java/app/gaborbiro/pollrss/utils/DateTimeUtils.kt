package app.gaborbiro.pollrss.utils

import app.gaborbiro.pollrss.AppContextProvider
import app.gaborbiro.pollrss.R
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mma, dd MMM")
val FEED_DATE_TIME_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z")

fun LocalDateTime.simpleFormattedTime(): String {
    val timePast = Duration.between(this, LocalDateTime.now())
    return when {
        timePast < Duration.ofMinutes(1) -> "Now"
        timePast < Duration.ofHours(1) -> AppContextProvider.appContext.resources.getQuantityString(
            R.plurals.minutes_ago, timePast.toMinutes().toInt(), timePast.toMinutes()
        )
        timePast < Duration.ofDays(1) -> AppContextProvider.appContext.resources.getQuantityString(
            R.plurals.hours_ago, timePast.toHours().toInt(), timePast.toHours()
        )
        else -> AppContextProvider.appContext.resources.getQuantityString(
            R.plurals.days_ago, timePast.toDays().toInt(), timePast.toDays()
        )
    }
}