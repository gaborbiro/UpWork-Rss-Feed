package app.gaborbiro.pollrss.model

import android.text.Html
import app.gaborbiro.pollrss.utils.LocalDateTimeAdapter
import app.gaborbiro.pollrss.utils.simpleFormattedTime
import com.google.gson.annotations.JsonAdapter
import java.time.LocalDateTime

class Job(
    val id: Long,
    val title: String,
    val link: String,
    @JsonAdapter(LocalDateTimeAdapter::class)
    val localDateTime: LocalDateTime,
    val description: String,
    val budget: String?,
    val category: String?,
    val skills: String?,
    val country: String?
) {
    override fun toString(): String {
        return "Job(title='$title', localDateTime=$localDateTime)"
    }
}

fun Job.formatDescriptionForNotification(): String {
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

fun Job.simpleFormattedTime() = localDateTime.simpleFormattedTime()