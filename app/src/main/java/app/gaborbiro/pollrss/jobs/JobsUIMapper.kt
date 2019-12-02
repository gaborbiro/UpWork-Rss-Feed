package app.gaborbiro.pollrss.jobs

import android.text.Html
import app.gaborbiro.pollrss.AppPreferences
import app.gaborbiro.pollrss.model.Job
import app.gaborbiro.pollrss.utils.simpleFormattedTime

object JobsUIMapper {

    fun map(job: Job) = JobUIModel(
        job.id,
        job.title,
        job.description,
        job.link,
        simpleFormattedTime(job),
        job.budget,
        job.country,
        markedAsRead = AppPreferences.markedAsRead[job.id] ?: false
    )

    fun formatDescriptionForNotification(job: Job): String {
        return StringBuilder().apply {
            append(simpleFormattedTime(job) + ", ")
            appendln()
            append("${job.budget ?: "Hourly"}: ")
            append(Html.fromHtml(job.description, 0))
            appendln()
            job.skills?.let { append("Skills: ${job.skills}") }
            job.country?.let { append(", from ${job.country}") }
            job.category?.let { append("\nCategory: ${Html.fromHtml(job.category, 0)}") }
        }.toString()
    }

    fun simpleFormattedTime(job: Job) = job.localDateTime.simpleFormattedTime()

}