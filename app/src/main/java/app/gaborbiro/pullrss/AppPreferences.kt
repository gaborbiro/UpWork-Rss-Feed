package app.gaborbiro.pullrss

import com.gb.prefsutil.PrefsUtil

object AppPreferences {
    private val prefsUtil = PrefsUtil(AppContextProvider.appContext, "app_prefs")

    var lastSeenDate: Long by prefsUtil.delegate(PREF_LAST_SEEN_DATE, 0L)
    var lastNotifiedDate: Long by prefsUtil.delegate(PREF_LAST_NOTIFIED_DATE, 0L)
}

private const val PREF_LAST_SEEN_DATE = "LAST_SEEN_DATE"
private const val PREF_LAST_NOTIFIED_DATE = "LAST_NOTIFIED_DATE"
