package app.gaborbiro.pollrss

import android.content.Intent
import app.gaborbiro.pollrss.AppContextProvider.appContext
import app.gaborbiro.utils.Navigator

class NavigatorImpl : Navigator {
    override fun getMainActivityIntent() = Intent(appContext, MainActivity::class.java)

    override fun getDismissJobIntent() =
        Intent(appContext, NotificationBroadcastReceiver::class.java)
}