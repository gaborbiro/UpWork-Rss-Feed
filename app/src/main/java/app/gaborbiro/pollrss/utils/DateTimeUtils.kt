package app.gaborbiro.pollrss.utils

import app.gaborbiro.pollrss.AppContextProvider
import app.gaborbiro.pollrss.R
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.time.*
import java.time.format.DateTimeFormatter


val DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mma, dd MMM")
val FEED_DATE_TIME_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z")

fun LocalDateTime.simpleFormattedTime(): String {
    val timePast = Duration.between(this, LocalDateTime.now())
    val res = AppContextProvider.appContext.resources
    return when {
        timePast < Duration.ofMinutes(1) -> "Now"
        timePast < Duration.ofHours(1) -> res.getQuantityString(
            R.plurals.minutes_ago,
            timePast.toMinutes().toInt(),
            timePast.toMinutes().toInt()
        )
        timePast < Duration.ofDays(1) -> res.getQuantityString(
            R.plurals.hours_ago,
            timePast.toHours().toInt(),
            timePast.toHours().toInt()
        )
        else -> res.getQuantityString(
            R.plurals.days_ago,
            timePast.toDays().toInt(),
            timePast.toDays().toInt()
        )
    }
}

class LocalDateTimeAdapter : TypeAdapter<LocalDateTime>() {

    override fun write(writer: JsonWriter, value: LocalDateTime) {
        writer.beginObject()
        writer.name("epochToMilli").value(value.toInstant(ZoneOffset.UTC).toEpochMilli())
        writer.endObject()
    }

    override fun read(reader: JsonReader): LocalDateTime {
        reader.beginObject()
        reader.hasNext()
        reader.nextName()
        val result =
            LocalDateTime.ofInstant(Instant.ofEpochMilli(reader.nextLong()), ZoneOffset.UTC)
        reader.endObject()
        return result
    }
}

fun epochMillis() = ZonedDateTime.now(ZoneOffset.UTC).toInstant().toEpochMilli()

fun LocalDateTime.epochMillis() =
    atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toInstant().toEpochMilli()

fun Long.toZDT(): ZonedDateTime =
    ZonedDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneOffset.UTC)