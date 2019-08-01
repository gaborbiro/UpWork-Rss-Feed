package app.gaborbiro.pollrss

import app.gaborbiro.pollrss.model.Job
import app.gaborbiro.pollrss.rss.RssItem

object JobMapper {

    fun map(rssItem: RssItem): Job {
        val description = rssItem.description!!
        var budgetGroups: MatchGroupCollection?
        var categoryGroups: MatchGroupCollection?
        var skillGroups: MatchGroupCollection?
        var countryGroups: MatchGroupCollection?
        var postedOnGroups: MatchGroupCollection?
        var linkGroups: MatchGroupCollection?
        val shortDescription = description.let {
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
            temp.replace(Regex("(<br[\\s]*[/]?>|[\\n]+)"), "\n")
                .replace(Regex("(<[/]?b>)"), "").cleanWS()
        }
        return Job(
            title = rssItem.title,
            link = rssItem.link,
            pubDate = rssItem.pubDate,
            description = shortDescription,
            budget = budgetGroups?.get(1)?.value?.cleanWS(),
            category = categoryGroups?.get(1)?.value?.cleanWS(),
            skills = skillGroups?.get(1)?.value?.cleanWS(),
            country = countryGroups?.get(1)?.value?.cleanWS()
        )
    }

    private fun String.getGroups(name: String) =
        Regex("<b>[\\s]*$name</b>: ([^<]+)<br />").find(this)?.groups

    private fun String.cleanWS() = replace(Regex("[\\s]+"), " ").trim()
}