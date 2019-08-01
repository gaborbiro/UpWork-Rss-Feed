package app.gaborbiro.pullrss

import android.content.Intent
import app.gaborbiro.pullrss.AppContextProvider.appContext
import app.gaborbiro.utils.Navigator

class NavigatorImpl : Navigator {
    override fun getMainActivityIntent() = Intent(appContext, MainActivity::class.java)
}