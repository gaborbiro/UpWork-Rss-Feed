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
        jobs.keys.filter { !favorites.contains(it) }.forEach {
            jobs.keys.remove(it)
        }
    }

    var markedAsRead: MutableMap<Long, Boolean> by prefsUtil.mutableDelegate(
        PREF_MARKED_AS_READ,
        mutableMapOf<Long, Boolean>()
    )

    var lastSeenDate: Long by prefsUtil.delegate(PREF_LAST_SEEN_DATE, 0L)
}

private const val PREF_FAVORITES = "FAVORITES"
private const val PREF_JOBS = "JOBS"
private const val PREF_MARKED_AS_READ = "MARKED_AS_READ"
private const val PREF_LAST_SEEN_DATE = "LAST_SEEN_DATE"
