package app.gaborbiro.pollrss.data

import app.gaborbiro.pollrss.model.Job
import app.gaborbiro.pollrss.rss.RssItem
import app.gaborbiro.pollrss.utils.FEED_DATE_TIME_FORMAT
import java.time.ZoneId
import java.time.ZonedDateTime

object JobsMapper {

    fun map(rssItem: RssItem): Job? {
        if (rssItem.guid.isNullOrBlank() ||
            rssItem.link.isNullOrBlank() ||
            rssItem.title.isNullOrBlank() ||
            rssItem.description.isNullOrBlank()
        ) {
            return null
        }
        val description = rssItem.description!!
        var budgetGroups: MatchGroupCollection?
        var categoryGroups: MatchGroupCollection?
        var skillGroups: MatchGroupCollection?
        var countryGroups: MatchGroupCollection?
        var postedOnGroups: MatchGroupCollection?
        var linkGroups: MatchGroupCollection?
        val cleanedUpDescription = description.let {
            var temp = it
            budgetGroups = temp.getGroups("Budget")
            temp = budgetGroups?.get(0)?.range?.let(temp::removeRange) ?: temp
            categoryGroups = temp.getGroups("Category")
            temp = categoryGroups?.get(0)?.range?.let(temp::removeRange) ?: temp
            skillGroups = temp.getGroups("Skills")
            temp = skillGroups?.get(0)?.range?.let(temp::removeRange) ?: temp
            countryGroups = temp.getGroups("Country")
            temp = countryGroups?.get(0)?.range?.let(temp::removeRange) ?: temp
            postedOnGroups = temp.getGroups("Posted On")
            temp = postedOnGroups?.get(0)?.range?.let(temp::removeRange) ?: temp
            linkGroups = Regex("<a([^<]+)</a>").find(temp)?.groups
            temp = linkGroups?.get(0)?.range?.let(temp::removeRange) ?: temp
            temp
                .removeTrailingBreakTags()
                .replace(Regex("($BREAK_TAG){3,}"), "<br/><br/>")
                .replace(
                    "This job was posted from a mobile device, so please pardon any typos or any missing details.",
                    ""
                )
                .replace(Regex("(<[/]?b>)"), "")
        }
        val zonedDateTime = ZonedDateTime.parse(rssItem.pubDate, FEED_DATE_TIME_FORMAT)

        return Job(
            id = zonedDateTime.toInstant().toEpochMilli(),
            title = rssItem.title!!.replace(" - Upwork", ""),
            link = rssItem.link!!,
            localDateTime = zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime(),
            fullDescription = cleanedUpDescription,
            budget = budgetGroups?.get(1)?.value?.cleanWS(),
            category = categoryGroups?.get(1)?.value?.cleanWS(),
            skills = skillGroups?.get(1)?.value?.cleanWS(),
            country = countryGroups?.get(1)?.value?.cleanWS()
        )
    }

    private fun String.getGroups(name: String) =
        Regex("<b>[\\s]*$name</b>: ([^<]+)<br />").find(this)?.groups

    private fun String.cleanWS() = replace(Regex("[\\s]+"), " ").trim()
    private fun String.removeTrailingBreakTags() = replace(Regex("($BREAK_TAG)+$"), "")
}

private const val BREAK_TAG = "<br[\\s]*[/]?>"