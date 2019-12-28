package app.gaborbiro.pollrss.model

import app.gaborbiro.pollrss.utils.LocalDateTimeAdapter
import com.google.gson.annotations.JsonAdapter
import java.time.LocalDateTime

class Job(
    val id: Long,
    val title: String,
    val link: String,
    @JsonAdapter(LocalDateTimeAdapter::class)
    val localDateTime: LocalDateTime,
    val fullDescription: String,
    val budget: String?,
    val budgetValue: Int?,
    val category: String?,
    val skills: String?,
    val country: String?
) {
    override fun toString(): String {
        return "Job(title='$title', localDateTime=$localDateTime)"
    }
}