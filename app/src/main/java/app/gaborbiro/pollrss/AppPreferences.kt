package app.gaborbiro.pollrss

import app.gaborbiro.pollrss.model.Job
import com.gb.prefsutil.PrefsUtil

object AppPreferences {
    private val prefsUtil = PrefsUtil(AppContextProvider.appContext, "app_prefs")

    val favorites: MutableList<Long> by prefsUtil.mutableDelegate(
        PREF_FAVORITES,
        mutableListOf<Long>()
    )
    val jobs: MutableMap<Long, Job> by prefsUtil.mutableDelegate(
        PREF_JOBS,
        mutableMapOf<Long, Job>()
    )

    fun cleanupJobs() {
        jobs.keys.filter { it !in favorites }.forEach {
            jobs.remove(it)
        }
    }

    var markedAsRead: MutableMap<Long, Boolean> by prefsUtil.mutableDelegate(
        PREF_MARKED_AS_READ,
        mutableMapOf<Long, Boolean>()
    )

    var lastMarkAllReadTimestamp: Long by prefsUtil.delegate(PREF_LAST_MARK_ALL_READ_TIMESTAMP, 0L)

    var lastDisplayedTimestamp: Long by prefsUtil.delegate(PREF_LAST_DISPLAYED_TIMESTAMP, 0L)

    var lastRefresh: Long by prefsUtil.delegate(PREF_LAST_REFRESH, 0L)

    var showAll: Boolean by prefsUtil.delegate(PREF_SHOW_ALL, false)
}

private const val PREF_FAVORITES = "FAVORITES"
private const val PREF_JOBS = "JOBS"
private const val PREF_MARKED_AS_READ = "MARKED_AS_READ"
private const val PREF_LAST_MARK_ALL_READ_TIMESTAMP = "LAST_MARK_ALL_READ_TIMESTAMP"
private const val PREF_LAST_DISPLAYED_TIMESTAMP = "LAST_DISPLAYED_TIMESTAMP"
private const val PREF_LAST_REFRESH = "LAST_REFRESH"
private const val PREF_SHOW_ALL = "SHOW_ALL"
