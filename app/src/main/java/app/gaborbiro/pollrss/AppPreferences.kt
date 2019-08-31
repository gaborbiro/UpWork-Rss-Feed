package app.gaborbiro.pollrss

import com.gb.prefsutil.PrefsUtil

object AppPreferences {
    private val prefsUtil = PrefsUtil(AppContextProvider.appContext, "app_prefs")

    var markedAsViewed: MutableMap<String, Boolean> by prefsUtil.mutableDelegate(
        PREF_MARKED_AS_VIEWED,
        mutableMapOf<String, Boolean>()
    )
}

private const val PREF_LAST_SEEN_DATE = "LAST_SEEN_DATE"
private const val PREF_MARKED_AS_VIEWED = "MARKED_AS_VIEWED"
