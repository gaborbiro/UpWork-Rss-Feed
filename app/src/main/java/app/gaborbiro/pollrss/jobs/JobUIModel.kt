package app.gaborbiro.pollrss.jobs

/**
 * Data class needed for diffing
 *
 * @see [app.gaborbiro.pollrss.JobDiffCallback]
 */
data class JobUIModel(
    val id: Long,
    val title: String,
    val description: String,
    val link: String,
    val time: String,
    val budget: String?,
    val country: String?,
    var markedAsRead: Boolean
)