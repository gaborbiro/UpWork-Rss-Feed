package app.gaborbiro.pollrss.model

import app.gaborbiro.pollrss.utils.DATE_TIME_FORMAT
import app.gaborbiro.pollrss.utils.simpleFormattedTime
import java.time.LocalDateTime

class Job(
    val id: String,
    val title: String,
    val link: String,
    val localDateTime: LocalDateTime,
    val description: String,
    val budget: String?,
    val category: String?,
    val skills: String?,
    val country: String?
)


fun Job.exactFormattedTime() = DATE_TIME_FORMAT.format(localDateTime)

fun Job.simpleFormattedTime() = localDateTime.simpleFormattedTime()

