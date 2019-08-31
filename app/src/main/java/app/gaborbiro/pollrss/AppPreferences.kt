package app.gaborbiro.pollrss

import com.gb.prefsutil.PrefsUtil

object AppPreferences {
    private val prefsUtil = PrefsUtil(AppContextProvider.appContext, "app_prefs")

    var dismissedJobs: MutableMap<String, Boolean> by prefsUtil.mutableDelegate(
        PREF_DISMISSED_JOBS,
        mutableMapOf<String, Boolean>()
    )
}

private const val PREF_LAST_SEEN_DATE = "LAST_SEEN_DATE"
private const val PREF_LAST_NOTIFIED_DATE = "LAST_NOTIFIED_DATE"
private const val PREF_SHOW_ALL_ENABLED = "SHOW_ALL_ENABLED"
private const val PREF_DISMISSED_JOBS = "DISMISSED_JOBS"
