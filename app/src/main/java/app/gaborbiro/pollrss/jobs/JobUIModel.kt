package app.gaborbiro.pollrss.jobs

class JobUIModel(
    val id: Long,
    val title: String,
    val description: String,
    val link: String,
    val time: String,
    val budget: String?,
    val country: String?,
    var markedAsRead: Boolean
)

//simpleFormattedTime()