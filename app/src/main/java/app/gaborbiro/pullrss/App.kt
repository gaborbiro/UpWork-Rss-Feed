package app.gaborbiro.pullrss

import android.app.Application
import app.gaborbiro.utils.NavigatorProvider

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        AppContextProvider.appContext = this
        NavigatorProvider.navigator = NavigatorImpl()
    }
}